resource "google_artifact_registry_repository" "containers" {
  project       = var.gcp_project_id
  location      = var.gcp_region
  repository_id = local.artifact_repository
  description   = "Planazo application container images"
  format        = "DOCKER"

  depends_on = [google_project_service.required]
}

resource "google_artifact_registry_repository_iam_member" "runtime_readers" {
  for_each = {
    backend  = google_service_account.backend.email
    frontend = google_service_account.frontend.email
    keycloak = google_service_account.keycloak.email
  }

  project    = google_artifact_registry_repository.containers.project
  location   = google_artifact_registry_repository.containers.location
  repository = google_artifact_registry_repository.containers.repository_id
  role       = "roles/artifactregistry.reader"
  member     = "serviceAccount:${each.value}"
}
