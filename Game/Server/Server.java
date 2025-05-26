package Game.Server;

import Game.Client.Info;
import Game.Client.Process;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server {
    private static final int PORT = 12345;
    private static final Logger LOGGER = Logger.getLogger(Server.class.getName());
    private static ExecutorService gameRoomExecutor = Executors.newCachedThreadPool();
    private static ExecutorService clientHandlerExecutor = Executors.newCachedThreadPool();

    // Separate waiting queues for different game modes
    protected static List<ClientHandler> waitingNormalModePlayers = new ArrayList<>();
    protected static List<ClientHandler> waitingShortModePlayers = new ArrayList<>();
    protected static final Object normalQueueLock = new Object();
    protected static final Object shortQueueLock = new Object();


    private static DatabaseManager dbManager;

    public static void main(String[] args) {
        LOGGER.info("中国象棋服务器启动中...");
        dbManager = new DatabaseManager();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            LOGGER.info("服务器已在端口 " + PORT + " 上监听...");

            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    LOGGER.info("新客户端连接: " + clientSocket.getRemoteSocketAddress());

                    ClientHandler clientHandler = new ClientHandler(clientSocket, dbManager);
                    clientHandlerExecutor.execute(clientHandler);

                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "接受客户端连接失败: " + e.getMessage(), e);
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "服务器主套接字错误: " + e.getMessage(), e);
        } finally {
            gameRoomExecutor.shutdown();
            clientHandlerExecutor.shutdown();
            LOGGER.info("服务器已关闭。");
        }
    }

    public static void addPlayerToWaitingQueue(ClientHandler player, Process gameMode) {
        if (player == null || player.getUsername() == null) {
            LOGGER.warning("尝试将未登录或无用户名的玩家添加到等待队列。");
            if(player != null) player.sendMessageToClient(Info.createServerMessage("错误：您需要先登录才能加入游戏。"));
            return;
        }

        if (gameMode == Process.normal_game) {
            synchronized (normalQueueLock) {
                if (waitingNormalModePlayers.contains(player)) {
                    player.sendMessageToClient(Info.createServerMessage("您已在普通模式等待队列中。"));
                    return;
                }
                // Remove from other queue if present
                synchronized(shortQueueLock) { waitingShortModePlayers.remove(player); }

                waitingNormalModePlayers.add(player);
                LOGGER.info("玩家 " + player.getUsername() + " 已添加到普通模式等待队列。队列人数: " + waitingNormalModePlayers.size());
                player.sendMessageToClient(Info.createServerMessage("已加入普通模式等待队列，请稍候..."));
                tryMatchPlayers(Process.normal_game);
            }
        } else if (gameMode == Process.short_game) {
            synchronized (shortQueueLock) {
                if (waitingShortModePlayers.contains(player)) {
                    player.sendMessageToClient(Info.createServerMessage("您已在快速模式等待队列中。"));
                    return;
                }
                // Remove from other queue if present
                synchronized(normalQueueLock) { waitingNormalModePlayers.remove(player); }

                waitingShortModePlayers.add(player);
                LOGGER.info("玩家 " + player.getUsername() + " 已添加到快速模式等待队列。队列人数: " + waitingShortModePlayers.size());
                player.sendMessageToClient(Info.createServerMessage("已加入快速模式等待队列，请稍候..."));
                tryMatchPlayers(Process.short_game);
            }
        }
        else {
            LOGGER.warning("玩家 " + player.getUsername() + " 尝试加入不支持的游戏模式: " + gameMode);
            player.sendMessageToClient(Info.createServerMessage("错误：不支持的游戏模式。"));
        }
    }

    private static void tryMatchPlayers(Process gameMode) {
        if (gameMode == Process.normal_game) {
            synchronized (normalQueueLock) {
                waitingNormalModePlayers.removeIf(ClientHandler::isSocketClosed); // Use method reference

                if (waitingNormalModePlayers.size() >= 2) {
                    ClientHandler player1Handler = waitingNormalModePlayers.remove(0);
                    ClientHandler player2Handler = waitingNormalModePlayers.remove(0);

                    if (player1Handler.isSocketClosed()) {
                        LOGGER.info("玩家 " + (player1Handler.getUsername() != null ? player1Handler.getUsername() : "未知") + " 在匹配前已断开(普通)，将 " + player2Handler.getUsername() + " 放回队列。");
                        waitingNormalModePlayers.add(0, player2Handler);
                        tryMatchPlayers(gameMode);
                        return;
                    }
                    if (player2Handler.isSocketClosed()) {
                        LOGGER.info("玩家 " + (player2Handler.getUsername() != null ? player2Handler.getUsername() : "未知") + " 在匹配前已断开(普通)，将 " + player1Handler.getUsername() + " 放回队列。");
                        waitingNormalModePlayers.add(0, player1Handler);
                        tryMatchPlayers(gameMode);
                        return;
                    }

                    LOGGER.info("匹配成功 (普通模式): " + player1Handler.getUsername() + " vs " + player2Handler.getUsername());
                    GameRoom gameRoom = new GameRoom(player1Handler, player2Handler, Process.normal_game); // Pass mode
                    gameRoomExecutor.execute(gameRoom);
                }
            }
        } else if (gameMode == Process.short_game) {
            synchronized (shortQueueLock) {
                waitingShortModePlayers.removeIf(ClientHandler::isSocketClosed);

                if (waitingShortModePlayers.size() >= 2) {
                    ClientHandler player1Handler = waitingShortModePlayers.remove(0);
                    ClientHandler player2Handler = waitingShortModePlayers.remove(0);

                    if (player1Handler.isSocketClosed()) {
                        LOGGER.info("玩家 " + (player1Handler.getUsername() != null ? player1Handler.getUsername() : "未知") + " 在匹配前已断开(快速)，将 " + player2Handler.getUsername() + " 放回队列。");
                        waitingShortModePlayers.add(0, player2Handler);
                        tryMatchPlayers(gameMode);
                        return;
                    }
                    if (player2Handler.isSocketClosed()) {
                        LOGGER.info("玩家 " + (player2Handler.getUsername() != null ? player2Handler.getUsername() : "未知") + " 在匹配前已断开(快速)，将 " + player1Handler.getUsername() + " 放回队列。");
                        waitingShortModePlayers.add(0, player1Handler);
                        tryMatchPlayers(gameMode);
                        return;
                    }
                    LOGGER.info("匹配成功 (快速模式): " + player1Handler.getUsername() + " vs " + player2Handler.getUsername());
                    GameRoom gameRoom = new GameRoom(player1Handler, player2Handler, Process.short_game); // Pass mode
                    gameRoomExecutor.execute(gameRoom);
                }
            }
        }
    }

    public static void removePlayerFromWaitingQueue(ClientHandler player, Process gameModeAttempted) {
        // It's possible the player wasn't in the queue for gameModeAttempted if they disconnected before choosing
        // Or if they were moved. For simplicity, try removing from both.
        boolean removedNormal = false;
        boolean removedShort = false;
        synchronized (normalQueueLock) {
            removedNormal = waitingNormalModePlayers.remove(player);
        }
        synchronized (shortQueueLock) {
            removedShort = waitingShortModePlayers.remove(player);
        }
        if (removedNormal || removedShort) {
            LOGGER.info("玩家 " + (player.getUsername() != null ? player.getUsername() : "未知") + " 已从等待队列中移除 (尝试模式: " + gameModeAttempted + ").");
        }
    }
}
