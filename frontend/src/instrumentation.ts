// OpenTelemetry for the Next.js server. Next.js already creates a span for every
// request it serves and continues the `traceparent` the browser sends; @vercel/otel registers the
// SDK that exports those spans and instruments `fetch`, so the proxy's call to the backend carries
// the trace on. It only starts where OTEL_EXPORTER_OTLP_ENDPOINT is set (docker-compose.yaml), never
// in the cloud.

export async function register() {
  if (process.env.NEXT_RUNTIME !== "nodejs" || !process.env.OTEL_EXPORTER_OTLP_ENDPOINT) return;

  const { registerOTel } = await import("@vercel/otel");
  registerOTel({
    serviceName: process.env.OTEL_SERVICE_NAME || "planazo-frontend",
    instrumentationConfig: {
      fetch: {
        // Only the backend receives the trace context; other hosts (e.g. Keycloak) never see it.
        propagateContextUrls: [process.env.BACKEND_URL || "http://localhost:8080"],
      },
    },
  });
}
