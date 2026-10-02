import { expect, test, type Page, type Route } from "@playwright/test";

const pastedProject = {
  id: "e2e-paste",
  title: "E2E project heading",
  kind: "document",
  status: "ready",
  sourceType: "text",
  sourceUrl: null,
  language: "en",
  textHash: "e2e-paste-hash",
  wordCount: 18,
  characterCount: 118,
  createdAt: "2026-10-02T00:00:00Z",
  updatedAt: "2026-10-02T00:00:00Z",
  lastOpenedAt: null,
  progress: null,
  rawText:
    "E2E project heading\n\nThis paragraph is loaded through the mocked backend and mapped by the real Rust/WASM Read worker.",
};

const sessionPayload = {
  apiKey: "e2e-token",
  user: {
    id: "e2e-user",
    email: "reader@example.com",
    name: "E2E Reader",
    plan: "free",
    readPlan: "reader",
    readAccess: true,
    readPolicy: {
      product: "read",
      tier: "paid",
      plan: "reader",
      features: {
        canExportAudio: true,
        canTranslate: true,
      },
      voiceAccess: {
        defaultTier: "standard_reader_plus",
        allowedTiers: ["standard_reader", "standard_reader_plus"],
        premiumStudioAllowed: false,
      },
      limits: {
        standardReadingSeconds: 18_000,
      },
    },
  },
};

function json(route: Route, body: unknown, status = 200) {
  return route.fulfill({
    status,
    contentType: "application/json",
    body: JSON.stringify(body),
  });
}

async function installBackend(
  page: Page,
  options: {
    authenticated?: boolean;
    failUploads?: boolean;
  } = {},
): Promise<void> {
  const authenticated = options.authenticated ?? true;

  await page.route("**/api/v1/auth/google/config", (route) =>
    json(route, { enabled: false }),
  );

  await page.route("**/api/v1/auth/session", (route) =>
    authenticated
      ? json(route, sessionPayload)
      : json(route, { detail: "No active session." }, 401),
  );

  await page.route("**/api/v1/auth/login/password", (route) =>
    json(route, sessionPayload),
  );

  await page.route("**/api/v1/projects?**", (route) =>
    json(route, { projects: [] }),
  );

  await page.route("**/api/v1/projects/from-text", (route) =>
    json(route, { project: pastedProject }),
  );

  await page.route("**/api/v1/projects/e2e-paste/progress", (route) =>
    json(route, { progress: null }),
  );

  await page.route("**/api/v1/projects/e2e-paste", (route) =>
    json(route, { project: pastedProject }),
  );

  await page.route("**/api/v1/read/usage", (route) =>
    json(route, {
      periodKey: "2026-10",
      plan: "reader",
      usage: {
        standardReadingSeconds: 1_800,
        audioExportSeconds: 0,
        transcriptionSeconds: 0,
        translationCharacters: 2_000,
        providerRequests: 2,
      },
      limits: {
        standardReadingSeconds: 18_000,
        audioExportSeconds: 3_600,
        transcriptionSeconds: 0,
        translationCharacters: 50_000,
        providerRequests: 100,
      },
      remaining: {
        standardReadingSeconds: 16_200,
        audioExportSeconds: 3_600,
        transcriptionSeconds: 0,
        translationCharacters: 48_000,
        providerRequests: 98,
      },
      updatedAt: "2026-10-02T00:00:00Z",
    }),
  );

  await page.route("**/api/v1/billing/read-pricing?**", (route) => {
    const url = new URL(route.request().url());
    const plan = url.searchParams.get("plan") === "creator"
      ? "creator"
      : "reader";
    const interval = url.searchParams.get("interval") === "yearly"
      ? "yearly"
      : "monthly";

    return json(route, {
      ok: true,
      data: {
        amount_display:
          plan === "creator"
            ? interval === "yearly" ? "€199.00" : "€19.99"
            : interval === "yearly" ? "€99.00" : "€9.99",
        amount_minor: plan === "creator" ? 1_999 : 999,
        band: "D",
        country: "Finland",
        country_code: "FI",
        currency: "EUR",
        economy_hours: plan === "creator" ? 150 : 75,
        interval,
        natural_hours: plan === "creator" ? 10 : 2,
        plan,
        premium_hours: plan === "creator" ? 2 : 0,
        stripe_price_lookup_key: `e2e-${plan}-${interval}`,
        stripe_product_lookup_key: `e2e-${plan}`,
      },
    });
  });

  await page.route("**/api/voices/unified", (route) =>
    json(route, {
      data: {
        default: "e2e:en-US",
        voices: [
          {
            id: "e2e:en-US",
            name: "E2E English",
            language: "en",
            locale: "en-US",
          },
          {
            id: "e2e:fi-FI",
            name: "E2E Finnish",
            language: "fi",
            locale: "fi-FI",
          },
        ],
      },
    }),
  );

  if (options.failUploads) {
    await page.route("**/api/v1/projects/upload", (route) =>
      json(route, { detail: "E2E extractor unavailable." }, 503),
    );
  }
}

