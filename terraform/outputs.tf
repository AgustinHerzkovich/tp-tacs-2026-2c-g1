output "artifact_registry_repository" {
  value = "${var.gcp_region}-docker.pkg.dev/${var.gcp_project_id}/${google_artifact_registry_repository.containers.repository_id}"
}

output "activity_images_bucket" {
  value = google_storage_bucket.activity_images.name
}

output "cloud_sql_connection_name" {
  value = google_sql_database_instance.keycloak.connection_name
}

output "secret_ids" {
  value = sort(tolist(local.secret_ids))
}

output "keycloak_url" {
  value = try(google_cloud_run_v2_service.keycloak[0].uri, null)
}

output "backend_url" {
  value = try(google_cloud_run_v2_service.backend[0].uri, null)
}

output "frontend_url" {
  value = try(google_cloud_run_v2_service.frontend[0].uri, null)
}

output "atlas_project_id" {
  value = mongodbatlas_project.planazo.id
}

output "atlas_cluster_name" {
  value = mongodbatlas_advanced_cluster.planazo.name
}

output "atlas_standard_connection_string" {
  description = "Connection seed without database credentials."
  value       = try(mongodbatlas_advanced_cluster.planazo.connection_strings.standard_srv, null)
}

output "github_actions_workload_identity_provider" {
  value = google_iam_workload_identity_pool_provider.github.name
}

output "github_actions_deploy_service_account" {
  value = google_service_account.github_deployer.email
}
