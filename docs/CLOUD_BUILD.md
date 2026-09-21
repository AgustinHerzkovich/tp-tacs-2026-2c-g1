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