test("protected routing preserves a safe returnTo and rejects an external one", async ({
  page,
}) => {
  await installBackend(page, { authenticated: false });

  await page.goto("/app/preferences?source=e2e");

  await expect(page).toHaveURL(/\/login\?returnTo=/);
  const redirected = new URL(page.url());
  expect(redirected.searchParams.get("returnTo")).toBe(
    "/app/preferences?source=e2e",
  );

  await page.goto(
    `/login?returnTo=${encodeURIComponent("https://example.com/escape")}`,
  );
  await page.getByLabel("Email").fill("reader@example.com");
  await page.getByLabel("Password").fill("safe-password");
  await page.getByRole("button", { name: "Sign in" }).click();

  await expect(page).toHaveURL(/\/app\/library$/);
  await expect(page.getByRole("heading", { name: "Library", exact: true })).toBeVisible();
});

test("authenticated app navigation reaches the core Read product surfaces", async ({
  page,
}) => {
  await installBackend(page);
  await page.goto("/app/library");

  await expect(page.getByRole("heading", { name: "Library", exact: true })).toBeVisible();

  await page.getByRole("link", { name: "Import", exact: true }).click();
  await expect(page).toHaveURL(/\/app\/import$/);
  await expect(page.getByRole("heading", { name: "Import", exact: true })).toBeVisible();
  await expect(
    page.getByText("PDFs keep their original pages. Websites keep their live page."),
  ).toBeVisible();

  await page.getByRole("link", { name: "Reader", exact: true }).click();
  await expect(page).toHaveURL(/\/app\/reader$/);

  await page.getByRole("link", { name: "Preferences", exact: true }).click();
  await expect(page).toHaveURL(/\/app\/preferences$/);
  await expect(page.getByRole("heading", { name: "Preferences", exact: true })).toBeVisible();
  await expect(
    page.getByLabel("Voice").locator('option[value="e2e:en-US"]'),
  ).toHaveText(/E2E English/);

  await page.getByRole("link", { name: "Account", exact: true }).click();
  await expect(page).toHaveURL(/\/app\/account$/);
  await expect(page.getByRole("heading", { name: "Account", exact: true })).toBeVisible();
  await expect(page.getByText("10%")).toBeVisible();
  await expect(page.getByText("Paid Read access")).toBeVisible();

  await page.getByRole("link", { name: "Plan", exact: true }).click();
  await expect(page).toHaveURL(/\/app\/subscription$/);
  await expect(
    page.getByRole("heading", { name: "Plan and billing" }),
  ).toBeVisible();
  await expect(page.getByText("€9.99")).toBeVisible();
  await expect(page.getByText("€19.99")).toBeVisible();
});

