package Game.Client;

import javax.swing.*;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Client {
    private String serverAddress = "localhost";
    private int serverPort = 12345;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    private String username;
    private boolean amIRed;
    private boolean isMyTurn;

    private GameGUI gameGUI;
    private LoginFrame loginFrame;
    private RegisterFrame registerFrame;

    private static final Logger LOGGER = Logger.getLogger(Client.class.getName());
    private volatile boolean running = true;

    public Client() {
    }

    public void start() {
        SwingUtilities.invokeLater(() -> {
            loginFrame = new LoginFrame(this);
            loginFrame.setVisible(true);
        });
    }

    public boolean connect() {
        try {
            socket = new Socket(serverAddress, serverPort);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            LOGGER.info("已连接到服务器: " + serverAddress + ":" + serverPort);

            Thread listenerThread = new Thread(this::listenToServer);
            listenerThread.setDaemon(true);
            listenerThread.start();
            return true;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "客户端连接错误: " + e.getMessage(), e);
            running = false;
            return false;
        }
    }

    private void listenToServer() {
        try {
            Info serverMessage;
            while (running && (serverMessage = (Info) in.readObject()) != null) {
                LOGGER.fine("客户端收到消息: " + serverMessage);
                final Info finalMessage = serverMessage;

                SwingUtilities.invokeLater(() -> {
                    // Update core client state based on the message
                    // isMyTurn is crucial and should always be updated if present in the message,
                    // unless it's a purely informational message like CHAT that shouldn't alter turn state.
                    if (finalMessage.getProcess() != Process.chat) { // Chat messages don't change turn status
                        // Check if the message is intended to update this client's turn status
                        if (finalMessage.getProcess() == Process.game_start ||
                                finalMessage.getProcess() == Process.game_update ||
                                finalMessage.getProcess() == Process.withdraw_result) {
                            this.isMyTurn = finalMessage.isMyTurn();
                        }
                        // For win/lose/draw/timeout_lose, isMyTurn is usually set to false by the server for both players
                        else if (finalMessage.getProcess() == Process.win ||
                                finalMessage.getProcess() == Process.lose ||
                                finalMessage.getProcess() == Process.draw ||
                                finalMessage.getProcess() == Process.timeout_lose) {
                            this.isMyTurn = false; // Game ended
                        }
                    }

                    if (gameGUI != null && finalMessage.getBoard() != null) {
                        gameGUI.updateBoard(finalMessage.getBoard());
                    }


                    switch (finalMessage.getProcess()) {
                        case login_success:
                            this.username = finalMessage.getAccount();
                            if (loginFrame != null) loginFrame.dispose();
                            if (registerFrame != null) {
                                registerFrame.dispose();
                                registerFrame = null;
                            }
                            gameGUI = new GameGUI(this, username);
                            gameGUI.setVisible(true);
                            gameGUI.displayMessage("登录成功！ " + finalMessage.getChatMessage());
                            gameGUI.promptGameModeSelection();
                            break;
                        case login_fail:
                            if (loginFrame != null && loginFrame.isVisible()) {
                                loginFrame.showLoginError(finalMessage.getChatMessage());
                            } else if (registerFrame != null && registerFrame.isVisible()){
                                registerFrame.showError("登录失败: " + finalMessage.getChatMessage());
                            } else {
                                JOptionPane.showMessageDialog(null, "登录失败: " + finalMessage.getChatMessage(), "登录失败", JOptionPane.ERROR_MESSAGE);
                            }
                            break;
                        case register_success:
                            if (registerFrame != null && registerFrame.isVisible()) {
                                registerFrame.showRegisterSuccess(finalMessage.getChatMessage());
                            } else {
                                JOptionPane.showMessageDialog(null, finalMessage.getChatMessage(), "注册成功", JOptionPane.INFORMATION_MESSAGE);
                            }
                            break;
                        case register_fail:
                            if (registerFrame != null && registerFrame.isVisible()) {
                                registerFrame.showRegisterError(finalMessage.getChatMessage());
                            } else {
                                JOptionPane.showMessageDialog(null, "注册失败: " + finalMessage.getChatMessage(), "注册失败", JOptionPane.ERROR_MESSAGE);
                            }
                            break;
                        case game_start:
                            this.amIRed = finalMessage.isRedPlayer();
                            // this.isMyTurn already updated if present
                            if (gameGUI == null) {
                                gameGUI = new GameGUI(this, username);
                                gameGUI.setVisible(true);
                            }
                            gameGUI.initializeGame(finalMessage.getBoard(), this.amIRed, finalMessage.getChatMessage());
                            gameGUI.updateTurnStatus(this.isMyTurn);
                            gameGUI.setOpponentName(finalMessage.getOpponentName());
                            if (finalMessage.getGameTimeLimitMillis() > 0 || finalMessage.getMoveTimeLimitMillis() > 0) { // Check if time limits are set
                                gameGUI.setTimers((int)(finalMessage.getGameTimeLimitMillis()/1000), (int)(finalMessage.getMoveTimeLimitMillis()/1000));
                            }
                            gameGUI.startGameTimers();
                            if(this.isMyTurn) gameGUI.resetMoveTimer();
                            break;
                        case game_update:
                            // this.isMyTurn already updated
                            if (gameGUI != null) {
                                // gameGUI.updateBoard already called if board was in message
                                gameGUI.updateTurnStatus(this.isMyTurn);
                                if (finalMessage.getChatMessage() != null && !finalMessage.getChatMessage().isEmpty()) {
                                    gameGUI.appendChatMessage(finalMessage.getChatMessage());
                                }
                                if(this.isMyTurn){
                                    if( !finalMessage.isInvalidMove() ) gameGUI.resetMoveTimer();
                                    else gameGUI.startGameTimers();
                                }
                                else gameGUI.pauseMoveTimer();
                            }
                            break;
                        case win:
                        case lose:
                        case draw:
                            // this.isMyTurn already set to false
                            if (gameGUI != null) {
                                gameGUI.showEndGameDialog(finalMessage.getProcess().toString(), finalMessage.getChatMessage());
                                gameGUI.stopTimers();
                                gameGUI.updateTurnStatus(this.isMyTurn);
                            }
                            break;
                        case timeout_lose:
                            // this.isMyTurn already set to false
                            if (gameGUI != null) {
                                // The chatMessage from server for timeout_lose should be "您已超时，判负！"
                                gameGUI.showEndGameDialog("超时判负", finalMessage.getChatMessage());
                                gameGUI.stopTimers();
                                gameGUI.updateTurnStatus(this.isMyTurn);
                            }
                            break;
                        case opponent_disconnected:
                            if (gameGUI != null) gameGUI.appendChatMessage(finalMessage.getChatMessage());
                            break;
                        case message:
                            if (gameGUI != null && gameGUI.isVisible()) {
                                gameGUI.displayMessage(finalMessage.getChatMessage());
                            } else if (loginFrame != null && loginFrame.isVisible()) {
                                loginFrame.showMessage(finalMessage.getChatMessage());
                            } else if (registerFrame != null && registerFrame.isVisible()) {
                                registerFrame.showMessage(finalMessage.getChatMessage());
                            }
                            else {
                                JOptionPane.showMessageDialog(null, finalMessage.getChatMessage(), "服务器消息", JOptionPane.INFORMATION_MESSAGE);
                            }
                            break;

                        case ask_withdraw:
                            if (gameGUI != null) {
                                gameGUI.askForWithdrawConfirmation(finalMessage.getChatMessage());
                            }
                            break;
                        case ask_even:
                            if (gameGUI != null) {
                                gameGUI.askForEvenConfirmation(finalMessage.getChatMessage());
                            }
                            break;
                        case withdraw_result:
                            if (gameGUI != null) {
                                gameGUI.displayMessage(finalMessage.getChatMessage());
                                // isMyTurn and board already updated if present in message
                                gameGUI.updateTurnStatus(this.isMyTurn);
                                if(this.isMyTurn) gameGUI.resetMoveTimer();
                            }
                            break;
                        case even_result:
                            if (gameGUI != null) {
                                gameGUI.displayMessage(finalMessage.getChatMessage());
                                gameGUI.updateTurnStatus(this.isMyTurn);
                            }
                            break;
                        case chat:
                            if (gameGUI != null && finalMessage.getChatMessage() != null) {
                                gameGUI.appendChatMessage(finalMessage.getChatMessage());
                            }
                            break;

                        default:
                            LOGGER.warning("收到未处理的服务器进程: " + finalMessage.getProcess());
                            break;
                    }
                });
            }
        } catch (EOFException | SocketException e) {
            if (running) {
                LOGGER.info("与服务器的连接已断开: " + e.getMessage());
                handleDisconnectionError("与服务器的连接已断开。");
            }
        } catch (IOException e) {
            if (running) {
                LOGGER.log(Level.WARNING, "从服务器读取消息时发生IO错误: " + e.getMessage(), e);
                handleDisconnectionError("读取服务器消息失败。");
            }
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.WARNING, "收到来自服务器的未知类: " + e.getMessage(), e);
        } finally {
            running = false;
            closeConnection();
        }
    }

    private void handleDisconnectionError(String errorMessage) {
        SwingUtilities.invokeLater(() -> {
            if (gameGUI != null && gameGUI.isVisible()) {
                gameGUI.showConnectionError(errorMessage);
            }
            if (registerFrame != null && registerFrame.isVisible()) {
                registerFrame.showError(errorMessage);
            }

            if (loginFrame == null || !loginFrame.isVisible()) {
                if (gameGUI != null) gameGUI.dispose(); gameGUI = null;
                if (registerFrame != null) registerFrame.dispose(); registerFrame = null;

                loginFrame = new LoginFrame(this);
                loginFrame.setVisible(true);
            }
            if (loginFrame != null) {
                loginFrame.showConnectionError(errorMessage + " 请尝试重新登录或检查服务器。");
            }
        });
    }


    public synchronized void sendMessageToServer(Info message) {
        if (socket == null || socket.isClosed() || out == null) {
            LOGGER.warning("尝试通过已关闭或未初始化的连接发送消息。");
            String errorMsg = "未连接到服务器，无法发送消息。";
            // Show error on the currently visible frame if possible
            JFrame currentVisibleFrame = null;
            if (gameGUI != null && gameGUI.isVisible()) currentVisibleFrame = gameGUI;
            else if (loginFrame != null && loginFrame.isVisible()) currentVisibleFrame = loginFrame;
            else if (registerFrame != null && registerFrame.isVisible()) currentVisibleFrame = registerFrame;

            if (currentVisibleFrame instanceof GameGUI) ((GameGUI)currentVisibleFrame).showConnectionError(errorMsg);
            else if (currentVisibleFrame instanceof LoginFrame) ((LoginFrame)currentVisibleFrame).showConnectionError(errorMsg);
            else if (currentVisibleFrame instanceof RegisterFrame) ((RegisterFrame)currentVisibleFrame).showError(errorMsg);
            else JOptionPane.showMessageDialog(null, errorMsg, "发送错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            out.writeObject(message);
            out.flush();
            out.reset();
            LOGGER.fine("客户端已发送消息: " + message);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "向服务器发送消息失败: " + e.getMessage(), e);
            String errorMsg = "发送消息到服务器失败。连接可能已断开。";
            // Similar error display as above
            JFrame currentVisibleFrame = null;
            if (gameGUI != null && gameGUI.isVisible()) currentVisibleFrame = gameGUI;
            else if (loginFrame != null && loginFrame.isVisible()) currentVisibleFrame = loginFrame;
            else if (registerFrame != null && registerFrame.isVisible()) currentVisibleFrame = registerFrame;

            if (currentVisibleFrame instanceof GameGUI) ((GameGUI)currentVisibleFrame).showConnectionError(errorMsg);
            else if (currentVisibleFrame instanceof LoginFrame) ((LoginFrame)currentVisibleFrame).showConnectionError(errorMsg);
            else if (currentVisibleFrame instanceof RegisterFrame) ((RegisterFrame)currentVisibleFrame).showError(errorMsg);
            else JOptionPane.showMessageDialog(null, errorMsg, "发送错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void openRegisterFrame() {
        if (loginFrame != null) loginFrame.setVisible(false);
        if (registerFrame == null || !registerFrame.isVisible()) {
            registerFrame = new RegisterFrame(this, loginFrame);
        }
        registerFrame.setVisible(true);
    }

    public void backToLoginFrame(boolean fromRegisterSuccess) {
        if (registerFrame != null) {
            registerFrame.dispose();
            registerFrame = null;
        }
        if (loginFrame == null || !loginFrame.isVisible()) {
            loginFrame = new LoginFrame(this);
        }
        loginFrame.setVisible(true);
        if(fromRegisterSuccess){
            loginFrame.showMessage("注册成功！请输入您的凭据登录。");
        }
    }


    private void closeConnection() {
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "关闭客户端连接时出错: " + e.getMessage());
        }
        LOGGER.info("客户端连接已关闭。");
    }

    public String getUsername() { return username; }
    public boolean isMyTurn() { return isMyTurn; }
    public boolean amIRed() { return amIRed; }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "无法设置系统 L&F", e);
        }
        Client client = new Client();
        client.start();
    }
}