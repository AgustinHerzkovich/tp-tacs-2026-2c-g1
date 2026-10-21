locals {
  legacy_weather_service_name = "${local.name}-legacy-weather"
  legacy_weather_url          = try(google_cloud_run_v2_service.legacy_weather[0].uri, "")
}

resource "google_service_account" "legacy_weather" {
  project      = var.gcp_project_id
  account_id   = "${local.name}-legacy-weather"
  display_name = "Planazo legacy weather provider (${var.environment})"
}

resource "google_secret_manager_secret_iam_member" "legacy_weather_session" {
  project   = var.gcp_project_id
  secret_id = google_secret_manager_secret.application["${local.name}-legacy-weather-session"].secret_id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${google_service_account.legacy_weather.email}"
}

resource "google_secret_manager_secret_iam_member" "backend_legacy_weather_token" {
  project   = var.gcp_project_id
  secret_id = google_secret_manager_secret.application["${local.name}-legacy-weather-token"].secret_id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${google_service_account.backend.email}"
}

# The binary embeds the course's Google OAuth client, so it is not versioned: it is uploaded here by
# hand and Cloud Build downloads it to build the image.
resource "google_storage_bucket" "legacy_weather_binary" {
  project                     = var.gcp_project_id
  name                        = "${var.gcp_project_id}-${local.name}-legacy-binary"
  location                    = var.gcp_region
  uniform_bucket_level_access = true
  public_access_prevention    = "enforced"
  force_destroy               = true

  depends_on = [google_project_service.required]
}

resource "google_storage_bucket_iam_member" "cloud_build_reads_legacy_binary" {
  bucket = google_storage_bucket.legacy_weather_binary.name
  role   = "roles/storage.objectViewer"
  member = "serviceAccount:${data.google_project.current.number}-compute@developer.gserviceaccount.com"
}

resource "google_cloud_run_v2_service" "legacy_weather" {
  count = var.legacy_weather_image == "" ? 0 : 1

  project             = var.gcp_project_id
  name                = local.legacy_weather_service_name
  location            = var.gcp_region
  deletion_protection = false
  ingress             = "INGRESS_TRAFFIC_ALL"

  template {
    service_account                  = google_service_account.legacy_weather.email
    max_instance_request_concurrency = 20

    scaling {
      min_instance_count = 0
      max_instance_count = 1
    }

    volumes {
      name = "session"
      secret {
        secret = google_secret_manager_secret.application["${local.name}-legacy-weather-session"].secret_id
        items {
          version = "latest"
          path    = "token.json"
        }
      }
    }

    containers {
      image = var.legacy_weather_image

      ports {
        container_port = 9978
      }

      resources {
        limits = {
          cpu    = "1"
          memory = "256Mi"
        }
        cpu_idle = true
      }

      env {
        name  = "SESSION_SECRET_FILE"
        value = "/secrets/session/token.json"
      }

      volume_mounts {
        name       = "session"
        mount_path = "/secrets/session"
      }

      startup_probe {
        http_get {
          path = "/healthz"
          port = 9978
        }
        period_seconds    = 2
        failure_threshold = 15
      }

      liveness_probe {
        http_get {
          path = "/healthz"
          port = 9978
        }
        period_seconds    = 10
        timeout_seconds   = 2
        failure_threshold = 3
      }
    }
  }

  depends_on = [
    google_project_service.required,
    google_secret_manager_secret_iam_member.legacy_weather_session,
  ]

  lifecycle {
    ignore_changes = [template[0].containers[0].image]
  }
}

resource "google_cloud_run_v2_service_iam_member" "backend_invokes_legacy_weather" {
  count = length(google_cloud_run_v2_service.legacy_weather)

  project  = var.gcp_project_id
  location = var.gcp_region
  name     = google_cloud_run_v2_service.legacy_weather[0].name
  role     = "roles/run.invoker"
  member   = "serviceAccount:${google_service_account.backend.email}"
}
