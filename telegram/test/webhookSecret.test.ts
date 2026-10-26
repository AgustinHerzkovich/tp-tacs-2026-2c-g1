import { test } from "node:test";
import assert from "node:assert/strict";
import { isValidWebhookSecret } from "../src/utils/webhookSecret";

test("accepts the configured secret", () => {
  assert.equal(isValidWebhookSecret("s3cret-token", "s3cret-token"), true);
});

test("rejects a missing, different or differently sized secret", () => {
  assert.equal(isValidWebhookSecret(undefined, "s3cret-token"), false);
  assert.equal(isValidWebhookSecret("s3cret-tokeX", "s3cret-token"), false);
  assert.equal(isValidWebhookSecret("s3cret", "s3cret-token"), false);
  assert.equal(isValidWebhookSecret(["s3cret-token"], "s3cret-token"), false);
});

test("fails closed when no secret is configured", () => {
  assert.equal(isValidWebhookSecret("", ""), false);
  assert.equal(isValidWebhookSecret("anything", ""), false);
});
