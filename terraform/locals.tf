locals {
  name                  = "${var.resource_prefix}-${var.environment}"
  artifact_repository   = "${local.name}-containers"
  backend_service_name  = "${local.name}-backend"
  frontend_service_name = "${local.name}-frontend"
  keycloak_service_name = "${local.name}-keycloak"
  bucket_name           = "${var.gcp_project_id}-${local.name}-images"
  keycloak_url          = try(google_cloud_run_v2_service.keycloak[0].uri, "")
  backend_url           = try(google_cloud_run_v2_service.backend[0].uri, "")
}
