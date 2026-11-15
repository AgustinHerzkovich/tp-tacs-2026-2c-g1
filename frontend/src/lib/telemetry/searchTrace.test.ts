import { context, propagation, SpanKind, SpanStatusCode, trace } from "@opentelemetry/api";
import { InMemorySpanExporter, SimpleSpanProcessor } from "@opentelemetry/sdk-trace-base";
import { WebTracerProvider } from "@opentelemetry/sdk-trace-web";
import { afterEach, beforeEach, describe, expect, it } from "vitest";
import { ActivitySearchTrace, SEARCH_SPAN, startTracedRequest } from "./searchTrace";

const exporter = new InMemorySpanExporter();

function finishedSpan(name: string) {
  const span = exporter.getFinishedSpans().find((candidate) => candidate.name === name);
  if (!span) throw new Error(`No span named ${name}`);
  return span;
}

describe("activity search tracing", () => {
  beforeEach(() => {
    new WebTracerProvider({ spanProcessors: [new SimpleSpanProcessor(exporter)] }).register();
  });

  afterEach(() => {
    exporter.reset();
    trace.disable();
    context.disable();
    propagation.disable();
  });

  it("starts the HTTP span as a child of the search and propagates the same trace", () => {
    const search = new ActivitySearchTrace({ "search.filter.text": true });

    const request = search.run(() => startTracedRequest("GET", "/api/activities"));
    request?.end(200);
    search.succeed(3);

    const searchSpan = finishedSpan(SEARCH_SPAN);
    const httpSpan = finishedSpan("HTTP GET /api/activities");
    expect(httpSpan.parentSpanContext?.spanId).toBe(searchSpan.spanContext().spanId);
    expect(httpSpan.kind).toBe(SpanKind.CLIENT);
    expect(httpSpan.attributes["http.response.status_code"]).toBe(200);
    expect(request?.headers.traceparent).toContain(searchSpan.spanContext().traceId);
    expect(request?.headers.traceparent).toContain(httpSpan.spanContext().spanId);
    expect(searchSpan.attributes["search.results"]).toBe(3);
    expect(searchSpan.status.code).toBe(SpanStatusCode.UNSET);
  });

  it("leaves requests outside a traced search untouched", () => {
    expect(startTracedRequest("GET", "/api/notifications")).toBeNull();
    expect(exporter.getFinishedSpans()).toHaveLength(0);
  });

  it("marks a failed response and a failed search as errors", () => {
    const search = new ActivitySearchTrace({});

    search.run(() => startTracedRequest("GET", "/api/activities"))?.end(503);
    search.fail(new TypeError("Failed to fetch"));

    expect(finishedSpan("HTTP GET /api/activities").status.code).toBe(SpanStatusCode.ERROR);
    const searchSpan = finishedSpan(SEARCH_SPAN);
    expect(searchSpan.status.code).toBe(SpanStatusCode.ERROR);
    expect(searchSpan.attributes["error.type"]).toBe("TypeError");
  });

  it("ends a search only once, keeping the first outcome", () => {
    const search = new ActivitySearchTrace({});

    search.cancel();
    search.succeed(5);

    expect(exporter.getFinishedSpans()).toHaveLength(1);
    const searchSpan = finishedSpan(SEARCH_SPAN);
    expect(searchSpan.attributes["search.cancelled"]).toBe(true);
    expect(searchSpan.attributes["search.results"]).toBeUndefined();
  });
});
