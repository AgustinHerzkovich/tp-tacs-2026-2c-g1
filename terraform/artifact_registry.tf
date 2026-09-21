resource "google_artifact_registry_repository" "containers" {
  project       = var.gcp_project_id
  location      = var.gcp_region
  repository_id = local.artifact_repository
  description   = "Planazo application container images"
  format        = "DOCKER"

  depends_on = [google_project_service.required]
}
