locals {
  scheduled_jobs = {
    weather = {
      schedule = "0 * * * *"
      argument = "weather"
    }
    activity-status = {
      schedule = "*/5 * * * *"
      argument = "activity-status"
    }
    votations = {
      schedule = "0 * * * *"
      argument = "votations"
    }
  }
}

resource "google_cloud_run_v2_job_iam_member" "scheduler_invoker" {
  count = length(google_cloud_run_v2_job.scheduled_tasks)

  project  = var.gcp_project_id
  location = var.gcp_region
  name     = google_cloud_run_v2_job.scheduled_tasks[0].name
  role     = "roles/run.invoker"
  member   = "serviceAccount:${google_service_account.scheduler.email}"
}

resource "google_cloud_scheduler_job" "scheduled_tasks" {
  for_each = length(google_cloud_run_v2_job.scheduled_tasks) == 0 ? {} : local.scheduled_jobs

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
    uri         = "https://${var.gcp_region}-run.googleapis.com/apis/run.googleapis.com/v1/namespaces/${var.gcp_project_id}/jobs/${google_cloud_run_v2_job.scheduled_tasks[0].name}:run"
    body = base64encode(jsonencode({
      overrides = {
        containerOverrides = [{
          args = [
            "-jar",
            "app.jar",
            "--spring.main.web-application-type=none",
            "--app.mode=scheduled-jobs",
            "--activity.weather-check-cron=-",
            "--activity.status-check-cron=-",
            "--votation.closing-check-cron=-",
            each.value.argument,
          ]
        }]
      }
    }))

    headers = {
      "Content-Type" = "application/json"
    }

    oauth_token {
      service_account_email = google_service_account.scheduler.email
    }
  }

  depends_on = [google_cloud_run_v2_job_iam_member.scheduler_invoker]
}
