resource "google_cloud_run_v2_service" "keycloak" {
  count = var.keycloak_image == "" ? 0 : 1

  project             = var.gcp_project_id
  name                = local.keycloak_service_name
  location            = var.gcp_region
  deletion_protection = false
  ingress             = "INGRESS_TRAFFIC_ALL"

  template {
    service_account                  = google_service_account.keycloak.email
    max_instance_request_concurrency = 20

    scaling {
      min_instance_count = 0
      max_instance_count = 1
    }

    volumes {
      name = "cloudsql"
      cloud_sql_instance {
        instances = [google_sql_database_instance.keycloak.connection_name]
      }
    }

    containers {
      image = var.keycloak_image

      ports {
        container_port = 8080
      }

      resources {
        limits = {
          cpu    = "1"
          memory = "1Gi"
        }
        cpu_idle = true
      }

      volume_mounts {
        name       = "cloudsql"
        mount_path = "/cloudsql"
      }

      env {
        name  = "KC_DB"
        value = "postgres"
      }
      env {
        name  = "KC_DB_URL"
        value = "jdbc:postgresql://localhost/keycloak?host=/cloudsql/${google_sql_database_instance.keycloak.connection_name}"
      }
      env {
        name  = "KC_DB_USERNAME"
        value = google_sql_user.keycloak.name
      }
      env {
        name  = "KC_HOSTNAME_STRICT"
        value = "false"
      }
      env {
        name  = "KC_HTTP_PORT"
        value = "8080"
      }
      env {
        name  = "KC_BOOTSTRAP_ADMIN_USERNAME"
        value = "admin"
      }

      env {
        name = "KC_BOOTSTRAP_ADMIN_PASSWORD"
        value_source {
          secret_key_ref {
            secret  = google_secret_manager_secret.application["${local.name}-keycloak-admin-password"].secret_id
            version = "latest"
          }
        }
      }

      env {
        name = "KC_DB_PASSWORD"
        value_source {
          secret_key_ref {
            secret  = google_secret_manager_secret.application["${local.name}-keycloak-db-password"].secret_id
            version = "latest"
          }
        }
      }
    }
  }

  depends_on = [
    google_project_service.required,
    google_secret_manager_secret_iam_member.keycloak_admin,
    google_secret_manager_secret_iam_member.keycloak_database,
  ]
}

resource "google_cloud_run_v2_service" "backend" {
  count = var.backend_image == "" || var.keycloak_image == "" ? 0 : 1

  project             = var.gcp_project_id
  name                = local.backend_service_name
  location            = var.gcp_region
  deletion_protection = false
  ingress             = "INGRESS_TRAFFIC_ALL"

  template {
    service_account = google_service_account.backend.email

    scaling {
      min_instance_count = 0
      max_instance_count = 2
    }

    containers {
      image = var.backend_image

      ports {
        container_port = 8080
      }

      resources {
        limits = {
          cpu    = "1"
          memory = "1Gi"
        }
        cpu_idle = true
      }

      env {
        name  = "STORAGE_PROVIDER"
        value = "gcs"
      }
      env {
        name  = "STORAGE_BUCKET"
        value = google_storage_bucket.activity_images.name
      }
      env {
        name  = "SECURITY_JWT_ISSUER_URI"
        value = "${local.keycloak_url}/realms/${var.keycloak_realm}"
      }
      env {
        name  = "SECURITY_JWT_JWK_SET_URI"
        value = "${local.keycloak_url}/realms/${var.keycloak_realm}/protocol/openid-connect/certs"
      }
      env {
        name  = "SECURITY_JWT_AUDIENCE"
        value = var.keycloak_backend_audience
      }
      env {
        name  = "APP_SEED_ENABLED"
        value = "false"
      }
      env {
        name  = "ACTIVITY_WEATHER_CHECK_CRON"
        value = "-"
      }
      env {
        name  = "ACTIVITY_STATUS_CHECK_CRON"
        value = "-"
      }
      env {
        name  = "VOTATION_CLOSING_CHECK_CRON"
        value = "-"
      }

      env {
        name = "MONGODB_URI"
        value_source {
          secret_key_ref {
            secret  = google_secret_manager_secret.application["${local.name}-atlas-uri"].secret_id
            version = "latest"
          }
        }
      }
    }
  }

  depends_on = [
    google_project_service.required,
    google_secret_manager_secret_iam_member.backend_atlas,
  ]
}

