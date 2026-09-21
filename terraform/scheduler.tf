locals {
  scheduled_jobs = {
    weather = {
      schedule = "0 * * * *"
      path     = "weather"
    }
    activity-status = {
      schedule = "*/5 * * * *"
      path     = "activity-status"
    }
    votations = {
      schedule = "0 * * * *"
      path     = "votations"
    }
  }
}

resource "google_cloud_scheduler_job" "scheduled_tasks" {
  for_each = length(google_cloud_run_v2_service.backend) == 0 ? {} : local.scheduled_jobs

  project          = var.gcp_project_id
  region           = var.gcp_region
  name             = "${local.name}-${each.key}"
  description      = "Runs the Planazo ${each.key} maintenance pass"
  schedule         = each.value.schedule
  time_zone        = "Etc/UTC"
  attempt_deadline = "900s"

  retry_config {
    retry_count = 1
  }

  http_target {
    http_method = "POST"
    uri         = "${local.backend_url}/internal/scheduled/${each.value.path}"

    oidc_token {
      service_account_email = google_service_account.scheduler.email
      audience              = var.scheduler_oidc_audience
    }
  }

  depends_on = [
    google_service_account_iam_member.scheduler_service_account_user,
    google_service_account_iam_member.scheduler_token_creator,
  ]
}
