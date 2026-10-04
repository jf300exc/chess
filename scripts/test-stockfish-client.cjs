/** Run against a disposable server with Stockfish installed:
 * CHESS_WEB_URL=http://localhost:8095 node scripts/test-stockfish-client.cjs
 * Requires Playwright and Chrome. Creates unique accounts/games; never clears the database.
 */
const assert = require('node:assert/strict');
const { chromium } = require('playwright');
const fs = require('node:fs');
const path = require('node:path');
const url = process.env.CHESS_WEB_URL || 'http://localhost:8095';
const tag = Date.now().toString(36), username = `AI-player-${tag}`, observerName = `AI-observer-${tag}`;
const output = process.env.CHESS_WEB_SCREENSHOTS;
const errors = [];
let browser;
async function text(page, selector, value) {
  await page.waitForFunction(({ selector, value }) => document.querySelector(selector)?.textContent.includes(value), { selector, value });
}
async function fits(page) { assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), 'Layout must fit the viewport'); }
async function capture(page, name) {
  if (output) { fs.mkdirSync(output, { recursive: true }); await page.screenshot({ path: path.join(output, name), fullPage: true }); }
}
async function register(page, name) {
  page.on('pageerror', error => errors.push(error.message));
  await page.goto(url); await page.click('#register-tab');
  await page.fill('#username', name); await page.fill('#password', 'stockfish-test-password'); await page.fill('#email', 'test');
  await page.click('#auth-submit'); await page.waitForSelector('#lobby-view:visible');
}
async function create(page, name, color, mode, level) {
  await page.fill('#game-name', name); await page.selectOption('#opponent', 'stockfish');
  await page.selectOption('#ai-color', color); await page.selectOption('#ai-mode', mode);
  if (level != null) await page.fill('#ai-level', String(level));
  await page.click('#create-form button[type=submit]'); await page.waitForSelector('#match-view:visible');
  await text(page, '#connection', 'Connected');
}
async function resign(page) {
  await page.click('#resign'); await page.click('#confirm-ok'); await text(page, '#turn', 'Game over');
  await page.click('#leave'); await page.waitForSelector('#lobby-view:visible');
}
(async () => {
  browser = await chromium.launch({ executablePath: process.env.CHROME_BIN || '/usr/bin/google-chrome-stable', headless: true, args: ['--no-sandbox'] });
  const context = await browser.newContext({ viewport: { width: 390, height: 844 }, isMobile: true, hasTouch: true });
  const observerContext = await browser.newContext({ viewport: { width: 1440, height: 950 }, hasTouch: true });
  const page = await context.newPage(), observer = await observerContext.newPage();
  await register(page, username); await text(page, '#player-rating', '1500 Elo');
  await page.selectOption('#opponent', 'stockfish'); await fits(page); await capture(page, 'stockfish-mobile-options.png');
  const adaptiveName = `Adaptive · ${tag}`;
  await create(page, adaptiveName, 'WHITE', 'ELO'); await text(page, '#turn', 'Your turn');
  await text(page, '#opponent-name', 'Stockfish');
  await register(observer, observerName);
  await observer.locator('.game-card').filter({ hasText: adaptiveName }).getByRole('button', { name: 'Watch', exact: true }).click();
  await text(observer, '#connection', 'Connected');
  await page.locator('[data-square=e2]').tap(); await page.waitForSelector('[data-square=e4].legal'); await page.locator('[data-square=e4]').tap();
  await page.waitForSelector('[data-square=e2] .piece', { state: 'detached' });
  await text(page, '#turn', 'Your turn');
  await text(observer, '#activity', 'Stockfish');
  assert.equal(await observer.locator('[data-square=e4] .piece').count(), 1);
  await fits(page); await capture(page, 'stockfish-mobile-match.png'); await capture(observer, 'stockfish-desktop-observer.png');
  await page.click('#leave'); await page.waitForSelector('#lobby-view:visible');
  await page.locator('.game-card').filter({ hasText: adaptiveName }).getByRole('button', { name: 'Resume as White' }).click();
  await text(page, '#turn', 'Your turn'); assert.equal(await page.locator('[data-square=e4] .piece').count(), 1);
  await page.reload(); await text(page, '#turn', 'Your turn');
  await resign(page); await text(page, '#player-rating', '1484 Elo');
  await observer.click('#leave'); await observer.waitForSelector('#lobby-view:visible');
  const nextName = `Updated Elo · ${tag}`;
  await create(page, nextName, 'WHITE', 'ELO'); await page.click('#leave'); await page.waitForSelector('#lobby-view:visible');
  await text(page, '.game-list', '1484 Elo');
  for (const level of [0, 20]) {
    await create(page, `Skill ${level} · ${tag}`, 'BLACK', 'SKILL', level);
    await text(page, '#turn', 'Your turn');
    await fits(page); await resign(page); await text(page, '#player-rating', '1484 Elo');
  }
  await page.selectOption('#opponent', 'human');
  const addName = `Add opponent · ${tag}`;
  await page.fill('#game-name', addName); await page.click('#create-form button[type=submit]');
  const card = page.locator('.game-card').filter({ hasText: addName }); await card.waitFor();
  await card.getByRole('button', { name: 'Add Stockfish', exact: true }).click();
  await page.selectOption('#add-ai-color', 'BLACK'); await page.selectOption('#add-ai-mode', 'SKILL'); await page.fill('#add-ai-level', '5');
  await page.locator('#add-ai-form button[type=submit]').click(); await text(page, '#turn', 'Your turn');
  await page.click('#leave'); await page.waitForSelector('#lobby-view:visible');
  await text(page, '.game-list', 'level 5');
  await page.setViewportSize({ width: 320, height: 740 }); await fits(page); await capture(page, 'stockfish-small-lobby.png');
  assert.deepEqual(errors, []);
  console.log('Stockfish browser checks passed: adaptive Elo, levels 0/20, engine White/Black, adding an opponent, observers, reconnect/resume, ratings, and mobile layouts.');
})().catch(error => { console.error(error); process.exitCode = 1; }).finally(async () => { await browser?.close(); });
