package Game.Server;

import Game.Client.Info;
import Game.Client.Process;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private DatabaseManager dbManager;
    private String username;
    private GameRoom gameRoom;
    private boolean isLoggedIn = false;
    private Process lastGameModeAttempted = null; // To help with removal from correct queue on disconnect

    private static final Logger LOGGER = Logger.getLogger(ClientHandler.class.getName());

    public ClientHandler(Socket socket, DatabaseManager dbManager) {
        this.clientSocket = socket;
        this.dbManager = dbManager;
        try {
            out = new ObjectOutputStream(clientSocket.getOutputStream());
            in = new ObjectInputStream(clientSocket.getInputStream());
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "ClientHandler 初始化流失败 for " + clientSocket.getRemoteSocketAddress(), e);
            closeConnection();
        }
    }

    public String getUsername() {
        return username;
    }

    public boolean isSocketClosed() {
        return clientSocket == null || clientSocket.isClosed();
    }

    public void setGameRoom(GameRoom room) {
        this.gameRoom = room;
    }

    public GameRoom getGameRoom() {
        return this.gameRoom;
    }

    @Override
    public void run() {
        try {
            Info clientMessage;
            while (!isSocketClosed() && (clientMessage = (Info) in.readObject()) != null) {
                LOGGER.info("收到来自 " + (username != null ? username : clientSocket.getRemoteSocketAddress()) + " 的消息: " + clientMessage);

                if (!isLoggedIn) {
                    switch (clientMessage.getProcess()) {
                        case login:
                            handleLogin(clientMessage);
                            break;
                        case register:
                            handleRegister(clientMessage);
                            break;
                        default:
                            sendMessageToClient(Info.createLoginFail(clientMessage.getAccount(),"请先登录或注册。"));
                            break;
                    }
                } else {
                    if (gameRoom != null && gameRoom.isGameActive()) {
                        gameRoom.processClientMessage(this, clientMessage);
                    } else {
                        handleOutOfGameMessages(clientMessage);
                    }
                }
            }
        } catch (EOFException | SocketException e) {
            LOGGER.info("客户端 " + (username != null ? username : clientSocket.getRemoteSocketAddress()) + " 断开连接: " + e.getMessage());
        } catch (IOException e) {
            if (!isSocketClosed()){
                LOGGER.log(Level.WARNING, "处理客户端 " + (username != null ? username : clientSocket.getRemoteSocketAddress()) + " 时IO异常: " + e.getMessage(), e);
            }
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.WARNING, "收到来自 " + (username != null ? username : clientSocket.getRemoteSocketAddress()) + " 的未知类: " + e.getMessage(), e);
        } finally {
            if (gameRoom != null) {
                gameRoom.notifyPlayerDisconnect(this);
            }
            // Use lastGameModeAttempted to try removing from the correct queue
            Server.removePlayerFromWaitingQueue(this, lastGameModeAttempted != null ? lastGameModeAttempted : Process.normal_game);
            closeConnection();
            LOGGER.info("与客户端 " + (username != null ? username : clientSocket.getRemoteSocketAddress()) + " 的连接处理器已停止。");
        }
    }

    private void handleLogin(Info loginMessage) {
        String acc = loginMessage.getAccount();
        String pass = loginMessage.getPassword();

        if (acc == null || acc.trim().isEmpty() || pass == null || pass.isEmpty()) {
            sendMessageToClient(Info.createLoginFail(acc,"用户名或密码不能为空。"));
            return;
        }

        boolean authenticated = dbManager.authenticateUser(acc, pass);

        if (authenticated) {
            this.username = acc;
            this.isLoggedIn = true;
            sendMessageToClient(Info.createLoginSuccess(this.username, "登录成功！欢迎, " + this.username + "。请选择游戏模式。"));
            LOGGER.info("用户 " + this.username + " 登录成功。");
        } else {
            sendMessageToClient(Info.createLoginFail(acc,"登录失败：无效的用户名或密码。"));
            LOGGER.info("用户 " + acc + " 登录失败。");
        }
    }

    private void handleRegister(Info registerMessage) {
        String acc = registerMessage.getAccount();
        String pass = registerMessage.getPassword();

        if (acc == null || acc.trim().isEmpty() || pass == null || pass.isEmpty()) {
            sendMessageToClient(Info.createRegisterFail(acc, "用户名或密码不能为空。"));
            return;
        }
        if (acc.length() < 3 || pass.length() < 3) {
            sendMessageToClient(Info.createRegisterFail(acc, "用户名和密码长度至少为3位。"));
            return;
        }

        boolean success = dbManager.registerUser(acc, pass);

        if (success) {
            sendMessageToClient(Info.createRegisterSuccess(acc, "注册成功！现在您可以登录了。"));
            LOGGER.info("用户 " + acc + " 注册成功。");
        } else {
            sendMessageToClient(Info.createRegisterFail(acc, "注册失败：用户名可能已被占用或发生服务器错误。"));
            LOGGER.info("用户 " + acc + " 注册失败。");
        }
    }


    private void handleOutOfGameMessages(Info clientMessage) {
        switch (clientMessage.getProcess()) {
            case normal_game:
            case short_game:
                if (this.gameRoom != null && this.gameRoom.isGameActive()) {
                    sendMessageToClient(Info.createServerMessage("您已在游戏中，不能选择模式。"));
                } else {
                    this.lastGameModeAttempted = clientMessage.getProcess(); // Store mode
                    Server.addPlayerToWaitingQueue(this, clientMessage.getProcess());
                }
                break;
            case chat:
                LOGGER.info("收到来自 " + username + " 的大厅聊天 (未实现广播): " + clientMessage.getChatMessage());
                sendMessageToClient(Info.createServerMessage("大厅聊天功能暂未完全实现。"));
                break;
            default:
                LOGGER.warning("用户 " + username + " 发送了无法在游戏外处理的消息: " + clientMessage.getProcess());
                sendMessageToClient(Info.createServerMessage("当前状态无法处理该请求。"));
                break;
        }
    }


    public void sendMessageToClient(Info message) {
        if (isSocketClosed() || out == null) { // Use isSocketClosed()
            LOGGER.warning("尝试向已关闭的连接发送消息给 " + (username != null ? username : "未知客户端"));
            return;
        }
        try {
            out.writeObject(message);
            out.flush();
            out.reset();
            LOGGER.fine("已发送消息给 " + (username != null ? username : clientSocket.getRemoteSocketAddress()) + ": " + message);
        } catch (SocketException se) {
            LOGGER.info("向 " + (username != null ? username : "未知客户端") + " 发送消息时连接已重置/断开: " + se.getMessage());
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "向 " + (username != null ? username : clientSocket.getRemoteSocketAddress()) + " 发送消息失败: " + e.getMessage(), e);
        }
    }

    private void closeConnection() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "关闭客户端连接时出错 for " + (username != null ? username : clientSocket.getRemoteSocketAddress()), e);
        }
        isLoggedIn = false;
        clientSocket = null; // Help GC and isSocketClosed()
        LOGGER.info("连接已关闭 for " + (username != null ? username : "未知客户端"));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClientHandler that = (ClientHandler) o;
        if (username != null) return username.equals(that.username);
        // Fallback to socket if username is null, but be cautious as socket might be null after closeConnection
        if (clientSocket != null && that.clientSocket != null) return clientSocket.equals(that.clientSocket);
        return false;
    }

    @Override
    public int hashCode() {
        return username != null ? username.hashCode() : 0;
    }
}