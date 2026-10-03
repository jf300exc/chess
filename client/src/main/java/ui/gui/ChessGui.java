package ui.gui;

import adapters.CastleRequirementsAdapter;
import adapters.ChessBoardAdapter;
import adapters.ChessGameAdapter;
import adapters.ChessPieceAdapter;
import adapters.ChessPositionAdapter;
import chess.ChessBoard;
import chess.ChessGame;
import chess.ChessMove;
import chess.ChessPiece;
import chess.ChessPosition;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import model.GameData;
import model.GameEntry;
import requests.CreateGameRequest;
import requests.JoinGameRequest;
import requests.ListGamesRequest;
import requests.LoginRequest;
import requests.LogoutRequest;
import requests.RegisterRequest;
import ui.ServerFacade;
import ui.WebSocketClient;
import ui.WebSocketListener;
import websocket.commands.MakeMoveCommand;
import websocket.commands.UserGameCommand;
import websocket.messages.ErrorMessage;
import websocket.messages.LoadGameMessage;
import websocket.messages.NotificationMessage;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicPasswordFieldUI;
import javax.swing.plaf.basic.BasicTextFieldUI;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/** Full desktop chess client backed by the existing server protocol. */
public class ChessGui extends JFrame implements WebSocketListener {
    private static final Color BACKGROUND = new Color(19, 25, 31);
    private static final Color SURFACE = new Color(30, 39, 47);
    private static final Color SURFACE_2 = new Color(40, 51, 60);
    private static final Color TEXT = new Color(238, 241, 239);
    private static final Color MUTED = new Color(162, 174, 181);
    private static final Color ACCENT = new Color(233, 171, 74);
    private static final Color SUCCESS = new Color(93, 185, 131);
    private static final Color DANGER = new Color(207, 87, 87);

    private static final String AUTH_CARD = "auth";
    private static final String LOBBY_CARD = "lobby";
    private static final String GAME_CARD = "game";

