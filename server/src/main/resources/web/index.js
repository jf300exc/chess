const $ = (id) => document.getElementById(id);
const glyphs = { KING: '♚', QUEEN: '♛', ROOK: '♜', BISHOP: '♝', KNIGHT: '♞', PAWN: '♟' };
const titleCase = (value) => value ? value[0] + value.slice(1).toLowerCase() : '';
const squareName = ({ row, col }) => String.fromCharCode(96 + col) + row;
const sameSquare = (a, b) => a?.row === b?.row && a?.col === b?.col;
const storage = {
  get(key) { try { return JSON.parse(sessionStorage.getItem(key)); } catch { return null; } },
  set(key, value) { try { value == null ? sessionStorage.removeItem(key) : sessionStorage.setItem(key, JSON.stringify(value)); } catch { /* Private browsing may disable storage. */ } },
};
let session = storage.get('chess-session');
let gameID = session ? storage.get('chess-match') : null;
let socket = null, retryTimer, pendingTimer;
let retryCount = 0, registering = false, flipped = false, selected = null, promotionMoves = [];
let snapshot = null, pending = false, ready = false, role = null, revision = 0, lobbyRequest = 0;

function show(view) {
  for (const name of ['auth', 'lobby', 'match']) $(`${name}-view`).hidden = name !== view;
  $('logout').hidden = !session || view === 'match';
  $('username-label').textContent = session?.username || '';
}
function alertMessage(message = '') {
  $('alert').textContent = message;
  $('alert').hidden = !message;
  if ($('add-ai-dialog').open) {
    $('add-ai-alert').textContent = message;
    $('add-ai-alert').hidden = !message;
  }
}
async function action(task, button) {
  if (button) button.disabled = true;
  alertMessage();
  try { await task(); } catch (error) { alertMessage(error.message || 'Something went wrong. Please try again.'); }
  finally { if (button) button.disabled = false; }
}
async function request(path, method = 'GET', body) {
  const response = await fetch(path, {
    method, headers: { 'Content-Type': 'application/json', ...(session ? { Authorization: session.authToken } : {}) },
    ...(body ? { body: JSON.stringify(body) } : {}), signal: AbortSignal.timeout(12000),
  });
  const data = await response.json();
  if (!response.ok) {
    if (response.status === 401 && session) resetSession();
    throw new Error(response.status === 401 ? 'Sign-in failed or your session expired. Please sign in again.' : data.message || 'The request failed.');
  }
  return data;
}
function stopSocket() {
  clearTimeout(retryTimer);
  clearTimeout(pendingTimer);
  const previous = socket;
  socket = null;
  previous?.close();
  pending = false; ready = false;
}
function resetSession() {
  $('add-ai-dialog').close();
  stopSocket(); session = null; gameID = null; snapshot = null;
  storage.set('chess-session', null); storage.set('chess-match', null);
  lobbyRequest++; setAuthMode(false); show('auth');
}
function setAuthMode(register) {
  registering = register;
  $('login-tab').classList.toggle('active', !register);
  $('register-tab').classList.toggle('active', register);
  $('login-tab').setAttribute('aria-pressed', String(!register));
  $('register-tab').setAttribute('aria-pressed', String(register));
  $('email-field').hidden = !register;
  $('email').required = register;
  $('password').autocomplete = register ? 'new-password' : 'current-password';
  $('auth-title').textContent = register ? 'Make your first move' : 'Welcome back';
  $('auth-hint').textContent = register ? 'Create an account to join the board.' : 'Sign in to find your next match.';
  $('auth-submit').textContent = register ? 'Create account' : 'Sign in';
}
$('login-tab').onclick = () => setAuthMode(false);
$('register-tab').onclick = () => setAuthMode(true);
$('auth-form').onsubmit = (event) => {
  event.preventDefault();
  action(async () => {
    const credentials = { username: $('username').value.trim(), password: $('password').value };
    if (!credentials.username) throw new Error('Please enter a username.');
    if (registering) credentials.email = $('email').value.trim();
    session = await request(registering ? '/user' : '/session', 'POST', credentials);
    storage.set('chess-session', session);
    $('password').value = '';
    show('lobby'); await refreshGames();
  }, $('auth-submit'));
};
$('logout').onclick = () => action(async () => { await request('/session', 'DELETE'); resetSession(); }, $('logout'));
function node(tag, className, text) {
  const element = document.createElement(tag);
  if (className) element.className = className;
  if (text != null) element.textContent = text;
  return element;
}
async function refreshGames() {
  const current = ++lobbyRequest;
  const [result, profile] = await Promise.all([request('/game'), request('/user')]);
  if (!session || gameID || current !== lobbyRequest) return;
  $('username-label').textContent = profile.username;
  $('player-rating').textContent = `Your rating: ${profile.elo} Elo`;
  const cards = (result.games || []).map((game) => {
    const card = node('article', 'card game-card');
    card.append(node('p', 'eyebrow', `GAME ${game.gameID}`), node('h2', '', game.gameName));
    if (game.stockfish) card.append(node('p', 'muted', game.stockfish.mode === 'ELO' ? `Stockfish · ${game.stockfish.elo} Elo` : `Stockfish · level ${game.stockfish.skillLevel} · unrated`));
    const seats = node('div', 'seats');
    for (const color of ['WHITE', 'BLACK']) {
      const seat = node('div', 'seat');
      seat.append(node('span', '', `${color === 'WHITE' ? '○' : '●'} ${titleCase(color)}`), node('strong', '', game[`${color.toLowerCase()}Username`] || 'Open seat'));
      seats.append(seat);
    }
    const buttons = node('div', 'game-actions');
    const ownSeat = game.whiteUsername === session.username ? 'WHITE' : game.blackUsername === session.username ? 'BLACK' : null;
    const add = (text, color, disabled = false) => {
      const button = node('button', color ? 'primary' : 'secondary', text);
      button.disabled = disabled;
      button.onclick = () => action(() => joinGame(game, color, ownSeat === color), button);
      buttons.append(button);
    };
    if (ownSeat) add(`Resume as ${titleCase(ownSeat)}`, ownSeat);
    else {
      add('Play White', 'WHITE', !!game.whiteUsername);
      add('Play Black', 'BLACK', !!game.blackUsername);
      add('Watch', null);
    }
    if (!game.stockfish && (!game.whiteUsername || !game.blackUsername)) {
      const addAI = node('button', 'secondary', 'Add Stockfish');
      addAI.onclick = () => openAddAI(game);
      buttons.append(addAI);
    }
    card.append(seats, buttons);
    return card;
  });
  if (!cards.length) cards.push(node('div', 'card empty', 'No games yet. Create a game and invite a friend to take the other seat.'));
  $('games').replaceChildren(...cards);
}
$('refresh').onclick = () => action(refreshGames, $('refresh'));
$('create-form').onsubmit = (event) => {
  event.preventDefault();
  const button = event.submitter;
  action(async () => {
    const name = $('game-name').value.trim();
    if (!name) throw new Error('Please enter a game name.');
    const stockfish = $('opponent').value === 'stockfish' ? aiOptions('ai') : null;
    const result = await request('/game', 'POST', { gameName: name, ...(stockfish ? { stockfish } : {}) });
    $('game-name').value = '';
    if (stockfish) await joinGame({ gameID: Number(result.gameID), gameName: name }, $('ai-color').value, true);
    else await refreshGames();
  }, button);
};
$('opponent').onchange = () => {
  $('ai-options').hidden = $('opponent').value !== 'stockfish';
  updateAIControls('ai');
};
function updateAIControls(prefix) {
  const manual = $(`${prefix}-mode`).value === 'SKILL';
  $(`${prefix}-level-label`).hidden = !manual;
  $(`${prefix}-level`).disabled = !manual || (prefix === 'ai' && $('opponent').value !== 'stockfish');
}
for (const prefix of ['ai', 'add-ai']) $(`${prefix}-mode`).onchange = () => updateAIControls(prefix);
function aiOptions(prefix) {
  const mode = $(`${prefix}-mode`).value;
  const options = { color: $(`${prefix}-color`).value === 'WHITE' ? 'BLACK' : 'WHITE', mode };
  if (mode === 'SKILL') {
    const level = Number($(`${prefix}-level`).value);
    if (!Number.isInteger(level) || level < 0 || level > 20) throw new Error('Choose a whole skill level from 0 to 20.');
    options.skillLevel = level;
  }
  return options;
}
let addAIGame = null;
function openAddAI(game) {
  addAIGame = game;
  $('add-ai-alert').hidden = true;
  const own = game.whiteUsername === session.username ? 'WHITE' : game.blackUsername === session.username ? 'BLACK' : null;
  $('add-ai-color').value = own || (!game.whiteUsername ? 'WHITE' : 'BLACK');
  $('add-ai-dialog').showModal();
}
$('add-ai-cancel').onclick = () => $('add-ai-dialog').close();
$('add-ai-form').onsubmit = (event) => {
  event.preventDefault();
  action(async () => {
    const game = addAIGame;
    const color = $('add-ai-color').value;
    const stockfish = aiOptions('add-ai');
    const seat = game[`${color.toLowerCase()}Username`];
    if (seat && seat !== session.username) throw new Error('Choose your own seat or an empty seat.');
    if (!seat) await request('/game', 'PUT', { gameID: game.gameID, playerColor: color });
    await request('/game/stockfish', 'PUT', { gameID: game.gameID, stockfish });
    $('add-ai-dialog').close();
    await joinGame(game, color, true);
  }, event.submitter);
};
async function joinGame(game, color, resume) {
  if (color && !resume) await request('/game', 'PUT', { gameID: game.gameID, playerColor: color });
  gameID = game.gameID; storage.set('chess-match', gameID);
  snapshot = null; selected = null; role = color; flipped = color === 'BLACK';
  $('match-name').textContent = game.gameName;
  $('match-id').textContent = `GAME ${gameID}`;
  $('activity').replaceChildren();
  $('board').replaceChildren();
  show('match'); connectGame();
}
function log(message) {
  const item = node('li', '', message);
  const time = node('time', '', new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }));
  item.append(time); $('activity').prepend(item);
  while ($('activity').children.length > 40) $('activity').lastChild.remove();
}
async function refreshSeats() {
  const currentGame = gameID;
  try {
    const result = await request('/game');
    const entry = result.games.find((game) => game.gameID === currentGame);
    if (entry && gameID === currentGame && snapshot) {
      snapshot.game.whiteUsername = entry.whiteUsername;
      snapshot.game.blackUsername = entry.blackUsername;
      role = entry.whiteUsername === session.username ? 'WHITE' : entry.blackUsername === session.username ? 'BLACK' : null;
      $('match-id').textContent = `GAME ${gameID} · ${role ? titleCase(role) : 'Observer'}`;
      renderBoard(); updateStatus();
    }
  } catch (error) { alertMessage(error.message); }
}
function isConnected() { return socket?.readyState === WebSocket.OPEN && ready && !!snapshot; }
function ended() { return !!(snapshot?.game.game.gameOver || snapshot?.checkmate || snapshot?.stalemate); }
function canMove() { return isConnected() && !pending && !ended() && role && role === snapshot.game.game.teamTurn; }
function clearSelection() {
  selected = null; promotionMoves = [];
  if ($('promotion').open) $('promotion').close();
  renderBoard(); updateStatus();
}
function updateStatus() {
  const live = isConnected();
  $('connection').textContent = live ? '● Connected' : 'Reconnecting…';
  $('connection').classList.toggle('live', live);
  $('reconnect').hidden = live;
  $('resign').hidden = !role;
  $('resign').disabled = !live || pending || ended();
  $('cancel-selection').hidden = !selected;
  let status = 'Loading game…';
  if (snapshot) {
    const turn = titleCase(snapshot.game.game.teamTurn);
    if (snapshot.checkmate) status = `${turn === 'White' ? 'Black' : 'White'} wins · Checkmate`;
    else if (snapshot.stalemate) status = 'Draw · Stalemate';
    else if (ended()) status = 'Game over';
    else if (pending) status = 'Waiting for server…';
    else status = `${canMove() ? 'Your turn' : `${turn} to move`}${snapshot.inCheck ? ' · Check' : ''}`;
  }
  $('turn').textContent = status;
  $('move-hint').textContent = !live ? 'Moves pause while the connection recovers.' : ended() ? 'The final position is yours to explore.' : selected ? `Selected ${squareName(selected)}. ${canMove() ? 'Tap a marked square to move.' : 'Legal moves are marked for inspection.'}` : role ? `You are playing ${titleCase(role)}. Tap a piece to see its legal moves.` : 'You are watching. Tap any piece to inspect its legal moves.';
}
function send(commandType, extra = {}) {
  if (socket?.readyState !== WebSocket.OPEN) throw new Error('Connection lost. Reconnect before continuing.');
  socket.send(JSON.stringify({ commandType, authToken: session.authToken, gameID, ...extra }));
}
function connectGame() {
  if (!session || !gameID) return;
  stopSocket(); selected = null; updateStatus(); renderBoard();
  const ws = new WebSocket(`${location.protocol === 'https:' ? 'wss:' : 'ws:'}//${location.host}/ws`);
  socket = ws;
  ws.onopen = () => { if (socket === ws) send('CONNECT'); };
  ws.onmessage = (event) => {
    if (socket !== ws) return;
    try {
      const message = JSON.parse(event.data);
      if (message.serverMessageType === 'LOAD_GAME') {
        if (message.game.gameID !== gameID) return;
        const firstLoad = !snapshot;
        snapshot = message; ready = true; revision++; retryCount = 0;
        pending = false; clearTimeout(pendingTimer);
        role = snapshot.game.whiteUsername === session.username ? 'WHITE' : snapshot.game.blackUsername === session.username ? 'BLACK' : null;
        if (firstLoad) flipped = role === 'BLACK';
        $('match-name').textContent = snapshot.game.gameName;
        $('match-id').textContent = `GAME ${gameID} · ${role ? titleCase(role) : 'Observer'}`;
        alertMessage(); clearSelection();
        if (firstLoad) log('Connected to the match.');
      } else if (message.serverMessageType === 'ERROR') {
        pending = false; clearTimeout(pendingTimer); clearSelection();
        if (message.errorMessage === 'Invalid authToken') resetSession();
        alertMessage(message.errorMessage);
      } else if (message.serverMessageType === 'NOTIFICATION') {
        log(message.message);
        if (/ has joined as (WHITE|BLACK)$| left the game\.$/.test(message.message)) refreshSeats();
        // The existing server announces resignations without sending a board snapshot.
        if (message.message.endsWith(' has resigned') && snapshot) {
          snapshot.game.game.gameOver = true; pending = false; clearTimeout(pendingTimer); clearSelection();
        }
      }
    } catch { alertMessage('Could not read the server update. Reconnect to reload the board.'); }
    updateStatus();
  };
  ws.onerror = () => { if (socket === ws) updateStatus(); };
  ws.onclose = () => {
    if (socket !== ws) return;
    socket = null; ready = false; pending = false; clearTimeout(pendingTimer); clearSelection();
    retryTimer = setTimeout(connectGame, Math.min(1000 * 2 ** retryCount++, 15000));
  };
}
$('reconnect').onclick = connectGame;
window.addEventListener('online', () => { if (gameID && !isConnected()) connectGame(); });
document.addEventListener('visibilitychange', () => {
  if (!document.hidden && gameID && socket?.readyState !== WebSocket.OPEN) connectGame();
});
function renderBoard() {
  if (!snapshot) return;
  const focus = document.activeElement?.dataset.square;
  const pieces = new Map(snapshot.game.game.gameBoard.board.map(({ position, piece }) => [squareName(position), piece]));
  const moves = selected ? snapshot.legalMoves.filter((move) => sameSquare(move.startPosition, selected)) : [];
  const squares = [];
  for (let y = 0; y < 8; y++) for (let x = 0; x < 8; x++) {
    const pos = { row: flipped ? y + 1 : 8 - y, col: flipped ? 8 - x : x + 1 };
    const name = squareName(pos), piece = pieces.get(name);
    const legal = moves.some((move) => sameSquare(move.endPosition, pos));
    const square = node('button', `square${(pos.row + pos.col) % 2 === 0 ? ' dark' : ''}${piece ? ' occupied' : ''}${sameSquare(pos, selected) ? ' selected' : ''}${legal ? ' legal' : ''}`);
    square.dataset.square = name;
    square.setAttribute('aria-label', `${name}${piece ? `, ${titleCase(piece.pieceColor)} ${titleCase(piece.type)}` : ', empty'}${legal ? ', legal destination' : ''}`);
    square.setAttribute('aria-pressed', String(sameSquare(pos, selected)));
    if (piece) square.append(node('span', `piece ${piece.pieceColor.toLowerCase()}`, glyphs[piece.type]));
    if (x === 0) square.append(node('span', 'coordinate rank', pos.row));
    if (y === 7) square.append(node('span', 'coordinate file', String.fromCharCode(96 + pos.col)));
    square.onclick = () => chooseSquare(pos, piece);
    squares.push(square);
  }
  $('board').replaceChildren(...squares);
  if (focus) $('board').querySelector(`[data-square="${focus}"]`)?.focus({ preventScroll: true });
  for (const [prefix, color] of [['opponent', flipped ? 'WHITE' : 'BLACK'], ['player', flipped ? 'BLACK' : 'WHITE']]) {
    $(prefix + '-name').textContent = snapshot.game[`${color.toLowerCase()}Username`] || 'Waiting for a player';
    $(prefix + '-color').textContent = `${titleCase(color)}${color === role ? ' · You' : ''}`;
    $(prefix + '-name').closest('.player-strip').querySelector('.avatar').classList.toggle('light-avatar', color === 'WHITE');
  }
}
function chooseSquare(position, piece) {
  if (!snapshot || !isConnected() || pending) return;
  if (sameSquare(position, selected)) { clearSelection(); return; }
  if (selected && canMove()) {
    const moves = snapshot.legalMoves.filter((move) => sameSquare(move.startPosition, selected) && sameSquare(move.endPosition, position));
    if (moves.length) {
      if (moves.some((move) => move.promotionPiece)) {
        promotionMoves = moves; $('promotion').showModal();
      } else submitMove(moves[0]);
      return;
    }
  }
  selected = piece && (!role || piece.pieceColor === role || ended()) ? position : null;
  renderBoard(); updateStatus();
}
function markPending() {
  pending = true; updateStatus();
  pendingTimer = setTimeout(() => {
    pending = false; alertMessage('The server has not confirmed the action. Reloading the latest position…'); connectGame();
  }, 8000);
}
function submitMove(move) {
  action(async () => {
    if (!canMove()) throw new Error('Please wait for your turn and a live connection.');
    send('MAKE_MOVE', { move });
    log(`Move submitted: ${squareName(move.startPosition)} → ${squareName(move.endPosition)}${move.promotionPiece ? ` = ${titleCase(move.promotionPiece)}` : ''}.`);
    markPending(); clearSelection();
  });
}
for (const button of document.querySelectorAll('[data-piece]')) button.onclick = () => {
  const move = promotionMoves.find((candidate) => candidate.promotionPiece === button.dataset.piece);
  if (move) submitMove(move);
};
$('cancel-promotion').onclick = clearSelection;
$('promotion').addEventListener('cancel', clearSelection);
$('cancel-selection').onclick = clearSelection;
$('flip').onclick = () => { flipped = !flipped; renderBoard(); };
document.addEventListener('keydown', (event) => {
  if (event.key === 'Escape' && !$('promotion').open && !$('confirm-dialog').open) clearSelection();
});
function confirmAction(title, text) {
  $('confirm-title').textContent = title; $('confirm-text').textContent = text;
  return new Promise((resolve) => {
    const dialog = $('confirm-dialog');
    const finish = (accepted) => { dialog.close(); resolve(accepted); };
    $('confirm-cancel').onclick = () => finish(false);
    $('confirm-ok').onclick = () => finish(true);
    dialog.oncancel = () => resolve(false);
    dialog.showModal();
  });
}
$('resign').onclick = () => action(async () => {
  const currentRevision = revision;
  if (await confirmAction('Resign this game?', 'Resigning ends the match for both players.')) {
    if (!role || ended() || !isConnected() || revision !== currentRevision) throw new Error('The game changed. Please review the board and try again.');
    send('RESIGN'); markPending();
  }
});
$('leave').onclick = () => action(async () => {
  if (role && !ended() && !snapshot?.game.stockfish && !(await confirmAction('Leave your seat?', isConnected() ? 'Your seat will be open for another player. The game will continue.' : 'You are disconnected. Your seat stays reserved; you can resume it from the lobby.'))) return;
  if (socket?.readyState === WebSocket.OPEN) send('LEAVE');
  stopSocket(); gameID = null; snapshot = null; role = null;
  storage.set('chess-match', null); clearSelection(); show('lobby'); await refreshGames();
});
if (session?.authToken && session?.username) {
  if (gameID) { show('match'); connectGame(); } else { show('lobby'); action(refreshGames); }
} else { resetSession(); }
