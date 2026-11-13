// Manual spans of the activity search in the browser. They only use the OpenTelemetry API, so they
// are no-ops until browserTracing.ts registers the SDK.
//
// The browser keeps the active span only while code runs synchronously: after an `await` it is
// lost. That is why the HTTP span is started at the very beginning of `request()` in api.ts,
// before it awaits the Keycloak token, and why the fetch auto-instrumentation is not used (its
// span would start after that await, detached from the search, in a trace of its own).

import {
  context,
  propagation,
  SpanKind,
  SpanStatusCode,
  trace,
  type Attributes,
  type Context,
  type Span,
} from "@opentelemetry/api";

const TRACER_NAME = "planazo-frontend-web/activity-search";
export const SEARCH_SPAN = "activities.search";

function tracer() {
  return trace.getTracer(TRACER_NAME);
}

function errorType(error: unknown): string {
  return error instanceof Error ? error.name : "Error";
}

/** One activity search seen from the browser: from the moment it is requested until its result is
 * ready to be shown. Ending it more than once has no effect. */
export class ActivitySearchTrace {
  private readonly span: Span;
  private readonly spanContext: Context;
  private ended = false;

  constructor(attributes: Attributes) {
    this.span = tracer().startSpan(SEARCH_SPAN, { attributes });
    this.spanContext = trace.setSpan(context.active(), this.span);
  }

  /** Runs `request` with this search as the active span, so the HTTP span becomes its child. */
  run<T>(request: () => T): T {
    return context.with(this.spanContext, request);
  }

  succeed(results: number): void {
    this.finish(() => this.span.setAttribute("search.results", results));
  }

  fail(error: unknown): void {
    this.finish(() => {
      this.span.setAttribute("error.type", errorType(error));
      this.span.setStatus({ code: SpanStatusCode.ERROR, message: errorType(error) });
    });
  }

  /** The search was superseded (filters changed or the screen closed) before its result arrived. */
  cancel(): void {
    this.finish(() => this.span.setAttribute("search.cancelled", true));
  }

  private finish(describe: () => void): void {
    if (this.ended) return;
    this.ended = true;
    describe();
    this.span.end();
  }
}

export interface TracedRequest {
  /** Headers that carry the trace context (`traceparent`) to the next hop. */
  headers: Record<string, string>;
  end(status: number): void;
  fail(error: unknown): void;
}

/** Starts a CLIENT span for a call to one of this app's API routes, as a child of the active span.
 * Returns null when nothing is being traced, so calls outside the activity search stay untouched.
 * `path` must not include the query string: the searched text never goes into a span. */
export function startTracedRequest(method: string, path: string): TracedRequest | null {
  const parent = context.active();
  if (!trace.getSpan(parent)) return null;

  const span = tracer().startSpan(
    `HTTP ${method} ${path}`,
    { kind: SpanKind.CLIENT, attributes: { "http.request.method": method, "url.path": path } },
    parent,
  );
  const headers: Record<string, string> = {};
  propagation.inject(trace.setSpan(parent, span), headers);

  let ended = false;
  const finish = (describe: () => void) => {
    if (ended) return;
    ended = true;
    describe();
    span.end();
  };
  return {
    headers,
    end: (status) =>
      finish(() => {
        span.setAttribute("http.response.status_code", status);
        if (status >= 400) span.setStatus({ code: SpanStatusCode.ERROR, message: `HTTP ${status}` });
      }),
    fail: (error) =>
      finish(() => {
        span.setAttribute("error.type", errorType(error));
        span.setStatus({ code: SpanStatusCode.ERROR, message: errorType(error) });
      }),
  };
}