    private final String host;
    private final int port;
    private final ServerFacade facade;
    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "chess-network");
        thread.setDaemon(true);
        return thread;
    });
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(ChessGame.class, new ChessGameAdapter())
            .registerTypeAdapter(ChessBoard.class, new ChessBoardAdapter())
            .registerTypeAdapter(ChessPiece.class, new ChessPieceAdapter())
            .registerTypeAdapter(ChessPosition.class, new ChessPositionAdapter())
            .registerTypeAdapter(
                    new TypeToken<Map<ChessGame.TeamColor, Map<ChessBoard.CastlePieceTypes,
                            Map<ChessBoard.CastleType, Boolean>>>>() { }.getType(),
                    new CastleRequirementsAdapter())
            .create();

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);
    private final JLabel connectionLabel = label("Ready", MUTED, 12, Font.PLAIN);
    private final DefaultListModel<GameEntry> gamesModel = new DefaultListModel<>();
    private final JList<GameEntry> gamesList = new JList<>(gamesModel);
    private final JLabel detailName = label("Select a game", TEXT, 26, Font.BOLD);
    private final JLabel detailPlayers = label("", MUTED, 14, Font.PLAIN);
    private final JButton joinWhiteButton = button("Join as White", true);
    private final JButton joinBlackButton = button("Join as Black", true);
    private final JButton observeButton = button("Observe", false);
    private final JTextField createGameField = textField();

    private final ChessBoardPanel boardPanel = new ChessBoardPanel();
    private final JLabel gameTitleLabel = label("Connecting…", TEXT, 25, Font.BOLD);
    private final JLabel roleLabel = label("", MUTED, 13, Font.BOLD);
    private final JLabel turnLabel = label("Waiting for position", ACCENT, 18, Font.BOLD);
    private final DefaultListModel<String> activityModel = new DefaultListModel<>();
    private final JList<String> activityList = new JList<>(activityModel);
    private final JScrollPane activityScroll = scroll(activityList);

    private volatile WebSocketClient webSocket;
    private volatile long navigationVersion;
    private GameEntry selectedGame;
    private GameData currentGame;
    private ChessGame.TeamColor playerColor;

    public ChessGui(String host, int port) {
        super("Chess — Multiplayer");
        this.host = host;
        this.port = port;
        this.facade = new ServerFacade(host, port);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(960, 700));
        setSize(1240, 820);
        setLocationByPlatform(true);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BACKGROUND);
        root.add(buildHeader(), BorderLayout.NORTH);
        cards.setOpaque(false);
        cards.add(buildAuthPanel(), AUTH_CARD);
        cards.add(buildLobbyPanel(), LOBBY_CARD);
        cards.add(buildGamePanel(), GAME_CARD);
        root.add(cards, BorderLayout.CENTER);
        setContentPane(root);

        boardPanel.setMoveHandler(this::makeMove);
        gamesList.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                showGameDetails(gamesList.getSelectedValue());
            }
        });
        joinWhiteButton.addActionListener(event -> joinSelected(ChessGame.TeamColor.WHITE));
        joinBlackButton.addActionListener(event -> joinSelected(ChessGame.TeamColor.BLACK));
        observeButton.addActionListener(event -> observeSelected());
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent event) {
                closeResources();
            }
        });
    }

    public static void installLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // The custom palette still gives the application a consistent appearance.
        }
        UIManager.put("ToolTip.background", SURFACE_2);
        UIManager.put("ToolTip.foreground", TEXT);
        UIManager.put("ToolTip.border", BorderFactory.createLineBorder(new Color(74, 90, 99)));
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(18, 0));
        header.setBackground(SURFACE);
        header.setBorder(new EmptyBorder(14, 24, 14, 24));
        JLabel brand = label("♞  CHESS", TEXT, 21, Font.BOLD);
        JLabel endpoint = label("Server  " + host + ":" + port, MUTED, 12, Font.PLAIN);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);
        right.add(connectionLabel);
        right.add(endpoint);
        header.add(brand, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JPanel buildAuthPanel() {
        JPanel page = page();
        JPanel content = new JPanel(new GridLayout(1, 2, 24, 0));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(54, 80, 68, 80));

        JTextField loginName = textField();
        JPasswordField loginPassword = passwordField();
        JButton login = button("Sign in", true);
        login.addActionListener(event -> login(loginName.getText(), new String(loginPassword.getPassword())));
        loginPassword.addActionListener(event -> login.doClick());
        content.add(authCard("Welcome back", "Continue a game or start a new match.",
                new Field("Username", loginName), new Field("Password", loginPassword), login));

        JTextField registerName = textField();
        JPasswordField registerPassword = passwordField();
        JTextField registerEmail = textField();
        JButton register = button("Create account", true);
        register.addActionListener(event -> register(registerName.getText(),
                new String(registerPassword.getPassword()), registerEmail.getText()));
        content.add(authCard("New player", "Create an account on this chess server.",
                new Field("Username", registerName), new Field("Password", registerPassword),
                new Field("Email", registerEmail), register));
        page.add(content, BorderLayout.CENTER);
        return page;
    }

    private JPanel authCard(String title, String subtitle, Object... rows) {
        JPanel panel = card();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(38, 42, 38, 42));
        JLabel titleLabel = label(title, TEXT, 29, Font.BOLD);
        JLabel subtitleLabel = label(subtitle, MUTED, 14, Font.PLAIN);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(7));
        panel.add(subtitleLabel);
        panel.add(Box.createVerticalStrut(30));
        for (Object row : rows) {
            if (row instanceof Field field) {
                JLabel fieldLabel = label(field.name, MUTED, 12, Font.BOLD);
                fieldLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                field.component.setAlignmentX(Component.LEFT_ALIGNMENT);
                field.component.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
                panel.add(fieldLabel);
                panel.add(Box.createVerticalStrut(7));
                panel.add(field.component);
                panel.add(Box.createVerticalStrut(18));
            } else if (row instanceof JButton action) {
                action.setAlignmentX(Component.LEFT_ALIGNMENT);
                action.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
                panel.add(Box.createVerticalStrut(4));
                panel.add(action);
            }
        }
        return panel;
    }

    private JPanel buildLobbyPanel() {
        JPanel page = page();
        JPanel toolbar = new JPanel(new BorderLayout(18, 0));
        toolbar.setOpaque(false);
        toolbar.setBorder(new EmptyBorder(28, 30, 18, 30));
        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        heading.add(label("Game lobby", TEXT, 30, Font.BOLD));
        heading.add(label("Choose a seat or watch a live match.", MUTED, 14, Font.PLAIN));
        toolbar.add(heading, BorderLayout.WEST);

        JPanel tools = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        tools.setOpaque(false);
        createGameField.setPreferredSize(new Dimension(210, 40));
        createGameField.putClientProperty("JTextField.placeholderText", "Game name");
        JButton create = button("New game", true);
        create.addActionListener(event -> createGame());
        createGameField.addActionListener(event -> create.doClick());
        JButton refresh = button("Refresh", false);
        refresh.addActionListener(event -> refreshGames());
        JButton logout = button("Log out", false);
        logout.addActionListener(event -> logout());
        tools.add(createGameField);
        tools.add(create);
        tools.add(refresh);
        tools.add(logout);
        toolbar.add(tools, BorderLayout.EAST);
        page.add(toolbar, BorderLayout.NORTH);

        gamesList.setBackground(SURFACE);
        gamesList.setForeground(TEXT);
        gamesList.setSelectionBackground(new Color(70, 94, 84));
        gamesList.setSelectionForeground(TEXT);
        gamesList.setFixedCellHeight(74);
        gamesList.setCellRenderer(new GameCellRenderer());
        JScrollPane gamesScroll = scroll(gamesList);
        gamesScroll.setBorder(BorderFactory.createLineBorder(new Color(57, 70, 79)));

        JPanel details = card();
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        details.setBorder(new EmptyBorder(38, 38, 38, 38));
        detailName.setAlignmentX(Component.LEFT_ALIGNMENT);
        detailPlayers.setAlignmentX(Component.LEFT_ALIGNMENT);
        details.add(label("MATCH DETAILS", ACCENT, 12, Font.BOLD));
        details.add(Box.createVerticalStrut(15));
        details.add(detailName);
        details.add(Box.createVerticalStrut(12));
        details.add(detailPlayers);
        details.add(Box.createVerticalGlue());
        joinWhiteButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        joinBlackButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        observeButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (JButton action : new JButton[]{joinWhiteButton, joinBlackButton, observeButton}) {
            action.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            details.add(action);
            details.add(Box.createVerticalStrut(10));
        }

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, gamesScroll, details);
        split.setOpaque(false);
        split.setBorder(new EmptyBorder(0, 30, 30, 30));
        split.setDividerLocation(650);
        split.setResizeWeight(.68);
        page.add(split, BorderLayout.CENTER);
        showGameDetails(null);
        return page;
    }

    private JPanel buildGamePanel() {
        JPanel page = page();
        page.setLayout(new BorderLayout(0, 0));
        page.add(boardPanel, BorderLayout.CENTER);

        JPanel side = new JPanel();
        side.setBackground(SURFACE);
        side.setPreferredSize(new Dimension(320, 600));
        side.setBorder(new EmptyBorder(28, 26, 24, 26));
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        gameTitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        roleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        turnLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        side.add(gameTitleLabel);
        side.add(Box.createVerticalStrut(5));
        side.add(roleLabel);
        side.add(Box.createVerticalStrut(22));
        side.add(turnLabel);
        side.add(Box.createVerticalStrut(25));
        JLabel activityTitle = label("ACTIVITY", MUTED, 12, Font.BOLD);
        activityTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        side.add(activityTitle);
        side.add(Box.createVerticalStrut(8));
        activityList.setBackground(SURFACE_2);
        activityList.setForeground(TEXT);
        activityList.setFixedCellHeight(31);
        activityList.setBorder(new EmptyBorder(5, 8, 5, 8));
        activityScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        activityScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        side.add(activityScroll);
        side.add(Box.createVerticalStrut(16));

        JButton flip = button("Flip board", false);
        flip.addActionListener(event -> boardPanel.flip());
        JButton resign = button("Resign", false);
        resign.setForeground(DANGER.brighter());
        resign.addActionListener(event -> resign());
        JButton leave = button("Leave game", true);
        leave.addActionListener(event -> leaveGame());
        for (JButton action : new JButton[]{flip, resign, leave}) {
            action.setAlignmentX(Component.LEFT_ALIGNMENT);
            action.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
            side.add(action);
            side.add(Box.createVerticalStrut(9));
        }
        page.add(side, BorderLayout.EAST);
        return page;
    }

    private void login(String name, String password) {
        if (name.isBlank() || password.isBlank()) {
            showError("Enter both a username and password.");
            return;
        }
        setConnection("Signing in…", ACCENT);
        task(() -> facade.loginClient(new LoginRequest(name.trim(), password)), result -> {
            if (result == null) {
                showError(error("Sign in failed."));
                return;
            }
            completeAuth(result.username(), result.authToken());
        });
    }

    private void register(String name, String password, String email) {
        if (name.isBlank() || password.isBlank() || email.isBlank()) {
            showError("Username, password, and email are required.");
            return;
        }
        setConnection("Creating account…", ACCENT);
        task(() -> facade.registerClient(new RegisterRequest(name.trim(), password, email.trim())), result -> {
            if (result == null) {
                showError(error("Registration failed."));
                return;
            }
            completeAuth(result.username(), result.authToken());
        });
    }

    private void completeAuth(String name, String token) {
        facade.setAuthToken(token);
        setConnection("Connected as " + name, SUCCESS);
        cardLayout.show(cards, LOBBY_CARD);
        refreshGames();
    }

    private void logout() {
        String token = facade.getAuthToken();
        setConnection("Signing out…", ACCENT);
        task(() -> facade.logoutClient(new LogoutRequest(token)), result -> {
            if (result == null) {
                showError(error("Could not sign out. Try again."));
                return;
            }
            navigationVersion++;
            facade.setAuthToken(null);
            gamesModel.clear();
            setConnection("Signed out", MUTED);
            cardLayout.show(cards, AUTH_CARD);
        });
    }

    private void createGame() {
        String name = createGameField.getText().trim();
        if (name.isBlank()) {
            showError("Give the new game a name.");
            return;
        }
        setConnection("Creating game…", ACCENT);
        task(() -> facade.createGameClient(new CreateGameRequest(facade.getAuthToken(), name)), result -> {
            if (result == null) {
                showError(error("Could not create the game."));
                return;
            }
            createGameField.setText("");
            setConnection("Game created", SUCCESS);
            refreshGames();
        });
    }

    private void refreshGames() {
        setConnection("Refreshing lobby…", ACCENT);
        task(() -> facade.listGamesClient(new ListGamesRequest(facade.getAuthToken())), result -> {
            if (result == null || result.games() == null) {
                showError(error("Could not load the game list."));
                return;
            }
            ArrayList<GameEntry> games = new ArrayList<>(result.games());
            games.sort(Comparator.comparing(GameEntry::gameName, String.CASE_INSENSITIVE_ORDER));
            gamesModel.clear();
            games.forEach(gamesModel::addElement);
            if (!gamesModel.isEmpty()) {
                gamesList.setSelectedIndex(0);
            } else {
                showGameDetails(null);
            }
            setConnection(games.size() + (games.size() == 1 ? " game" : " games") + " available", SUCCESS);
        });
    }

    private void showGameDetails(GameEntry game) {
        selectedGame = game;
        boolean selected = game != null;
        if (!selected) {
            detailName.setText("No game selected");
            detailPlayers.setText("Create a game or select one from the list.");
        } else {
            detailName.setText(game.gameName());
            detailPlayers.setText("<html>White&nbsp;&nbsp;<b>" + seat(game.whiteUsername())
                    + "</b><br><br>Black&nbsp;&nbsp;<b>" + seat(game.blackUsername()) + "</b></html>");
        }
        joinWhiteButton.setEnabled(selected && game.whiteUsername() == null);
        joinBlackButton.setEnabled(selected && game.blackUsername() == null);
        observeButton.setEnabled(selected);
    }

    private void joinSelected(ChessGame.TeamColor color) {
        GameEntry game = selectedGame;
        if (game == null) {
            return;
        }
        setConnection("Claiming " + color.name().toLowerCase() + " seat…", ACCENT);
        JoinGameRequest request = new JoinGameRequest(facade.getAuthToken(), color.name(),
                Integer.toString(game.gameID()));
        task(() -> facade.joinGameClient(request), result -> {
            if (result == null) {
                showError(error("Could not join that seat."));
                refreshGames();
                return;
            }
            enterGame(game, color);
        });
    }

    private void observeSelected() {
        if (selectedGame != null) {
            enterGame(selectedGame, null);
        }
    }

    private void enterGame(GameEntry game, ChessGame.TeamColor color) {
        long version = ++navigationVersion;
        String token = facade.getAuthToken();
        currentGame = null;
        playerColor = color;
        gameTitleLabel.setText(game.gameName());
        roleLabel.setText(color == null ? "OBSERVER" : "PLAYING " + color.name());
        turnLabel.setText("Connecting…");
        activityModel.clear();
        addActivity("Opening live game connection…");
        boardPanel.setOrientation(color == null ? ChessGame.TeamColor.WHITE : color);
        boardPanel.setPosition(null, color, false);
        cardLayout.show(cards, GAME_CARD);

        task(() -> {
            WebSocketClient client = new WebSocketClient(host, port, message -> handleMessage(message, version));
            try {
                client.connectClient();
                webSocket = client;
                client.sendCommand(new UserGameCommand(UserGameCommand.CommandType.CONNECT, token, game.gameID()));
                if (version != navigationVersion) {
                    client.sendCommand(new UserGameCommand(UserGameCommand.CommandType.LEAVE, token, game.gameID()));
                    client.closeClient();
                    return false;
                }
            } catch (Exception e) {
                client.closeClient();
                throw e;
            }
            return true;
        }, connected -> setConnection("Live connection", SUCCESS));
    }

    private void makeMove(ChessMove move) {
        if (currentGame == null || playerColor == null) {
            boardPanel.acknowledgeMove();
            return;
        }
        ChessPiece moving = currentGame.game().getBoard().getPiece(move.getStartPosition());
        ChessMove commandMove = move;
        if (moving != null && moving.getPieceType() == ChessPiece.PieceType.PAWN
                && (move.getEndPosition().getRow() == 1 || move.getEndPosition().getRow() == 8)) {
            ChessPiece.PieceType[] choices = {ChessPiece.PieceType.QUEEN, ChessPiece.PieceType.ROOK,
                    ChessPiece.PieceType.BISHOP, ChessPiece.PieceType.KNIGHT};
            Object choice = JOptionPane.showInputDialog(this, "Promote pawn to:", "Pawn promotion",
                    JOptionPane.QUESTION_MESSAGE, null, choices, choices[0]);
            if (!(choice instanceof ChessPiece.PieceType promotion)) {
                boardPanel.acknowledgeMove();
                return;
            }
            commandMove = new ChessMove(move.getStartPosition(), move.getEndPosition(), promotion);
        }

        WebSocketClient client = webSocket;
        int gameId = currentGame.gameID();
        String token = facade.getAuthToken();
        ChessMove finalMove = commandMove;
        task(() -> {
            if (client == null || !client.isSessionOpen()) {
                throw new IllegalStateException("The live game connection is closed.");
            }
            client.sendCommand(new MakeMoveCommand(UserGameCommand.CommandType.MAKE_MOVE,
                    token, gameId, finalMove));
            return true;
        }, sent -> addActivity("Move sent: " + notation(finalMove)));
    }

    private void resign() {
        if (playerColor == null || currentGame == null) {
            showError("Observers cannot resign a game.");
            return;
        }
        int answer = JOptionPane.showConfirmDialog(this, "Resign this game? This cannot be undone.",
                "Confirm resignation", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        sendSimpleCommand(UserGameCommand.CommandType.RESIGN);
    }

    private void leaveGame() {
        navigationVersion++;
        int gameId = currentGame != null ? currentGame.gameID() : selectedGame == null ? 0 : selectedGame.gameID();
        String token = facade.getAuthToken();
        task(() -> {
            WebSocketClient client = webSocket;
            webSocket = null;
            if (client != null) {
                try {
                    if (client.isSessionOpen() && gameId != 0) {
                        client.sendCommand(new UserGameCommand(UserGameCommand.CommandType.LEAVE,
                                token, gameId));
                    }
                } finally {
                    client.closeClient();
                }
            }
            return true;
        }, ignored -> {
            currentGame = null;
            playerColor = null;
            boardPanel.setPosition(null, null, false);
            cardLayout.show(cards, LOBBY_CARD);
            refreshGames();
        });
    }

    private void sendSimpleCommand(UserGameCommand.CommandType type) {
        WebSocketClient client = webSocket;
        if (client == null || currentGame == null) {
            showError("The live game connection is not available.");
            return;
        }
        int gameId = currentGame.gameID();
        String token = facade.getAuthToken();
        task(() -> {
            client.sendCommand(new UserGameCommand(type, token, gameId));
            return true;
        }, ignored -> addActivity(type == UserGameCommand.CommandType.RESIGN ? "Resignation sent" : "Command sent"));
    }

    @Override
    public void onMessage(String message) {
        handleMessage(message, navigationVersion);
    }

    private void handleMessage(String message, long version) {
        try {
            JsonObject json = JsonParser.parseString(message).getAsJsonObject();
            String type = json.get("serverMessageType").getAsString();
            switch (type) {
                case "LOAD_GAME" -> {
                    LoadGameMessage load = gson.fromJson(message, LoadGameMessage.class);
                    updateIfCurrent(version, () -> loadGame(load.getGame()));
                }
                case "ERROR" -> {
                    ErrorMessage error = gson.fromJson(message, ErrorMessage.class);
                    updateIfCurrent(version, () -> {
                        boardPanel.acknowledgeMove();
                        showError(error.getErrorMessage());
                        addActivity("Error: " + error.getErrorMessage());
                    });
                }
                case "NOTIFICATION" -> {
                    NotificationMessage notification = gson.fromJson(message, NotificationMessage.class);
                    updateIfCurrent(version, () -> {
                        addActivity(notification.getMessage());
                        if (currentGame != null && notification.getMessage().endsWith(" has resigned")) {
                            currentGame.game().setGameOver(true);
                            loadGame(currentGame);
                        }
                    });
                }
                default -> updateIfCurrent(version, () -> addActivity("Server: " + message));
            }
        } catch (Exception e) {
            updateIfCurrent(version, () -> addActivity("Unreadable server message"));
        }
    }

    private void loadGame(GameData game) {
        currentGame = game;
        gameTitleLabel.setText(game.gameName());
        String turn = game.game().isGameOver() ? "Game over" : game.game().getTeamTurn().name() + " to move";
        turnLabel.setText(turn);
        turnLabel.setForeground(game.game().isGameOver() ? DANGER : ACCENT);
        boardPanel.setPosition(game.game(), playerColor, playerColor != null && !game.game().isGameOver());
        addActivity(turn);
    }

    private <T> void task(Callable<T> action, Consumer<T> success) {
        long version = navigationVersion;
        networkExecutor.submit(() -> {
            try {
                T result = action.call();
                updateIfCurrent(version, () -> success.accept(result));
            } catch (Exception e) {
                updateIfCurrent(version, () -> {
                    boardPanel.acknowledgeMove();
                    showError(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
                });
            }
        });
    }

    private void updateIfCurrent(long version, Runnable update) {
        SwingUtilities.invokeLater(() -> {
            if (version == navigationVersion) {
                update.run();
            }
        });
    }

    private void showError(String message) {
        setConnection(message, DANGER);
        Toolkit.getDefaultToolkit().beep();
    }

    private void setConnection(String message, Color color) {
        connectionLabel.setText("●  " + message);
        connectionLabel.setForeground(color);
    }

    private void addActivity(String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        JScrollBar scrollbar = activityScroll.getVerticalScrollBar();
        boolean atBottom = scrollbar.getValue() + scrollbar.getVisibleAmount() >= scrollbar.getMaximum();
        Point previousPosition = activityScroll.getViewport().getViewPosition();
        activityModel.addElement(message);
        int removed = 0;
        while (activityModel.size() > 100) {
            activityModel.remove(0);
            removed++;
        }
        // Update the scroll range before following the newly appended message.
        activityList.revalidate();
        activityScroll.validate();
        if (atBottom) {
            scrollbar.setValue(scrollbar.getMaximum());
        } else if (removed > 0) {
            activityScroll.getViewport().setViewPosition(new Point(previousPosition.x,
                    Math.max(0, previousPosition.y - removed * activityList.getFixedCellHeight())));
        }
    }

    private String error(String fallback) {
        String detail = facade.getLastError();
        return detail == null || detail.isBlank() ? fallback : detail;
    }

    private void closeResources() {
        navigationVersion++;
        WebSocketClient client = webSocket;
        if (client != null) {
            client.closeClient();
        }
        networkExecutor.shutdownNow();
    }

    private static String seat(String username) {
        return username == null ? "Open seat" : escape(username);
    }

    private static String notation(ChessMove move) {
        return square(move.getStartPosition()) + square(move.getEndPosition())
                + (move.getPromotionPiece() == null ? "" : "=" + move.getPromotionPiece().name().charAt(0));
    }

    private static String square(ChessPosition position) {
        return "" + (char) ('a' + position.getColumn() - 1) + position.getRow();
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static JPanel page() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BACKGROUND);
        return panel;
    }

    private static JPanel card() {
        JPanel panel = new JPanel();
        panel.setBackground(SURFACE);
        panel.setBorder(BorderFactory.createLineBorder(new Color(57, 70, 79)));
        return panel;
    }

    private static JLabel label(String text, Color color, int size, int style) {
        JLabel label = new JLabel(text);
        label.setForeground(color);
        label.setFont(new Font(Font.SANS_SERIF, style, size));
        return label;
    }

    private static JTextField textField() {
        JTextField field = new JTextField();
        styleField(field);
        return field;
    }

    private static JPasswordField passwordField() {
        JPasswordField field = new JPasswordField();
        styleField(field);
        return field;
    }

    private static void styleField(JTextField field) {
        if (field instanceof JPasswordField passwordField) {
            passwordField.setUI(new BasicPasswordFieldUI());
        } else {
            field.setUI(new BasicTextFieldUI());
        }
        field.setOpaque(true);
        field.setBackground(SURFACE_2);
        field.setForeground(TEXT);
        field.setCaretColor(TEXT);
        field.setSelectionColor(new Color(86, 118, 103));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(67, 81, 90)), new EmptyBorder(8, 11, 8, 11)));
        field.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
    }

    private static JButton button(String text, boolean primary) {
        JButton button = new StyledButton(text);
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(new EmptyBorder(10, 16, 10, 16));
        button.setBackground(primary ? ACCENT : SURFACE_2);
        button.setForeground(primary ? new Color(28, 31, 31) : TEXT);
        return button;
    }

    private static JScrollPane scroll(Component component) {
        JScrollPane scroll = new JScrollPane(component);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(SURFACE);
        return scroll;
    }

    private record Field(String name, JTextField component) { }

    private static class StyledButton extends JButton {
        private StyledButton(String text) {
            super(text);
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setRolloverEnabled(true);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color color = getBackground();
                if (!isEnabled()) {
                    color = new Color(color.getRed(), color.getGreen(), color.getBlue(), 85);
                } else if (getModel().isPressed()) {
                    color = color.darker();
                } else if (getModel().isRollover()) {
                    color = color.brighter();
                }
                g.setColor(color);
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            } finally {
                g.dispose();
            }
            super.paintComponent(graphics);
        }
    }

    private static class GameCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean selected, boolean focused) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focused);
            if (value instanceof GameEntry game) {
                String seats = (game.whiteUsername() == null ? "White open" : "White: " + escape(game.whiteUsername()))
                        + "  ·  " + (game.blackUsername() == null ? "Black open" : "Black: " + escape(game.blackUsername()));
                label.setText("<html><b style='font-size:13px'>" + escape(game.gameName())
                        + "</b><br><span style='color:#a2aeb5'>" + seats + "</span></html>");
                label.setBorder(new EmptyBorder(9, 14, 9, 14));
            }
            return label;
        }
    }
}
