package Game.Client;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

public class Info implements Serializable {
    @Serial
    private static final long serialVersionUID = -2095916884810199536L; // Updated serialVersionUID

    // Time-related fields
    long gameStartTime;         // Milliseconds, set by server at game_start
    long moveStartTime;         // Milliseconds, set by server for the current turn
    long gameTimeLimitMillis;   // Total game time limit for the mode
    long moveTimeLimitMillis;   // Per-move time limit for the mode

    Process process;            // Current game process or client request

    // Server -> Client specific fields
    String[][] board;           // Current board state
    boolean isRedPlayer;        // Is this client playing as Red?
    boolean isMyTurn;           // Is it currently this client's turn to move?
    String opponentName;        // Opponent's username
    String account;             // This client's username (confirmed by server)
    boolean invalid_move;

    // Client -> Server specific fields
    int ox, oy, nx, ny;         // Move coordinates: (oy, ox) -> (ny, nx)
    String password;            // For login
    String chatMessage;         // Chat or other message content

    // --- Constructors ---

    // Server: For game_start, game_update, results (win/lose/draw/withdraw_result)
    public Info(Process process, String[][] board, boolean isRedPlayer, boolean isMyTurn, String chatMessage, String opponentName) {
        this.process = process;
        this.board = board;
        this.isRedPlayer = isRedPlayer;
        this.isMyTurn = isMyTurn;
        this.chatMessage = chatMessage;
        this.opponentName = opponentName;
        this.invalid_move = Objects.equals(chatMessage, "无效的走法，请重试。");
    }

    // Client: Sending a move
    public Info(Process process, int oy, int ox, int ny, int nx, String chatMessage) {
        this.process = process;
        this.oy = oy;
        this.ox = ox;
        this.ny = ny;
        this.nx = nx;
        this.chatMessage = chatMessage;
    }

    // Client: Login or Register. Server: Login/Register success/fail (passwordOrMessage is message for server responses)
    public Info(Process process, String account, String passwordOrMessage) {
        this.process = process;
        this.account = account;
        if (process == Process.login || process == Process.register) {
            this.password = passwordOrMessage;
        } else {
            this.chatMessage = passwordOrMessage;
        }
    }

    // General purpose for simple process signals with a message (e.g., mode selection, surrender, chat)
    public Info(Process process, String messageContent) {
        this.process = process;
        this.chatMessage = messageContent;
    }

    // Static factory for server messages (simplifies creation)
    public static Info createServerMessage(String message) {
        return new Info(Process.message, message);
    }
    public static Info createLoginSuccess(String account, String message) {
        Info info = new Info(Process.login_success, account, message);
        return info;
    }
    public static Info createLoginFail(String attemptedAccount, String message) {
        Info info = new Info(Process.login_fail, attemptedAccount, message);
        return info;
    }
    public static Info createRegisterSuccess(String account, String message) {
        Info info = new Info(Process.register_success, account, message);
        return info;
    }
    public static Info createRegisterFail(String account, String message) {
        Info info = new Info(Process.register_fail, account, message);
        return info;
    }


    // --- Getters ---
    public Process getProcess() { return process; }
    public String[][] getBoard() { return board; }
    public boolean isRedPlayer() { return isRedPlayer; }
    public boolean isMyTurn() { return isMyTurn; }
    public String getOpponentName() { return opponentName; }
    public String getAccount() { return account; }
    public int getOx() { return ox; }
    public int getOy() { return oy; }
    public int getNx() { return nx; }
    public int getNy() { return ny; }
    public String getPassword() { return password; }
    public String getChatMessage() { return chatMessage; }
    public long getGameStartTime() { return gameStartTime; }
    public long getMoveStartTime() { return moveStartTime; }
    public long getGameTimeLimitMillis() { return gameTimeLimitMillis; }
    public long getMoveTimeLimitMillis() { return moveTimeLimitMillis; }
    public boolean isInvalidMove() { return invalid_move; }

    // --- Setters (Mainly for server to assemble Info objects) ---
    public void setProcess(Process process) { this.process = process; }
    public void setBoard(String[][] board) { this.board = board; }
    public void setRedPlayer(boolean redPlayer) { isRedPlayer = redPlayer; }
    public void setMyTurn(boolean myTurn) { isMyTurn = myTurn; }
    public void setChatMessage(String chatMessage) { this.chatMessage = chatMessage; }
    public void setOpponentName(String opponentName) { this.opponentName = opponentName; }
    public void setAccount(String account) { this.account = account; }
    public void setGameStartTime(long gameStartTime) { this.gameStartTime = gameStartTime; }
    public void setMoveStartTime(long moveStartTime) { this.moveStartTime = moveStartTime; }
    public void setGameTimeLimitMillis(long gameTimeLimitMillis) { this.gameTimeLimitMillis = gameTimeLimitMillis; }
    public void setMoveTimeLimitMillis(long moveTimeLimitMillis) { this.moveTimeLimitMillis = moveTimeLimitMillis; }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder("Process: " + process);
        if (account != null) s.append(", Account: ").append(account);
        if (opponentName != null) s.append(", Opponent: ").append(opponentName);
        if (board != null) s.append(", Board: (present)");
        if (process == Process.gaming && (ox != 0 || oy != 0 || nx != 0 || ny != 0)) {
            s.append(String.format(", Move: (%d,%d) to (%d,%d)", oy, ox, ny, nx));
        }
        if (chatMessage != null) s.append(", Chat: \"").append(chatMessage).append("\"");
        if (moveTimeLimitMillis > 0 && (process == Process.game_start || process == Process.game_update || process == Process.withdraw_result)) {
            s.append(", MoveLimit: ").append(moveTimeLimitMillis/1000).append("s");
        }
        return s.toString();
    }
}