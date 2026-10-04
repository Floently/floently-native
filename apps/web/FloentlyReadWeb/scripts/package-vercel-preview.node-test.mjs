import assert from "node:assert/strict";
import { mkdtemp, mkdir, readFile, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import test from "node:test";
import {
  packageReadWebVercelPreview,
  readWebPreviewOutputConfig,
} from "./package-vercel-preview.mjs";

test("preview output serves static files before SPA fallback", () => {
  assert.deepEqual(readWebPreviewOutputConfig(), {
    version: 3,
    routes: [
      { handle: "filesystem" },
      { src: "/.*", dest: "/index.html" },
    ],
  });
});

test("preview packaging copies only built web output into Vercel static output", async () => {
  const root = await mkdtemp(join(tmpdir(), "read-web-preview-"));
  const dist = join(root, "dist");
  const output = join(root, ".vercel", "output");

  try {
    await mkdir(join(dist, "assets"), { recursive: true });
    await writeFile(join(dist, "index.html"), "<main>Read</main>", "utf8");
    await writeFile(join(dist, "assets", "app.js"), "export {};", "utf8");

    await packageReadWebVercelPreview({
      distDirectory: dist,
      outputDirectory: output,
    });

    assert.equal(
      await readFile(join(output, "static", "index.html"), "utf8"),
      "<main>Read</main>",
    );
    assert.equal(
      await readFile(join(output, "static", "assets", "app.js"), "utf8"),
      "export {};",
    );
    assert.deepEqual(
      JSON.parse(await readFile(join(output, "config.json"), "utf8")),
      readWebPreviewOutputConfig(),
    );
  } finally {
    await rm(root, { recursive: true, force: true });
  }
});

test("preview packaging fails closed when the production bundle is missing", async () => {
  const root = await mkdtemp(join(tmpdir(), "read-web-preview-missing-"));

  try {
    await assert.rejects(
      () => packageReadWebVercelPreview({
        distDirectory: join(root, "missing-dist"),
        outputDirectory: join(root, ".vercel", "output"),
      }),
      /Build the app first/,
    );
  } finally {
    await rm(root, { recursive: true, force: true });
  }
});
