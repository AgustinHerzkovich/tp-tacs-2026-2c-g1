import { test } from "node:test";
import assert from "node:assert/strict";
import { COMMAND_DESCRIPTIONS, MAIN_MENU, mainMenuText } from "../src/utils/command-tree";

test("the greeting lists every main menu command with its description", () => {
  const lines = mainMenuText().split("\n");

  assert.equal(lines.length, MAIN_MENU.length);
  for (const command of MAIN_MENU) {
    assert.ok(COMMAND_DESCRIPTIONS[command], `${command} has no description`);
    assert.ok(lines.some((line) => line.startsWith(`${command} — `)));
  }
});
