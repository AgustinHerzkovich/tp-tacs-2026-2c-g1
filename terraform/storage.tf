resource "google_storage_bucket" "activity_images" {
  project                     = var.gcp_project_id
  name                        = local.bucket_name
  location                    = var.gcp_region
  uniform_bucket_level_access = true
  force_destroy               = true

  lifecycle_rule {
    condition {
      age = 30
    }
    action {
      type = "Delete"
    }
  }

  depends_on = [google_project_service.required]
}

resource "google_storage_bucket_iam_member" "backend_objects" {
  bucket = google_storage_bucket.activity_images.name
  role   = "roles/storage.objectAdmin"
  member = "serviceAccount:${google_service_account.backend.email}"
}

resource "google_service_account_iam_member" "backend_url_signer" {
  service_account_id = google_service_account.backend.name
  role               = "roles/iam.serviceAccountTokenCreator"
  member             = "serviceAccount:${google_service_account.backend.email}"
}