test("keyboard users can skip to content and route focus follows navigation", async ({
  page,
}) => {
  await installBackend(page);
  await page.goto("/app/library");

  await page.keyboard.press("Tab");
  const skipLink = page.getByRole("link", { name: "Skip to content" });
  await expect(skipLink).toBeFocused();

  await skipLink.press("Enter");
  const main = page.locator("#read-main-content");
  await expect(main).toBeFocused();

  await page.getByRole("link", { name: "Preferences", exact: true }).click();
  await expect(page).toHaveURL(/\/app\/preferences$/);
  await expect(main).toBeFocused();
});

test("interface language persists and updates protected shell labels", async ({
  page,
}) => {
  await installBackend(page);
  await page.goto("/app/preferences");

  const language = page.getByLabel("Interface language");
  await language.selectOption("fi");

  await expect(
    page.getByRole("link", { name: "Kirjasto", exact: true }),
  ).toBeVisible();
  await expect(page.locator("html")).toHaveAttribute("lang", "fi");

  await page.reload();

  await expect(page.locator("#read-interface-language")).toHaveValue("fi");
  await expect(
    page.getByRole("link", { name: "Kirjasto", exact: true }),
  ).toBeVisible();
});

test("reading preferences persist through a real browser reload", async ({
  page,
}) => {
  await installBackend(page);
  await page.goto("/app/preferences");

  const theme = page.getByLabel("Theme");
  await theme.selectOption("dark");
  await expect(page.locator("html")).toHaveAttribute("data-read-theme", "dark");

  const speed = page.getByLabel("Speed");
  await speed.selectOption("1.75");
  await expect(page.getByText("Persisted default: 1.75×")).toBeVisible();

  await page.reload();

  await expect(page.locator("html")).toHaveAttribute("data-read-theme", "dark");
  await expect(page.getByLabel("Theme")).toHaveValue("dark");
  await expect(page.getByLabel("Speed")).toHaveValue("1.75");
  await expect(page.getByText("Persisted default: 1.75×")).toBeVisible();
});

test("pasted text opens through the real Rust/WASM synced reader", async ({
  page,
}) => {
  await installBackend(page);
  await page.goto("/app/import");

  await page.getByRole("button", { name: "Paste text" }).click();
  await page.getByLabel("Text").fill(pastedProject.rawText);
  await page.getByRole("button", { name: "Save and read" }).click();

  await expect(page).toHaveURL(/\/app\/project\/e2e-paste$/);
  await expect(
    page.getByRole("heading", { name: pastedProject.title, level: 1 }),
  ).toBeVisible();
  await expect(page.locator(".reader-surface")).toBeVisible();
  await expect(
    page.getByText(/loaded through the mocked backend/),
  ).toBeVisible();
});

test("opening a PDF keeps the original iframe visible when extraction fails", async ({
  page,
}) => {
  await installBackend(page, { failUploads: true });
  await page.goto("/app/import");

  const fileInput = page.locator('input[type="file"]');
  await fileInput.setInputFiles({
    name: "source-preserved.pdf",
    mimeType: "application/pdf",
    buffer: Buffer.from(
      "%PDF-1.4\n1 0 obj<</Type/Catalog>>endobj\ntrailer<</Root 1 0 R>>\n%%EOF",
    ),
  });

  await expect(page).toHaveURL(/\/app\/document\//);
  await expect(page.getByText("source-preserved.pdf")).toBeVisible();
  await expect(
    page.locator('iframe[title="source-preserved.pdf"]'),
  ).toBeVisible();
  await expect(page.getByText(/Original pages/).first()).toBeVisible();
  await expect(page.locator(".reader-surface")).toHaveCount(0);
});

test("mobile shell keeps the primary import action reachable", async ({
  page,
}) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await installBackend(page);
  await page.goto("/app/account");

  await expect(page.getByRole("heading", { name: "Account", exact: true })).toBeVisible();
  await expect(page.locator(".app-sidebar")).toBeHidden();
  await expect(page.getByRole("button", { name: "Import" })).toBeVisible();

  await page.getByRole("button", { name: "Import" }).click();
  await expect(page).toHaveURL(/\/app\/import$/);
  await expect(page.getByRole("heading", { name: "Import", exact: true })).toBeVisible();
});
