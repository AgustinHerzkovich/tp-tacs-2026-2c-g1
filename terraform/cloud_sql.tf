resource "google_sql_database_instance" "keycloak" {
  project             = var.gcp_project_id
  name                = "${local.name}-keycloak"
  region              = var.gcp_region
  database_version    = "POSTGRES_17"
  deletion_protection = var.cloud_sql_deletion_protection

  settings {
    edition           = "ENTERPRISE"
    tier              = var.cloud_sql_tier
    availability_type = "ZONAL"
    disk_type         = "PD_HDD"
    disk_size         = 10
    disk_autoresize   = true

    backup_configuration {
      enabled = false
    }

    ip_configuration {
      ipv4_enabled = true
    }
  }

  depends_on = [google_project_service.required]
}

resource "google_sql_database" "keycloak" {
  project  = var.gcp_project_id
  name     = "keycloak"
  instance = google_sql_database_instance.keycloak.name
}

# The password is loaded into both Cloud SQL and Secret Manager after apply. Keeping it out of
# Terraform prevents plaintext credentials from being persisted in tfstate.
resource "google_sql_user" "keycloak" {
  project  = var.gcp_project_id
  name     = "keycloak"
  instance = google_sql_database_instance.keycloak.name
  password = "replace-after-apply"

  lifecycle {
    ignore_changes = [password]
  }
}
