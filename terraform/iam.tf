resource "google_service_account" "backend" {
  project      = var.gcp_project_id
  account_id   = "${local.name}-backend"
  display_name = "Planazo backend (${var.environment})"
}

resource "google_service_account" "frontend" {
  project      = var.gcp_project_id
  account_id   = "${local.name}-frontend"
  display_name = "Planazo frontend (${var.environment})"
}

resource "google_service_account" "keycloak" {
  project      = var.gcp_project_id
  account_id   = "${local.name}-keycloak"
  display_name = "Planazo Keycloak (${var.environment})"
}

resource "google_project_iam_member" "keycloak_cloud_sql_client" {
  project = var.gcp_project_id
  role    = "roles/cloudsql.client"
  member  = "serviceAccount:${google_service_account.keycloak.email}"
}

resource "google_service_account" "scheduler" {
  project      = var.gcp_project_id
  account_id   = "${local.name}-scheduler"
  display_name = "Planazo Cloud Scheduler (${var.environment})"
}

data "google_project" "current" {
  project_id = var.gcp_project_id
}

locals {
  cloud_scheduler_service_agent = "service-${data.google_project.current.number}@gcp-sa-cloudscheduler.iam.gserviceaccount.com"
}

resource "google_service_account_iam_member" "scheduler_service_account_user" {
  service_account_id = google_service_account.scheduler.name
  role               = "roles/iam.serviceAccountUser"
  member             = "serviceAccount:${local.cloud_scheduler_service_agent}"
}

resource "google_service_account_iam_member" "scheduler_token_creator" {
  service_account_id = google_service_account.scheduler.name
  role               = "roles/iam.serviceAccountTokenCreator"
  member             = "serviceAccount:${local.cloud_scheduler_service_agent}"
}
