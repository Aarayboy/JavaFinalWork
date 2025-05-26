package Game.Server;

import Game.Client.Info;
import Game.Client.Process;
import Game.GameLogic;
import Game.GameBoard;

import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GameRoom implements Runnable {
    private ClientHandler playerRed;
    private ClientHandler playerBlack;
    private GameLogic gameLogic;
    private volatile boolean gameActive;
    private Process gameMode;

    private long gameStartTimeMillis;
    private volatile long currentMoveStartTimeMillis;
    private long currentGameTimeLimitMillis;
    private long currentMoveTimeLimitMillis;

    private Stack<String[][]> boardHistory;
    private Stack<Boolean> turnHistory;

    private ClientHandler pendingWithdrawRequester = null;
    private ClientHandler pendingEvenRequester = null;

    private Timer moveTimer;
    private TimerTask currentMoveTimerTask;

    private static final Logger LOGGER = Logger.getLogger(GameRoom.class.getName());
    private final Object gameLock = new Object();

    public GameRoom(ClientHandler p1, ClientHandler p2, Process mode) {
        if (Math.random() < 0.5) {
            this.playerRed = p1;
            this.playerBlack = p2;
        } else {
            this.playerRed = p2;
            this.playerBlack = p1;
        }
        this.gameLogic = new GameLogic();
        this.gameActive = false;
        this.gameMode = mode; // Store the selected game mode
        this.boardHistory = new Stack<>();
        this.turnHistory = new Stack<>();

        // Initialize time limits based on gameMode *before* initializing timer
        if (this.gameMode == Process.short_game) {
            this.currentGameTimeLimitMillis = 10 * 60 * 1000;
            this.currentMoveTimeLimitMillis = 30 * 1000;
        } else {
            this.currentGameTimeLimitMillis = 30 * 60 * 1000;
            this.currentMoveTimeLimitMillis = 120 * 1000;
        }

        if (this.currentMoveTimeLimitMillis > 0) { // Only create timer if there's a move limit
            this.moveTimer = new Timer("GameRoomMoveTimer-" + playerRed.getUsername() + "-" + playerBlack.getUsername(), true);
        } else {
            this.moveTimer = null;
        }

        this.playerRed.setGameRoom(this);
        this.playerBlack.setGameRoom(this);
        LOGGER.info("游戏房间已创建: " + playerRed.getUsername() + " (红) vs " + playerBlack.getUsername() + " (黑). 模式: " + mode);
    }

    public boolean isGameActive() {
        return gameActive;
    }

    @Override
    public void run() {
        startGame();
    }

    private String[][] deepCopyBoard(String[][] originalBoard) {
        if (originalBoard == null) return null;
        String[][] copy = new String[GameBoard.ROWS][GameBoard.COLS];
        for (int i = 0; i < GameBoard.ROWS; i++) {
            if (originalBoard[i] != null) {
                System.arraycopy(originalBoard[i], 0, copy[i], 0, GameBoard.COLS);
            } else {
                copy[i] = new String[GameBoard.COLS];
                for (int j = 0; j < GameBoard.COLS; j++) copy[i][j] = "---";
            }
        }
        return copy;
    }

    private void saveCurrentBoardState() {
        synchronized (gameLock) {
            String[][] boardStateFromLogic = gameLogic.getGameBoard().getBoardState();
            boardHistory.push(deepCopyBoard(boardStateFromLogic));
            turnHistory.push(gameLogic.isRedTurn());
            LOGGER.fine("Board state saved. History size: " + boardHistory.size() + ". Turn for this state: " + (gameLogic.isRedTurn() ? "Red" : "Black"));
            if (boardHistory.size() > 25) {
                boardHistory.remove(0);
                turnHistory.remove(0);
            }
        }
    }

    private boolean canUndo() {
        synchronized (gameLock) {
            return boardHistory.size() >= 2;
        }
    }

    private void performUndo() {
        synchronized (gameLock) {
            if (canUndo()) {
                boardHistory.pop();
                turnHistory.pop();
                String[][] boardToRestore = boardHistory.peek();
                boolean turnToRestore = turnHistory.peek();
                gameLogic.getGameBoard().setBoardState(deepCopyBoard(boardToRestore));
                gameLogic.setTurn(turnToRestore);
                LOGGER.info("悔棋成功，棋盘已回滚。轮到: " + (turnToRestore ? playerRed.getUsername() + "(红)" : playerBlack.getUsername() + "(黑)"));
            } else {
                LOGGER.warning("尝试悔棋但历史记录不足。 History size: " + boardHistory.size());
            }
        }
    }

    private void scheduleMoveTimeout() {
        synchronized (gameLock) {
            if (!gameActive || moveTimer == null) {
                return;
            }

            if (currentMoveTimerTask != null) {
                currentMoveTimerTask.cancel();
            }
            // currentMoveTimeLimitMillis is already set based on game mode
            if (currentMoveTimeLimitMillis <= 0) {
                LOGGER.info("步时限制为0或无效，不安排超时。");
                return;
            }

            currentMoveStartTimeMillis = System.currentTimeMillis();
            currentMoveTimerTask = new TimerTask() {
                @Override
                public void run() {
                    synchronized (gameLock) {
                        if (!gameActive) return;
                        LOGGER.info("玩家 " + (gameLogic.isRedTurn() ? playerRed.getUsername() : playerBlack.getUsername()) + " 步时超时 (TimerTask fired)!");
                        handleTimeout();
                    }
                }
            };
            try {
                moveTimer.schedule(currentMoveTimerTask, currentMoveTimeLimitMillis);
                LOGGER.fine("为 " + (gameLogic.isRedTurn() ? playerRed.getUsername() : playerBlack.getUsername()) + " 安排了 " + currentMoveTimeLimitMillis/1000 + "s 的步时超时。");
            } catch (IllegalStateException e) {
                LOGGER.log(Level.SEVERE, "无法安排超时任务，计时器可能已取消: " + e.getMessage(), e);
            }
        }
    }

    private void handleTimeout() {
        synchronized (gameLock) {
            if (!gameActive) return;
            // gameActive will be set to false in handleGameEnd
            handleGameEnd(Process.timeout_lose);
        }
    }

    private void cancelAllTimers() {
        synchronized(gameLock){
            if (currentMoveTimerTask != null) {
                currentMoveTimerTask.cancel();
                currentMoveTimerTask = null;
            }
            if (moveTimer != null) {
                moveTimer.cancel();
                moveTimer.purge();
                moveTimer = null;
                LOGGER.fine("All move timers cancelled and purged.");
            }
        }
    }


    private void startGame() {
        synchronized(gameLock){
            this.gameActive = true;
            this.gameStartTimeMillis = System.currentTimeMillis();

            boardHistory.clear();
            turnHistory.clear();
            gameLogic = new GameLogic();

            // Time limits are already set in the constructor based on gameMode.
            LOGGER.info(this.gameMode + " 启动，时间限制: " + currentGameTimeLimitMillis/1000 + "s (总), " + currentMoveTimeLimitMillis/1000 + "s (步).");

            saveCurrentBoardState();

            Info startMsgRed = new Info(Process.game_start, gameLogic.getGameBoard().getBoardState(), true, true,
                    "游戏开始！您执红棋，请走棋。对手: " + playerBlack.getUsername(), playerBlack.getUsername());
            startMsgRed.setGameStartTime(gameStartTimeMillis);
            startMsgRed.setGameTimeLimitMillis(this.currentGameTimeLimitMillis);
            startMsgRed.setMoveTimeLimitMillis(this.currentMoveTimeLimitMillis);
            playerRed.sendMessageToClient(startMsgRed);

            Info startMsgBlack = new Info(Process.game_start, gameLogic.getGameBoard().getBoardState(), false, false,
                    "游戏开始！您执黑棋，等待红方走棋。对手: " + playerRed.getUsername(), playerRed.getUsername());
            startMsgBlack.setGameStartTime(gameStartTimeMillis);
            startMsgBlack.setGameTimeLimitMillis(this.currentGameTimeLimitMillis);
            startMsgBlack.setMoveTimeLimitMillis(this.currentMoveTimeLimitMillis);
            playerBlack.sendMessageToClient(startMsgBlack);

            LOGGER.info("游戏已在房间 (" + playerRed.getUsername() + " vs " + playerBlack.getUsername() + ") 开始。红方先行。");
            scheduleMoveTimeout();
        }
    }

    public void processClientMessage(ClientHandler sender, Info message) {
        synchronized (gameLock) {
            if (!gameActive &&
                    message.getProcess() != Process.withdraw_accept && message.getProcess() != Process.withdraw_reject &&
                    message.getProcess() != Process.even_accept && message.getProcess() != Process.even_reject) {
                sender.sendMessageToClient(Info.createServerMessage("游戏尚未开始或已结束。"));
                return;
            }

            boolean isSenderRed = (sender == playerRed);
            ClientHandler opponent = isSenderRed ? playerBlack : playerRed;
            boolean senderIsCurrentTurnPlayerInLogic = (isSenderRed == gameLogic.isRedTurn());
            String chat = message.getChatMessage();

            boolean turnCheckFailed = false;
            switch (message.getProcess()) {
                case gaming:
                case want_lose:
                case want_even:
                    if (!senderIsCurrentTurnPlayerInLogic) {
                        sender.sendMessageToClient(new Info(Process.message, gameLogic.getGameBoard().getBoardState(), isSenderRed, false, "还未轮到您执行此操作。", opponent.getUsername()));
                        turnCheckFailed = true;
                    }
                    break;
                case withdraw:
                    if (senderIsCurrentTurnPlayerInLogic) {
                        sender.sendMessageToClient(new Info(Process.message, gameLogic.getGameBoard().getBoardState(), isSenderRed, true, "您只能在您走棋之后，轮到对方时请求悔棋。", opponent.getUsername()));
                        turnCheckFailed = true;
                    }
                    break;
            }
            if (turnCheckFailed) {
                return; // Do not proceed further, do not cancel/reschedule timer
            }

            // If an action is about to be processed (that isn't just chat AND isn't an invalid move that keeps the timer running),
            // cancel the current timer. A new one will be scheduled if the game continues and turn changes.
            boolean shouldCancelTimerBeforeProcessing = true;
            if (message.getProcess() == Process.chat ||
                    (message.getProcess() == Process.gaming && pendingWithdrawRequester != null) || // If pending request, gaming is blocked
                    (message.getProcess() == Process.gaming && pendingEvenRequester != null) ) {
                shouldCancelTimerBeforeProcessing = false;
            }


            if (shouldCancelTimerBeforeProcessing && currentMoveTimerTask != null) {
                currentMoveTimerTask.cancel();
                currentMoveTimerTask = null;
                LOGGER.fine("Timer task cancelled before processing message: " + message.getProcess());
            }

            switch (message.getProcess()) {
                case gaming:
                    if (pendingWithdrawRequester != null || pendingEvenRequester != null) {
                        sender.sendMessageToClient(Info.createServerMessage("当前有待处理的请求（悔棋/和棋），请先响应。"));
                        if (gameActive) scheduleMoveTimeout(); // Reschedule for current player as their action was blocked
                        return;
                    }
                    boolean moveValid = gameLogic.processMove(message.getOy(), message.getOx(), message.getNy(), message.getNx(), isSenderRed);
                    if (moveValid) {
                        gameLogic.switchTurn();
                        saveCurrentBoardState();

                        Process gameState = gameLogic.checkWinLossCondition();
                        String statusMessage = chat != null ? sender.getUsername() + ": " + chat : "对方已走棋。";

                        Info nextPlayerUpdate = new Info(gameState == Process.gaming ? Process.game_update : gameState,
                                deepCopyBoard(gameLogic.getGameBoard().getBoardState()),
                                !isSenderRed, true, statusMessage + "轮到您了。", sender.getUsername());
                        nextPlayerUpdate.setGameTimeLimitMillis(this.currentGameTimeLimitMillis);
                        nextPlayerUpdate.setMoveTimeLimitMillis(this.currentMoveTimeLimitMillis);
                        opponent.sendMessageToClient(nextPlayerUpdate);

                        Info senderUpdate = new Info(gameState == Process.gaming ? Process.game_update : gameState,
                                deepCopyBoard(gameLogic.getGameBoard().getBoardState()),
                                isSenderRed, false, chat != null ? "您: " + chat : "等待对方走棋...", opponent.getUsername());
                        senderUpdate.setGameTimeLimitMillis(this.currentGameTimeLimitMillis);
                        senderUpdate.setMoveTimeLimitMillis(this.currentMoveTimeLimitMillis);
                        sender.sendMessageToClient(senderUpdate);

                        LOGGER.info("房间 (" + playerRed.getUsername() + " vs " + playerBlack.getUsername() + "): " + sender.getUsername() + " 走棋成功. 轮到 " + opponent.getUsername());
                        if (gameState != Process.gaming) {
                            handleGameEnd(gameState);
                        } else if (gameActive) {
                            scheduleMoveTimeout(); // Schedule for the NEXT player
                        }
                    } else { // Invalid move
                        sender.sendMessageToClient(new Info(Process.game_update, deepCopyBoard(gameLogic.getGameBoard().getBoardState()), isSenderRed, true, "无效的走法，请重试。", opponent.getUsername()));
                        // For an invalid move, the timer for the current player (sender) should continue.
                        // Since it was cancelled at the start of processClientMessage (if shouldCancelTimerBeforeProcessing was true),
                        // we need to reschedule it for the SAME player.
                        if (gameActive) scheduleMoveTimeout();
                    }
                    break;

                case want_lose:
                    // Timer already cancelled if shouldCancelTimerBeforeProcessing was true
                    LOGGER.info("房间 (" + playerRed.getUsername() + " vs " + playerBlack.getUsername() + "): " + sender.getUsername() + " 投降。");
                    handleGameEnd(isSenderRed ? Process.lose : Process.win);
                    clearPendingRequests();
                    break;

                case chat:
                    if (chat != null && !chat.trim().isEmpty()) {
                        LOGGER.info("房间 (" + playerRed.getUsername() + " vs " + playerBlack.getUsername() + ") 聊天来自 " + sender.getUsername() + ": " + chat);
                        opponent.sendMessageToClient(new Info(Process.chat, null, !isSenderRed, gameLogic.isRedTurn() == !isSenderRed, sender.getUsername() + ": " + chat, sender.getUsername()));
                    }
                    // Chat does not affect the timer of the current player.
                    // If timer was cancelled at the start of processClientMessage, it needs to be rescheduled.
                    // However, shouldCancelTimerBeforeProcessing is false for chat. So timer was not cancelled.
                    break;

                case withdraw:
                    if (pendingWithdrawRequester != null || pendingEvenRequester != null) {
                        sender.sendMessageToClient(Info.createServerMessage("已有待处理的请求。"));
                        if (gameActive) scheduleMoveTimeout(); // Reschedule for current player (opponent of sender)
                        return;
                    }
                    if (!canUndo()) {
                        sender.sendMessageToClient(Info.createServerMessage("当前无法悔棋（棋局刚开始或无历史记录）。"));
                        if (gameActive) scheduleMoveTimeout(); // Reschedule for current player (opponent of sender)
                        return;
                    }
                    pendingWithdrawRequester = sender;
                    opponent.sendMessageToClient(new Info(Process.ask_withdraw, null, !isSenderRed, true, sender.getUsername() + " 请求悔棋，您是否同意？", sender.getUsername()));
                    sender.sendMessageToClient(Info.createServerMessage("已发送悔棋请求，等待对方 ("+opponent.getUsername()+") 响应..."));
                    LOGGER.info(sender.getUsername() + " 请求悔棋，等待 " + opponent.getUsername() + " 响应。");
                    // Timer for the opponent (who needs to respond) should now be active.
                    // The timer for gameLogic.isRedTurn() (which is opponent) was cancelled. Reschedule.
                    if(gameActive) scheduleMoveTimeout();
                    break;

                case withdraw_accept:
                    // Timer for responder (sender) was cancelled.
                    if (pendingWithdrawRequester == null ) {
                        sender.sendMessageToClient(Info.createServerMessage("当前没有待处理的悔棋请求。"));
                        if(gameActive) scheduleMoveTimeout(); // Reschedule for current player in gameLogic
                        return;
                    }
                    if (sender == pendingWithdrawRequester) {
                        sender.sendMessageToClient(Info.createServerMessage("您不能同意自己的悔棋请求。"));
                        if(gameActive) scheduleMoveTimeout(); // Reschedule for current player in gameLogic
                        return;
                    }
                    LOGGER.info(sender.getUsername() + " 同意了 " + pendingWithdrawRequester.getUsername() + " 的悔棋请求。");
                    performUndo();

                    ClientHandler requester = pendingWithdrawRequester;
                    boolean requesterIsRed = (requester == playerRed);

                    Info requesterMessage = new Info(
                            Process.withdraw_result,
                            deepCopyBoard(gameLogic.getGameBoard().getBoardState()),
                            requesterIsRed,
                            true,
                            "对方 ("+sender.getUsername()+") 同意悔棋，轮到您走棋。",
                            sender.getUsername()
                    );
                    requesterMessage.setGameTimeLimitMillis(this.currentGameTimeLimitMillis);
                    requesterMessage.setMoveTimeLimitMillis(this.currentMoveTimeLimitMillis);
                    requester.sendMessageToClient(requesterMessage);

                    ClientHandler acceptor = sender;
                    boolean acceptorIsRed = (acceptor == playerRed);
                    Info acceptorMessage = new Info(
                            Process.withdraw_result,
                            deepCopyBoard(gameLogic.getGameBoard().getBoardState()),
                            acceptorIsRed,
                            false,
                            "您同意了悔棋。等待 " + requester.getUsername() + " 走棋。",
                            requester.getUsername()
                    );
                    acceptorMessage.setGameTimeLimitMillis(this.currentGameTimeLimitMillis);
                    acceptorMessage.setMoveTimeLimitMillis(this.currentMoveTimeLimitMillis);
                    acceptor.sendMessageToClient(acceptorMessage);

                    pendingWithdrawRequester = null;
                    if(gameActive) scheduleMoveTimeout(); // Schedule for the requester (whose turn it is now)
                    break;

                case withdraw_reject:
                    // Timer for responder (sender) was cancelled.
                    if (pendingWithdrawRequester == null || sender == pendingWithdrawRequester) { /* ... */ if(gameActive && pendingWithdrawRequester != null) scheduleMoveTimeout(); return; }
                    LOGGER.info(sender.getUsername() + " 拒绝了 " + pendingWithdrawRequester.getUsername() + " 的悔棋请求。");
                    pendingWithdrawRequester.sendMessageToClient(Info.createServerMessage("对方 ("+sender.getUsername()+") 拒绝了您的悔棋请求。游戏继续。"));
                    sender.sendMessageToClient(Info.createServerMessage("您拒绝了对方 ("+pendingWithdrawRequester.getUsername()+") 的悔棋请求。游戏继续。"));
                    if (gameActive) scheduleMoveTimeout(); // Reschedule for the player whose turn it still is
                    pendingWithdrawRequester = null;
                    break;

                case want_even:
                    if (pendingWithdrawRequester != null || pendingEvenRequester != null) { /* ... */ if(gameActive) scheduleMoveTimeout(); return; }
                    pendingEvenRequester = sender;
                    opponent.sendMessageToClient(new Info(Process.ask_even, null, !isSenderRed, true, sender.getUsername() + " 请求和棋，您是否同意？", sender.getUsername()));
                    sender.sendMessageToClient(Info.createServerMessage("已发送和棋请求，等待对方 ("+opponent.getUsername()+") 响应..."));
                    if(gameActive) scheduleMoveTimeout(); // Schedule for opponent who needs to respond
                    break;

                case even_accept:
                    if (pendingEvenRequester == null || sender == pendingEvenRequester) { /* ... */ if(gameActive && pendingEvenRequester != null) scheduleMoveTimeout(); return; }
                    LOGGER.info(sender.getUsername() + " 同意了 " + pendingEvenRequester.getUsername() + " 的和棋请求。");
                    handleGameEnd(Process.draw);
                    pendingEvenRequester = null;
                    // Timer cancelled by handleGameEnd
                    break;

                case even_reject:
                    if (pendingEvenRequester == null || sender == pendingEvenRequester) { /* ... */ if(gameActive && pendingEvenRequester != null) scheduleMoveTimeout(); return; }
                    LOGGER.info(sender.getUsername() + " 拒绝了 " + pendingEvenRequester.getUsername() + " 的和棋请求。");
                    pendingEvenRequester.sendMessageToClient(Info.createServerMessage("对方 ("+sender.getUsername()+") 拒绝了您的和棋请求。"));
                    sender.sendMessageToClient(Info.createServerMessage("您拒绝了对方 ("+pendingEvenRequester.getUsername()+") 的和棋请求。游戏继续。"));
                    if (gameActive) scheduleMoveTimeout(); // Reschedule for current player
                    pendingEvenRequester = null;
                    break;

                default:
                    sender.sendMessageToClient(Info.createServerMessage("游戏中无法识别的指令。"));
                    LOGGER.warning("房间 (" + playerRed.getUsername() + " vs " + playerBlack.getUsername() + "): 收到来自 " + sender.getUsername() + " 的未知游戏指令: " + message.getProcess());
                    if(gameActive) scheduleMoveTimeout(); // Reschedule if an unknown command didn't change turn
                    break;
            }
        }
    }

    private void clearPendingRequests() {
        synchronized(gameLock){
            pendingWithdrawRequester = null;
            pendingEvenRequester = null;
        }
    }

    private void handleGameEnd(Process gameResultTrigger) {
        synchronized(gameLock){
            if (!gameActive && gameResultTrigger != Process.timeout_lose) {
                LOGGER.info("handleGameEnd called but game is not active or not a timeout. Trigger: " + gameResultTrigger + " GameActive: " + gameActive);
                return;
            }
            if (gameActive || gameResultTrigger == Process.timeout_lose) { // Ensure it proceeds if it's a timeout, even if another thread just set gameActive false
                gameActive = false;
                cancelAllTimers();

                Process finalResultForRed, finalResultForBlack;
                String endMessageToRed, endMessageToBlack; // Separate messages for timeout
                ClientHandler winner = null, loser = null;

                if (gameResultTrigger == Process.draw) {
                    finalResultForRed = Process.draw;
                    finalResultForBlack = Process.draw;
                    endMessageToRed = "游戏结束！双方和棋。";
                    endMessageToBlack = "游戏结束！双方和棋。";
                } else if (gameResultTrigger == Process.timeout_lose) {
                    if (gameLogic.isRedTurn()) { // Red timed out
                        loser = playerRed; winner = playerBlack;
                        finalResultForRed = Process.timeout_lose; finalResultForBlack = Process.win;
                    } else { // Black timed out
                        loser = playerBlack; winner = playerRed;
                        finalResultForRed = Process.win; finalResultForBlack = Process.timeout_lose;
                    }
                    String loserName = (loser != null ? loser.getUsername() : "一方");
                    String winnerName = (winner != null ? winner.getUsername() : "另一方");
                    endMessageToRed = (finalResultForRed == Process.timeout_lose) ? "您已超时，判负！对手 " + winnerName + " 胜利。" : "对手 " + loserName + " 超时，您获胜！";
                    endMessageToBlack = (finalResultForBlack == Process.timeout_lose) ? "您已超时，判负！对手 " + winnerName + " 胜利。" : "对手 " + loserName + " 超时，您获胜！";
                    LOGGER.info("房间 (" + (playerRed != null ? playerRed.getUsername() : "P1") + " vs " + (playerBlack != null ? playerBlack.getUsername(): "P2") + "): 玩家 " + loserName + " 超时，" + winnerName + " 胜利！");

                } else { // Win/loss by game logic (checkmate, surrender)
                    boolean redPlayerWon;
                    if (gameLogic.isRedTurn()) {
                        redPlayerWon = (gameResultTrigger == Process.win);
                    } else {
                        redPlayerWon = (gameResultTrigger == Process.lose);
                    }

                    if (redPlayerWon) {
                        winner = playerRed; loser = playerBlack;
                        finalResultForRed = Process.win; finalResultForBlack = Process.lose;
                    } else {
                        winner = playerBlack; loser = playerRed;
                        finalResultForRed = Process.lose; finalResultForBlack = Process.win;
                    }
                    String winnerNameDisplay = (winner != null ? winner.getUsername() : "一方") + " (" + (winner == playerRed ? "红" : "黑") + ")";
                    endMessageToRed = "游戏结束！" + winnerNameDisplay + " 胜利！";
                    endMessageToBlack = endMessageToRed; // Same message for both in this case
                    LOGGER.info("房间 (" + (playerRed != null ? playerRed.getUsername() : "P1") + " vs " + (playerBlack != null ? playerBlack.getUsername(): "P2") + "): " + endMessageToRed);
                }

                Info redEndInfo = new Info(finalResultForRed, deepCopyBoard(gameLogic.getGameBoard().getBoardState()), true, false, endMessageToRed, (playerBlack != null ? playerBlack.getUsername() : "对手"));
                redEndInfo.setGameTimeLimitMillis(this.currentGameTimeLimitMillis);
                redEndInfo.setMoveTimeLimitMillis(this.currentMoveTimeLimitMillis);
                if (playerRed != null && !playerRed.isSocketClosed()) playerRed.sendMessageToClient(redEndInfo);

                Info blackEndInfo = new Info(finalResultForBlack, deepCopyBoard(gameLogic.getGameBoard().getBoardState()), false, false, endMessageToBlack, (playerRed != null ? playerRed.getUsername() : "对手"));
                blackEndInfo.setGameTimeLimitMillis(this.currentGameTimeLimitMillis);
                blackEndInfo.setMoveTimeLimitMillis(this.currentMoveTimeLimitMillis);
                if (playerBlack != null && !playerBlack.isSocketClosed()) playerBlack.sendMessageToClient(blackEndInfo);

                if (playerRed != null) playerRed.setGameRoom(null);
                if (playerBlack != null) playerBlack.setGameRoom(null);
                clearPendingRequests();
            } else {
                LOGGER.info("handleGameEnd called but game was already inactive and not a timeout. No action taken. Trigger: " + gameResultTrigger);
            }
        }
    }

    public void notifyPlayerDisconnect(ClientHandler disconnectedPlayer) {
        synchronized(gameLock){
            ClientHandler otherPlayer = null;
            if (disconnectedPlayer == playerRed && playerBlack != null) {
                otherPlayer = playerBlack;
            } else if (disconnectedPlayer == playerBlack && playerRed != null) {
                otherPlayer = playerRed;
            }

            String disconnectedUsername = (disconnectedPlayer != null && disconnectedPlayer.getUsername() != null) ? disconnectedPlayer.getUsername() : "对手";

            if (!gameActive && pendingWithdrawRequester == null && pendingEvenRequester == null) {
                LOGGER.info("玩家 " + disconnectedUsername + " 在游戏非活动时断开连接。");
                if (disconnectedPlayer != null) disconnectedPlayer.setGameRoom(null);
                return;
            }

            if(pendingWithdrawRequester == disconnectedPlayer || pendingEvenRequester == disconnectedPlayer) {
                if (otherPlayer != null && !otherPlayer.isSocketClosed()) {
                    otherPlayer.sendMessageToClient(Info.createServerMessage(disconnectedUsername + " 已断开，" + (pendingWithdrawRequester != null ? "悔棋" : "和棋") + "请求已取消。"));
                }
            } else if (otherPlayer != null && (pendingWithdrawRequester == otherPlayer || pendingEvenRequester == otherPlayer) ) {
                if (otherPlayer != null && !otherPlayer.isSocketClosed()){
                    otherPlayer.sendMessageToClient(Info.createServerMessage("对方 ("+ disconnectedUsername +") 已断开，您的" + (pendingWithdrawRequester != null ? "悔棋" : "和棋") + "请求已取消。"));
                }
            }
            clearPendingRequests();

            if (gameActive) {
                gameActive = false;
                cancelAllTimers();
                ClientHandler winner = otherPlayer;

                LOGGER.info("房间玩家 " + disconnectedUsername + " 断开连接。");

                if (winner != null && winner.getUsername() != null && !winner.isSocketClosed()) {
                    String endMessage = "对手 " + disconnectedUsername + " 已断开连接，您获胜！";
                    Info winnerInfo = new Info(Process.win, deepCopyBoard(gameLogic.getGameBoard().getBoardState()), (winner == playerRed), false, endMessage, disconnectedUsername);
                    winnerInfo.setGameTimeLimitMillis(this.currentGameTimeLimitMillis);
                    winnerInfo.setMoveTimeLimitMillis(this.currentMoveTimeLimitMillis);
                    winner.sendMessageToClient(winnerInfo);
                    LOGGER.info(winner.getUsername() + " 因对手断线而获胜。");
                    winner.setGameRoom(null);
                }
            }
            if (disconnectedPlayer != null) disconnectedPlayer.setGameRoom(null);
        }
    }
}