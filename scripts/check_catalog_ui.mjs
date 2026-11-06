// Run against a disposable local test origin; never personal browser storage.
import assert from "node:assert/strict";
import fs from "node:fs/promises";
const { chromium } = await import(
  process.env.PLAYWRIGHT_MODULE || "playwright"
);
const browser = await chromium.launch({
  headless: true,
  args: ["--no-sandbox"],
});
const origin = process.env.ONEPUNCH_QA_URL || "http://127.0.0.1:8098";
const output = process.env.ONEPUNCH_QA_OUT || "build/qa-after";
const catalog = JSON.parse(
  await fs.readFile(new URL("../shared/catalog.json", import.meta.url), "utf8"),
);
await fs.mkdir(output, { recursive: true });
let checks = 0;
try {
  for (const width of [1440, 768, 390, 320]) {
    const context = await browser.newContext({
      viewport: { width, height: 900 },
      reducedMotion: "reduce",
    });
    const page = await context.newPage();
    const errors = [];
    page.on("pageerror", (error) => errors.push(error.message));
    await page.goto(origin);
    const nav = page.getByRole("navigation", {
      name: width <= 700 ? "Navegação mobile" : "Navegação principal",
      exact: true,
    });
    await nav.getByRole("button", { name: "Exercícios", exact: true }).click();
    await page
      .getByLabel("Equipamento", { exact: true })
      .selectOption("Máquina");
    await page
      .getByRole("textbox", { name: "Buscar na biblioteca" })
      .fill(" MAQUINA  SUPINO ");
    assert.equal(await page.locator(".exercise-card").count(), 3);
    checks++;
    await page.getByRole("button", { name: "Costas", exact: true }).click();
    await page
      .getByRole("heading", { name: "Nenhum exercício por aqui." })
      .waitFor();
    checks++;
    await page
      .getByRole("button", { name: "Limpar filtros", exact: true })
      .click();
    assert.equal(await page.locator(".exercise-card").count(), catalog.length);
    checks++;
    await page
      .getByRole("textbox", { name: "Buscar na biblioteca" })
      .fill("desenvolvimento maquina");
    await page.screenshot({
      path: `${output}/library-${width}.png`,
      fullPage: true,
    });
    assert(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth,
      ),
    );
    checks++;
    await page.locator(".exercise-card").click();
    await page
      .getByRole("button", { name: "Iniciar treino com este exercício" })
      .click();
    await page
      .getByRole("button", { name: "Adicionar exercício", exact: true })
      .click();
    const dialog = page.getByRole("dialog");
    await dialog
      .getByLabel("Equipamento", { exact: true })
      .selectOption("Máquina");
    await dialog
      .getByRole("textbox", { name: "Buscar exercício para adicionar" })
      .fill("desenvolvimento maquina");
    assert(await dialog.locator(".exercise-list-row").isDisabled());
    checks++;
    assert.match(
      await dialog.locator(".exercise-list-row").innerText(),
      /Já adicionado/,
    );
    checks++;
    await dialog
      .getByRole("textbox", { name: "Buscar exercício para adicionar" })
      .fill("sem-resultados-qa");
    await dialog.getByText(/Nenhum exercício encontrado/).waitFor();
    checks++;
    await page.screenshot({
      path: `${output}/picker-${width}.png`,
      fullPage: true,
    });
    await dialog
      .getByRole("button", { name: "Limpar filtros", exact: true })
      .click();
    assert.equal(
      await dialog.locator(".exercise-list-row").count(),
      catalog.length,
    );
    checks++;
    await dialog
      .getByRole("button", { name: "Fechar janela", exact: true })
      .click();
    await page.screenshot({
      path: `${output}/long-session-${width}.png`,
      fullPage: true,
    });
    const box = await page.locator(".set-check").first().boundingBox();
    assert(box.width >= 44 && box.height >= 44);
    checks++;
    assert(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth,
      ),
    );
    checks++;
    // Stress text enlargement separately from the ordinary screenshots.
    await page.evaluate(() => {
      for (const element of document.querySelectorAll(
        ".session-exercise h2, .session-exercise p, .set-row input, .sets-table-head span, .previous-set",
      )) {
        element.style.fontSize = `${parseFloat(getComputedStyle(element).fontSize) * 1.5}px`;
      }
    });
    assert(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth,
      ),
    );
    checks++;
    await page.screenshot({
      path: `${output}/large-text-session-${width}.png`,
      fullPage: true,
    });
    await page
      .getByLabel("Carga, série 1, Desenvolvimento na máquina", { exact: true })
      .fill("12,5");
    await page
      .getByRole("button", {
        name: "Concluir série 1 de Desenvolvimento na máquina",
        exact: true,
      })
      .click();
    await page
      .getByRole("button", { name: "Finalizar treino", exact: true })
      .click();
    await page
      .getByRole("button", { name: "Salvar treino", exact: true })
      .click();
    await page
      .getByRole("button", { name: "Sincronizar treinos", exact: true })
      .click();
    await page
      .getByRole("status")
      .filter({ hasText: "Tudo sincronizado" })
      .waitFor();
    assert(
      await page.evaluate(() =>
        JSON.parse(localStorage.getItem("onepunch.workspace.v1")).workouts.some(
          (workout) =>
            workout.sets.some(
              (set) =>
                set.exerciseId === "machine-shoulder-press" &&
                set.weight === 12.5,
            ),
        ),
      ),
    );
    checks++;
    assert.deepEqual(errors, []);
    checks++;
    await context.close();
  }
  console.log(
    `${checks} catalog and layout browser checks passed at 1440/768/390/320.`,
  );
} finally {
  await browser.close();
}
