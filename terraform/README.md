# Planazo infrastructure

This single Terraform root manages MongoDB Atlas and all GCP resources. Local secret values stay out of
`terraform.tfvars`: Atlas API keys use `TF_VAR_atlas_public_key` and `TF_VAR_atlas_private_key`, while
application credentials are loaded into Secret Manager after the base apply.

## Apply sequence

1. Configure `terraform.tfvars` from `terraform.tfvars.example` with the GCP project and Atlas
   organization. Keep all image variables empty.
2. Export Atlas API credentials and authenticate GCP:

```powershell
$env:TF_VAR_atlas_public_key="..."
$env:TF_VAR_atlas_private_key="..."
gcloud auth application-default login
```

3. Initialize, review, and apply the base infrastructure:

```powershell
terraform init
terraform fmt -check -recursive
terraform validate
terraform plan -out="base.tfplan"
terraform apply "base.tfplan"
```

4. Replace the placeholder Cloud SQL password and load these secret versions:

```text
planazo-prod-keycloak-db-password
planazo-prod-keycloak-admin-password
planazo-prod-atlas-uri
```

5. Build and push Keycloak, set `keycloak_image`, then run `plan` and `apply` again.
6. Build and push the backend, set `backend_image`, then apply again. This also creates the Cloud Run
   Job and Cloud Scheduler triggers.
7. Build the frontend with `NEXT_PUBLIC_KEYCLOAK_URL` set to the `keycloak_url` output, set
   `frontend_image`, and apply again.
8. Update the `solnotfoundFrontend` Keycloak client with the final frontend origin, redirect URI, web
   origin, and post-logout URI.

Image build commands are documented in [`../docs/CLOUD_BUILD.md`](../docs/CLOUD_BUILD.md).

## Cost and shutdown

Defaults use Cloud Run scale-to-zero, Atlas M0, and zonal Enterprise `db-f1-micro` Cloud SQL without
backups or high availability. Cloud SQL is the main fixed cost. After grading:

```powershell
terraform destroy
```

Verify that no container images, secret versions, Cloud SQL backups, Atlas projects, or billing-enabled
GCP projects remain.

## Known tradeoffs

- Cloud Run has no static egress by default, so Atlas uses `0.0.0.0/0`. Authentication remains
  mandatory; a long-lived environment should use static egress and a narrow access list.
- Timers are disabled in the web service. Cloud Scheduler invokes a finite Cloud Run Job for weather,
  activity status, and votation closing.
- The imported Keycloak realm contains development settings. Rotate credentials before public use.
