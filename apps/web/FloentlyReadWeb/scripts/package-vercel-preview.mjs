#!/usr/bin/env node
import {
  access,
  cp,
  mkdir,
  rm,
  writeFile,
} from "node:fs/promises";
import { fileURLToPath, pathToFileURL } from "node:url";
import { dirname, join, resolve } from "node:path";

const scriptDirectory = dirname(fileURLToPath(import.meta.url));
const appDirectory = resolve(scriptDirectory, "..");

export function readWebPreviewOutputConfig() {
  return {
    version: 3,
    routes: [
      { handle: "filesystem" },
      { src: "/.*", dest: "/index.html" },
    ],
  };
}

export async function packageReadWebVercelPreview(options = {}) {
  const distDirectory =
    options.distDirectory ?? join(appDirectory, "dist");
  const outputDirectory =
    options.outputDirectory ?? join(appDirectory, ".vercel", "output");
  const staticDirectory = join(outputDirectory, "static");
  const indexPath = join(distDirectory, "index.html");

  try {
    await access(indexPath);
  } catch {
    throw new Error(
      `Read web preview packaging requires ${indexPath}. Build the app first.`,
    );
  }

  await rm(outputDirectory, { recursive: true, force: true });
  await mkdir(staticDirectory, { recursive: true });
  await cp(distDirectory, staticDirectory, {
    recursive: true,
    force: true,
    dereference: true,
  });

  const config = readWebPreviewOutputConfig();
  await writeFile(
    join(outputDirectory, "config.json"),
    `${JSON.stringify(config, null, 2)}\n`,
    "utf8",
  );

  return {
    distDirectory,
    outputDirectory,
    staticDirectory,
    config,
  };
}

const isMain =
  process.argv[1]
  && import.meta.url === pathToFileURL(process.argv[1]).href;

if (isMain) {
  packageReadWebVercelPreview()
    .then(({ outputDirectory }) => {
      console.log(
        `Floently Read preview Build Output API package written to ${outputDirectory}`,
      );
    })
    .catch((error) => {
      console.error(
        error instanceof Error ? error.message : String(error),
      );
      process.exitCode = 1;
    });
}
