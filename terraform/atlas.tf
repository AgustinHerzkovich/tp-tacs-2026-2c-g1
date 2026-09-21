resource "mongodbatlas_project" "planazo" {
  name   = local.name
  org_id = var.atlas_org_id
}

resource "mongodbatlas_advanced_cluster" "planazo" {
  project_id   = mongodbatlas_project.planazo.id
  name         = local.name
  cluster_type = "REPLICASET"

  replication_specs = [{
    region_configs = [{
      provider_name         = "TENANT"
      backing_provider_name = "GCP"
      region_name           = var.atlas_region
      priority              = 7

      electable_specs = {
        instance_size = "M0"
      }
    }]
  }]
}

resource "mongodbatlas_project_ip_access_list" "application" {
  for_each = var.atlas_access_cidrs

  project_id = mongodbatlas_project.planazo.id
  cidr_block = each.value
  comment    = "Local or Cloud Run application access"
}
