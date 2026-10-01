import { describe, expect, it } from "vitest";
import {
  ingestWebsiteIntoReader,
  ingestionBudgets,
} from "./unifiedDocumentIngestion";

describe("unified document ingestion", () => {
  it("keeps websites on the live Browser V2 path", async () => {
    await expect(
      ingestWebsiteIntoReader("https://example.com/course"),
    ).rejects.toThrow("live page");
  });

  it("keeps the production large-book fast-open budget", () => {
    expect(ingestionBudgets.bookSizeThresholdBytes).toBe(1_500_000);
    expect(ingestionBudgets.bookFastOpenBudgetMs).toBe(1_800);
  });
});