resource "google_cloud_run_v2_job" "scheduled_tasks" {
  count = var.backend_image == "" || var.keycloak_image == "" ? 0 : 1

  project             = var.gcp_project_id
  name                = "${local.name}-scheduled-tasks"
  location            = var.gcp_region
  deletion_protection = false

  template {
    template {
      service_account = google_service_account.backend.email
      max_retries     = 1
      timeout         = "900s"

      containers {
        image   = var.backend_image
        command = ["java"]
        args = [
          "-jar",
          "app.jar",
          "--spring.main.web-application-type=none",
          "--app.mode=scheduled-jobs",
          "--activity.weather-check-cron=-",
          "--activity.status-check-cron=-",
          "--votation.closing-check-cron=-",
        ]

        resources {
          limits = {
            cpu    = "1"
            memory = "1Gi"
          }
        }

        env {
          name  = "WEATHER_PROVIDER"
          value = "open-meteo"
        }
        env {
          name  = "STORAGE_PROVIDER"
          value = "gcs"
        }
        env {
          name  = "STORAGE_BUCKET"
          value = google_storage_bucket.activity_images.name
        }
        env {
          name = "MONGODB_URI"
          value_source {
            secret_key_ref {
              secret  = google_secret_manager_secret.application["${local.name}-atlas-uri"].secret_id
              version = "latest"
            }
          }
        }
      }
    }
  }

  depends_on = [
    google_project_service.required,
    google_secret_manager_secret_iam_member.backend_atlas,
  ]
}

resource "google_cloud_run_v2_service" "frontend" {
  count = var.frontend_image == "" || var.backend_image == "" || var.keycloak_image == "" ? 0 : 1

  project             = var.gcp_project_id
  name                = local.frontend_service_name
  location            = var.gcp_region
  deletion_protection = false
  ingress             = "INGRESS_TRAFFIC_ALL"

  template {
    service_account = google_service_account.frontend.email

    scaling {
      min_instance_count = 0
      max_instance_count = 2
    }

    containers {
      image = var.frontend_image

      ports {
        container_port = 3000
      }

      resources {
        limits = {
          cpu    = "1"
          memory = "512Mi"
        }
        cpu_idle = true
      }

      env {
        name  = "BACKEND_URL"
        value = local.backend_url
      }
    }
  }

  depends_on = [google_project_service.required]
}

resource "google_cloud_run_v2_service_iam_member" "public_keycloak" {
  count = length(google_cloud_run_v2_service.keycloak)

  project  = var.gcp_project_id
  location = var.gcp_region
  name     = google_cloud_run_v2_service.keycloak[0].name
  role     = "roles/run.invoker"
  member   = "allUsers"
}

resource "google_cloud_run_v2_service_iam_member" "public_backend" {
  count = length(google_cloud_run_v2_service.backend)

  project  = var.gcp_project_id
  location = var.gcp_region
  name     = google_cloud_run_v2_service.backend[0].name
  role     = "roles/run.invoker"
  member   = "allUsers"
}

resource "google_cloud_run_v2_service_iam_member" "public_frontend" {
  count = length(google_cloud_run_v2_service.frontend)

  project  = var.gcp_project_id
  location = var.gcp_region
  name     = google_cloud_run_v2_service.frontend[0].name
  role     = "roles/run.invoker"
  member   = "allUsers"
}
