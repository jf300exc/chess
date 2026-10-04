/** Run against a disposable test server: CHESS_WEB_URL=http://localhost:8094 node scripts/test-web-client.cjs.
 * Requires Playwright (npm install --no-save playwright), Chrome, and a running chess server.
 * Creates uniquely named users/games. Never calls DELETE /db.
 */
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { chromium } = require('playwright');
const url = process.env.CHESS_WEB_URL || 'http://localhost:8094';
const prefix = `web-${Date.now()}`;
const tag = Date.now().toString(36);
const whiteName = `Ada-${tag}`, blackName = `Ben-${tag}`, observerName = `Sam-${tag}`;
const output = process.env.CHESS_WEB_SCREENSHOTS;
const password = 'browser-test-password';
const errors = [];
let browser;
async function expectText(page, selector, text) {
  await page.waitForFunction(({ selector, text }) => document.querySelector(selector)?.textContent.includes(text), { selector, text });
}
async function noOverflow(page) {
  assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), 'The mobile view must fit the viewport');
}
async function capture(page, name) {
  if (output) { fs.mkdirSync(output, { recursive: true }); await page.screenshot({ path: path.join(output, name), fullPage: true }); }
}
async function register(page, name) {
  await page.goto(url);
  page.on('pageerror', (error) => errors.push(error.message));
  await page.click('#register-tab');
  await page.fill('#username', name); await page.fill('#password', password); await page.fill('#email', 'not-an-email');
  await page.click('#auth-submit'); await page.waitForSelector('#lobby-view:visible');
}
async function square(page, name) { await page.locator(`[data-square="${name}"]`).tap(); }
async function move(page, from, to, nextTurn) {
  await square(page, from); await page.waitForSelector(`[data-square="${to}"].legal`); await square(page, to);
  await expectText(page, '#turn', nextTurn);
}
(async () => {
  browser = await chromium.launch({ executablePath: process.env.CHROME_BIN || '/usr/bin/google-chrome-stable', headless: true, args: ['--no-sandbox'] });
  const whiteContext = await browser.newContext({ viewport: { width: 390, height: 844 }, isMobile: true, hasTouch: true });
  const blackContext = await browser.newContext({ viewport: { width: 1440, height: 950 }, hasTouch: true });
  const observerContext = await browser.newContext({ viewport: { width: 320, height: 740 }, isMobile: true, hasTouch: true });
  const white = await whiteContext.newPage(), black = await blackContext.newPage(), observer = await observerContext.newPage();
  await white.goto(url); await noOverflow(white); await capture(white, 'web-mobile-sign-in.png');
  await white.fill('#username', 'unknown-user'); await white.fill('#password', 'wrong'); await white.click('#auth-submit');
  await white.waitForSelector('#alert:visible');
  assert.equal(await white.locator('#lobby-view').isVisible(), false);
  await register(white, whiteName); await noOverflow(white);
  // Render hostile user content as text; no inline handlers or HTML injection.
  const hostileName = `${prefix} <img src=x onerror=alert(1)>`;
  await white.fill('#game-name', hostileName); await white.click('#create-form button');
  const hostileCard = white.locator('.game-card').filter({ hasText: hostileName });
  await hostileCard.waitFor(); assert.equal(await hostileCard.locator('img').count(), 0);
  const gameName = `Sunday chess · ${tag.slice(-4)}`;
  await white.fill('#game-name', gameName); await white.click('#create-form button');
  const card = white.locator('.game-card').filter({ hasText: gameName });
  await card.waitFor();
  await capture(white, 'web-mobile-lobby.png');
  await card.getByRole('button', { name: 'Play White', exact: true }).click(); await expectText(white, '#turn', 'Your turn');
  await register(black, blackName);
  await black.locator('.game-card').filter({ hasText: gameName }).getByRole('button', { name: 'Play Black', exact: true }).click();
  await expectText(black, '#connection', 'Connected');
  await expectText(white, '#opponent-name', blackName);
  assert.equal(await black.locator('.square').first().getAttribute('data-square'), 'h1', 'Black should see an oriented board');
  await register(observer, observerName);
  await observer.locator('.game-card').filter({ hasText: gameName }).getByRole('button', { name: 'Watch', exact: true }).click();
  await expectText(observer, '#match-id', 'Observer');
  await noOverflow(white); await noOverflow(black); await noOverflow(observer);
  await square(observer, 'e2'); await observer.waitForSelector('[data-square="e4"].legal'); await square(observer, 'e4');
  assert.equal(await observer.locator('[data-square="e2"] .piece').count(), 1, 'An observer cannot submit moves');
  assert.equal(await observer.locator('#resign').isVisible(), false);
  await square(white, 'e2'); await white.waitForSelector('[data-square="e4"].legal');
  await white.click('#flip'); assert.equal(await white.locator('.square').first().getAttribute('data-square'), 'h1');
  await white.click('#flip'); await white.click('#cancel-selection');
  assert.equal(await white.locator('.selected').count(), 0);
  await capture(white, 'web-mobile-board.png'); await capture(black, 'web-desktop-board.png');
  await move(white, 'f2', 'f3', 'Black to move'); await expectText(black, '#turn', 'Your turn');
  await expectText(observer, '#turn', 'Black to move');
  await move(black, 'e7', 'e5', 'White to move'); await expectText(white, '#turn', 'Your turn');
  // Reload preserves the authenticated seat and live board.
  await white.reload(); await expectText(white, '#turn', 'Your turn');
  assert.equal(await white.locator('[data-square="f3"] .piece').count(), 1);
  // A mobile network drop must reconnect and reload, without resubmitting a move.
  await whiteContext.setOffline(true); await white.reload().catch(() => {}); await whiteContext.setOffline(false);
  await white.goto(url); await expectText(white, '#turn', 'Your turn');
  await move(white, 'g2', 'g4', 'Black to move'); await expectText(black, '#turn', 'Your turn');
  await move(black, 'd8', 'h4', 'wins'); await expectText(white, '#turn', 'Black wins · Checkmate');
  await expectText(observer, '#turn', 'Black wins · Checkmate');
  assert.equal(await white.locator('#resign').isDisabled(), true);
  await capture(white, 'web-mobile-checkmate.png');
  await observer.click('#leave'); await observer.waitForSelector('#lobby-view:visible');
  await observer.click('#logout'); await observer.waitForSelector('#auth-view:visible');
  // Ordinary account login and resignation require explicit confirmation.
  await observer.fill('#username', observerName); await observer.fill('#password', password); await observer.click('#auth-submit');
  await observer.waitForSelector('#lobby-view:visible'); await noOverflow(observer);
  console.log('Passed live multiplayer, reload, network recovery, and checkmate; checking resignation.');
  await observer.fill('#game-name', `${prefix}-resign`); await observer.click('#create-form button');
  await observer.locator('.game-card').filter({ hasText: `${prefix}-resign` }).getByRole('button', { name: 'Play White', exact: true }).click();
  await expectText(observer, '#turn', 'Your turn');
  await observer.click('#resign'); await observer.click('#confirm-cancel'); await expectText(observer, '#turn', 'Your turn');
  await observer.click('#resign'); await observer.click('#confirm-ok'); await expectText(observer, '#turn', 'Game over');
  await observer.reload(); await expectText(observer, '#turn', 'Game over');
  async function toLobby(page) {
    await page.click('#leave');
    if (await page.locator('#confirm-dialog').isVisible()) await page.click('#confirm-ok');
    await page.waitForSelector('#lobby-view:visible');
  }
  async function freshMatch(name) {
    await toLobby(white); await toLobby(black);
    await white.fill('#game-name', name); await white.click('#create-form button');
    await white.locator('.game-card').filter({ hasText: name }).getByRole('button', { name: 'Play White', exact: true }).click();
    await expectText(white, '#turn', 'Your turn');
    await black.click('#refresh');
    await black.locator('.game-card').filter({ hasText: name }).getByRole('button', { name: 'Play Black', exact: true }).click();
    await expectText(black, '#connection', 'Connected');
  }
  await freshMatch(`${prefix}-promotion`);
  for (const [page, from, to, next] of [
    [white, 'a2', 'a4', 'Black to move'], [black, 'h7', 'h5', 'White to move'],
    [white, 'a4', 'a5', 'Black to move'], [black, 'h5', 'h4', 'White to move'],
    [white, 'a5', 'a6', 'Black to move'], [black, 'h4', 'h3', 'White to move'],
    [white, 'a6', 'b7', 'Black to move'], [black, 'h3', 'g2', 'White to move'],
  ]) { await expectText(page, '#turn', 'Your turn'); await move(page, from, to, next); }
  await expectText(white, '#turn', 'Your turn');
  await square(white, 'b7'); await square(white, 'a8'); await white.waitForSelector('#promotion:visible');
  assert.equal(await white.locator('#promotion [data-piece]').count(), 4);
  await white.click('#cancel-promotion'); assert.equal(await white.locator('[data-square="b7"] .piece').count(), 1);
  await square(white, 'b7'); await square(white, 'a8'); await white.click('[data-piece="KNIGHT"]');
  await expectText(white, '#turn', 'Black to move');
  assert((await white.locator('[data-square="a8"]').getAttribute('aria-label')).includes('White Knight'));
  await expectText(black, '#turn', 'Your turn');
  await square(black, 'g2'); await square(black, 'h1'); await black.click('[data-piece="QUEEN"]');
  await expectText(black, '#turn', 'White to move');
  assert((await black.locator('[data-square="h1"]').getAttribute('aria-label')).includes('Black Queen'));
  await freshMatch(`${prefix}-castle`);
  for (const [page, from, to, next] of [
    [white, 'e2', 'e4', 'Black to move'], [black, 'a7', 'a6', 'White to move'],
    [white, 'g1', 'f3', 'Black to move'], [black, 'a6', 'a5', 'White to move'],
    [white, 'f1', 'e2', 'Black to move'], [black, 'b7', 'b6', 'White to move'],
    [white, 'e1', 'g1', 'Black to move'],
  ]) { await expectText(page, '#turn', 'Your turn'); await move(page, from, to, next); }
  assert((await white.locator('[data-square="f1"]').getAttribute('aria-label')).includes('White Rook'));
  assert.equal(await white.locator('[data-square="h1"] .piece').count(), 0);
  await freshMatch(`${prefix}-en-passant`);
  for (const [page, from, to, next] of [
    [white, 'e2', 'e4', 'Black to move'], [black, 'a7', 'a6', 'White to move'],
    [white, 'e4', 'e5', 'Black to move'], [black, 'd7', 'd5', 'White to move'],
    [white, 'e5', 'd6', 'Black to move'],
  ]) { await expectText(page, '#turn', 'Your turn'); await move(page, from, to, next); }
  assert.equal(await white.locator('[data-square="d5"] .piece').count(), 0);
  assert((await white.locator('[data-square="d6"]').getAttribute('aria-label')).includes('White Pawn'));
  console.log('Passed promotion for both colors, cancel promotion, castling, and en passant.');
  assert.deepEqual(errors, [], 'No browser runtime errors');
  assert.equal((await white.request.get(`${url}/api/index.html`)).status(), 200, 'API playground is still available');
  console.log('PASS: auth, lobby, XSS text rendering, touch moves, White/Black orientation, observer permissions, flip/cancel, live broadcasts, reload/network recovery, checkmate, resignation, sign-out, 320/390/1440px layouts.');
})().catch((error) => { console.error(error); process.exitCode = 1; }).finally(async () => { await browser?.close(); });
