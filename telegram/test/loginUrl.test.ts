import { test } from "node:test";
import assert from "node:assert/strict";
import { loginUrl } from "../src/handlers/handleLogin";

test("the login link carries the single-use code and never the chat id", () => {
  const url = new URL(loginUrl("Xb7k2mQ9_pL4vN8rT1wYz3Hc6Fj0Ds5Ae-GuKoPiWqM"));

  assert.equal(url.pathname, "/login");
  assert.equal(url.searchParams.get("t"), "true");
  assert.equal(url.searchParams.get("link"), "Xb7k2mQ9_pL4vN8rT1wYz3Hc6Fj0Ds5Ae-GuKoPiWqM");
  assert.equal(url.searchParams.has("cid"), false);
  // "code" is reserved by OAuth: Keycloak rejects a redirect_uri that already carries it.
  assert.equal(url.searchParams.has("code"), false);
});

test("the code is URL-encoded", () => {
  const url = new URL(loginUrl("a&b=c"));

  assert.equal(url.searchParams.get("link"), "a&b=c");
});
