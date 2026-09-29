# Cloud container builds

`cloudbuild.yaml` builds and publishes immutable backend, Keycloak, and frontend images to the
Artifact Registry repository created by Terraform. Every image is tagged with Cloud Build's
`$COMMIT_SHA`.

The frontend's `NEXT_PUBLIC_*` variables are compiled into the browser bundle. `_KEYCLOAK_URL` must
therefore be the public URL of the deployed Keycloak service, not an internal Cloud Run hostname.

After the GCP project and Artifact Registry exist, run from the repository root:

```bash
gcloud builds submit \
  --config cloudbuild.yaml \
  --project PROJECT_ID \
  --substitutions=_REGION=us-central1,_REPOSITORY=planazo-prod-containers,_KEYCLOAK_URL=https://KEYCLOAK_URL
```

For a manual build not associated with a Git commit, Cloud Build may not populate `$COMMIT_SHA`. Pass
it explicitly with a concrete revision:

```bash
gcloud builds submit \
  --config cloudbuild.yaml \
  --project PROJECT_ID \
  --substitutions=COMMIT_SHA=$(git rev-parse HEAD),_REGION=us-central1,_REPOSITORY=planazo-prod-containers,_KEYCLOAK_URL=https://KEYCLOAK_URL
```

Do not use `latest` in Terraform. Copy the resulting image URIs, including the SHA tag, into
`terraform/terraform.tfvars` and apply that root.

## Automatic deployments

`.github/workflows/deploy.yml` deploys changed components after a merge to `main` or `develop`:

- `backend/**` builds and updates the backend Cloud Run service.
- `frontend/**` builds the frontend with the production Keycloak URL and updates its service.
- Keycloak, realm, or theme changes build and update the Keycloak service.

Path filtering compares the files changed by each push, so components without effective source or build
configuration changes are neither rebuilt nor deployed.

The workflow authenticates through Workload Identity Federation. Configure these GitHub environment
secrets in the `production` environment:

```text
GCP_WORKLOAD_IDENTITY_PROVIDER=projects/689432164639/locations/global/workloadIdentityPools/github-actions/providers/github
GCP_DEPLOY_SERVICE_ACCOUNT=planazo-github-deployer@planazo-tacs-g1-2026.iam.gserviceaccount.com
```

The deploy service account needs permission to submit Cloud Build builds, update Cloud Run services
and jobs, and act as the runtime service accounts. Terraform ignores only externally deployed container
image fields, so subsequent Terraform applies continue managing all other service configuration without
rolling images back.

Both branches currently deploy to the same production Cloud Run services. Deployments are serialized,
but the most recently completed `main` or `develop` workflow determines the running image. Introduce a
separate environment and service names before using `develop` as an isolated staging environment.

## Local equivalents

```bash
docker build -t planazo-backend:local backend
docker build -f keycloak/Dockerfile -t planazo-keycloak:local .
docker build \
  --build-arg NEXT_PUBLIC_KEYCLOAK_URL=http://localhost:8090 \
  --build-arg NEXT_PUBLIC_KEYCLOAK_REALM=solnotfound \
  --build-arg NEXT_PUBLIC_KEYCLOAK_CLIENT_ID=solnotfoundFrontend \
  -t planazo-frontend:local frontend
```
