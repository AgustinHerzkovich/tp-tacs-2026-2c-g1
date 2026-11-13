// OpenTelemetry SDK for the browser. It only traces the activity search (see
// searchTrace.ts) and posts the spans straight to the OpenTelemetry Collector over OTLP/HTTP.
// It is off unless NEXT_PUBLIC_OTEL_ENABLED is "true" at build time, as docker-compose.yaml sets it
// locally; the cloud build leaves it off.

import { OTLPTraceExporter } from "@opentelemetry/exporter-trace-otlp-http";
import { resourceFromAttributes } from "@opentelemetry/resources";
import { BatchSpanProcessor, WebTracerProvider } from "@opentelemetry/sdk-trace-web";
import { ATTR_SERVICE_NAME } from "@opentelemetry/semantic-conventions";

const SERVICE_NAME = "planazo-frontend-web";
const DEFAULT_ENDPOINT = "http://localhost:4318";

let started = false;

/** Starts the browser SDK once: registers the provider, the W3C trace context propagator and the
 * synchronous context manager. Without it every helper in searchTrace.ts is a no-op. */
export function initBrowserTracing(): void {
  if (started || typeof window === "undefined") return;
  started = true;
  if (process.env.NEXT_PUBLIC_OTEL_ENABLED !== "true") return;

  const endpoint = process.env.NEXT_PUBLIC_OTEL_EXPORTER_OTLP_ENDPOINT || DEFAULT_ENDPOINT;
  const provider = new WebTracerProvider({
    resource: resourceFromAttributes({ [ATTR_SERVICE_NAME]: SERVICE_NAME }),
    spanProcessors: [new BatchSpanProcessor(new OTLPTraceExporter({ url: `${endpoint}/v1/traces` }))],
  });
  provider.register();
}
