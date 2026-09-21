variable "gcp_project_id" {
  description = "Existing GCP project with billing enabled."
  type        = string
}

variable "gcp_region" {
  description = "Region used by Cloud Run, Cloud SQL, Artifact Registry and Storage."
  type        = string
  default     = "us-central1"
}

variable "environment" {
  description = "Short environment name used in resource names."
  type        = string
  default     = "prod"

  validation {
    condition     = can(regex("^[a-z][a-z0-9-]{0,10}$", var.environment))
    error_message = "environment must start with a letter and contain at most 11 lowercase letters, digits or hyphens."
  }
}

variable "resource_prefix" {
  description = "Prefix used in globally visible resource names."
  type        = string
  default     = "planazo"
}

variable "backend_image" {
  description = "Backend image URI. Leave empty until the first image is pushed."
  type        = string
  default     = ""
}

variable "frontend_image" {
  description = "Frontend image URI built with the production Keycloak URL. Leave empty initially."
  type        = string
  default     = ""
}

variable "keycloak_image" {
  description = "Custom Keycloak image URI containing the realm import and theme. Leave empty initially."
  type        = string
  default     = ""
}

variable "frontend_url" {
  description = "Public frontend origin used by Keycloak. Set after the first frontend deployment."
  type        = string
  default     = ""
}

variable "keycloak_realm" {
  type    = string
  default = "solnotfound"
}

variable "keycloak_frontend_client_id" {
  type    = string
  default = "solnotfoundFrontend"
}

variable "keycloak_backend_audience" {
  type    = string
  default = "solnotfoundBackend"
}

variable "cloud_sql_tier" {
  description = "Small non-HA PostgreSQL instance suitable for the short-lived academic environment."
  type        = string
  default     = "db-f1-micro"
}

variable "cloud_sql_deletion_protection" {
  description = "Enable for long-lived environments. False permits terraform destroy after the delivery."
  type        = bool
  default     = false
}

variable "atlas_org_id" {
  description = "MongoDB Atlas organization ID."
  type        = string
}

variable "atlas_public_key" {
  description = "MongoDB Atlas API public key. Set with TF_VAR_atlas_public_key."
  type        = string
  sensitive   = true
  default     = null
}

variable "atlas_private_key" {
  description = "MongoDB Atlas API private key. Set with TF_VAR_atlas_private_key."
  type        = string
  sensitive   = true
  default     = null
}

variable "atlas_region" {
  description = "Atlas GCP region name used by the M0 cluster."
  type        = string
  default     = "CENTRAL_US"
}

variable "atlas_access_cidrs" {
  description = "CIDRs allowed to reach Atlas. Cloud Run requires 0.0.0.0/0 without static egress."
  type        = set(string)
  default     = ["0.0.0.0/0"]
}
