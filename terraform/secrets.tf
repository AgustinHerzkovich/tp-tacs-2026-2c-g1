locals {
  secret_ids = toset([
    "${local.name}-atlas-uri",
    "${local.name}-keycloak-admin-password",
    "${local.name}-keycloak-db-password",
  ])
}

resource "google_secret_manager_secret" "application" {
  for_each = local.secret_ids

  project   = var.gcp_project_id
  secret_id = each.value

  replication {
    auto {}
  }

  depends_on = [google_project_service.required]
}

resource "google_secret_manager_secret_iam_member" "backend_atlas" {
  project   = var.gcp_project_id
  secret_id = google_secret_manager_secret.application["${local.name}-atlas-uri"].secret_id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${google_service_account.backend.email}"
}

resource "google_secret_manager_secret_iam_member" "keycloak_admin" {
  project   = var.gcp_project_id
  secret_id = google_secret_manager_secret.application["${local.name}-keycloak-admin-password"].secret_id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${google_service_account.keycloak.email}"
}

resource "google_secret_manager_secret_iam_member" "keycloak_database" {
  project   = var.gcp_project_id
  secret_id = google_secret_manager_secret.application["${local.name}-keycloak-db-password"].secret_id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${google_service_account.keycloak.email}"
}
