package Game.Client;

import Game.GameLogic;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects; // 用于 Objects.requireNonNull

public class GameGUI extends JFrame {
    private Client client;
    private String localPlayerName;
    private String opponentPlayerName = "对方"; // 默认

    // --- 来自 GameView 的常量和变量 ---
    private static final int PIECE_WIDTH = 55, PIECE_HEIGHT = 55; // 棋子图标大小 (略微调整)
    private static final int CELL_SIZE = 60; // 棋盘格子视觉大小 (来自SCOL_GAP, SROW_GAP)
    private static final int BOARD_OFFSET_X = 30; // 棋盘左边距
    private static final int BOARD_OFFSET_Y = 50; // 棋盘上边距

    // 棋盘逻辑大小 (10行 x 9列)
    private static final int LOGICAL_ROWS = 10;
    private static final int LOGICAL_COLS = 9;

    private String[][] currentBoardModel; // 存储当前棋盘状态 (board[row][col])
    private boolean isLocalPlayerRed; // 本地玩家是否执红

    private JLayeredPane layeredPane;
    private ChessBoardPanel chessBoardPanel; // 棋盘绘制面板
    private JLabel turnIndicatorLabel; // 显示轮到谁
    private JLabel localPlayerLabel, opponentPlayerLabel; // 玩家昵称
    // private JLabel localPlayerAvatar, opponentPlayerAvatar; // 玩家头像 (暂用文字代替)

    private JTextArea chatDisplayArea;
    private JTextField chatInputField;
    private JButton sendChatButton;
    private JButton surrenderButton, requestDrawButton, requestUndoButton; // 游戏操作按钮

    private ClockPanel clockPanel; // 计时器面板

    private int selectedPieceRow = -1, selectedPieceCol = -1; // 选中的棋子逻辑坐标

    public GameGUI(Client client, String playerName) {
        this.client = client;
        this.localPlayerName = playerName;

        setTitle("中国象棋 - " + playerName);
        setSize(950, 850); // 调整窗口大小以容纳聊天和按钮
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // 或 DISPOSE_ON_CLOSE 如果希望返回登录

        initComponents();
    }

    private void initComponents() {
        layeredPane = new JLayeredPane();
        getContentPane().add(layeredPane, BorderLayout.CENTER);

        // 1. 背景 (可以是一个大的JLabel)
        JLabel backgroundLabel = new JLabel(new ImageIcon(Objects.requireNonNull(getClass().getResource("./img/bg.png")))); //确保图片在类路径的img文件夹下
        backgroundLabel.setBounds(0, 0, getWidth(), getHeight());
        layeredPane.add(backgroundLabel, Integer.valueOf(0)); // 最底层

        // 2. 棋盘面板
        chessBoardPanel = new ChessBoardPanel();
        // 棋盘的精确尺寸: (LOGICAL_COLS-1) * CELL_SIZE + PIECE_WIDTH for width
        // (LOGICAL_ROWS-1) * CELL_SIZE + PIECE_HEIGHT for height
        // 但由于棋盘线本身也占像素，通常直接使用背景图尺寸或固定尺寸
        int boardDisplayWidth = (LOGICAL_COLS -1) * CELL_SIZE + PIECE_WIDTH +20; // 估算棋盘绘制区域宽度
        int boardDisplayHeight = (LOGICAL_ROWS-1) * CELL_SIZE + PIECE_HEIGHT +20; // 估算棋盘绘制区域高度
        chessBoardPanel.setBounds(BOARD_OFFSET_X, BOARD_OFFSET_Y, boardDisplayWidth, boardDisplayHeight);
        chessBoardPanel.setOpaque(false); // ChessBoardPanel 本身透明，依赖背景图
        layeredPane.add(chessBoardPanel, Integer.valueOf(1));


        // 3. 玩家信息和回合指示器 (放在棋盘上方或侧边)
        JPanel topPanel = new JPanel(new BorderLayout(10,0));
        topPanel.setOpaque(false);
        topPanel.setBorder(new EmptyBorder(5, BOARD_OFFSET_X, 5, BOARD_OFFSET_X));

        opponentPlayerLabel = new JLabel(opponentPlayerName + " (对方)", SwingConstants.LEFT);
        opponentPlayerLabel.setFont(new Font("Serif", Font.BOLD, 16));
        opponentPlayerLabel.setForeground(Color.WHITE);
        topPanel.add(opponentPlayerLabel, BorderLayout.WEST);

        turnIndicatorLabel = new JLabel("等待游戏开始", SwingConstants.CENTER);
        turnIndicatorLabel.setFont(new Font("Serif", Font.BOLD, 18));
        turnIndicatorLabel.setForeground(Color.ORANGE);
        topPanel.add(turnIndicatorLabel, BorderLayout.CENTER);

        // 计时器 (放在顶部右侧)

        clockPanel = new ClockPanel(1000000, 10); // 初始时间由服务器设定
        clockPanel.setPreferredSize(new Dimension(120, 50));
        topPanel.add(clockPanel, BorderLayout.EAST);

        // 将topPanel添加到layeredPane的特定位置，而不是JFrame的BorderLayout.NORTH
        // topPanel.setBounds(0,0, getWidth(), 60); // 需要精确计算位置和大小
        // layeredPane.add(topPanel, Integer.valueOf(2));
        // 或者，更简单的方式是将layeredPane作为JFrame的CENTER，然后其他控制面板用BorderLayout放置
        // 为了简单起见，暂时将控制组件直接添加到layeredPane的较高层
        // 这里我们改变策略，使用一个主JPanel，它使用BorderLayout
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setOpaque(false); // 主面板透明，让背景可见
        // getContentPane().add(mainPanel); // 如果不用layeredPane作为根
        // mainPanel.add(layeredPane, BorderLayout.CENTER); // layeredPane放中间

        // 我们还是用 layeredPane 作为根，然后把控制面板放上面
        topPanel.setBounds(BOARD_OFFSET_X, BOARD_OFFSET_Y - 55, boardDisplayWidth, 50); // 棋盘上方
        layeredPane.add(topPanel, Integer.valueOf(2));


        localPlayerLabel = new JLabel(localPlayerName + " (我)", SwingConstants.LEFT);
        localPlayerLabel.setFont(new Font("Serif", Font.BOLD, 16));
        localPlayerLabel.setForeground(Color.WHITE);
        localPlayerLabel.setBounds(BOARD_OFFSET_X, BOARD_OFFSET_Y + boardDisplayHeight + 10, 200, 30);
        layeredPane.add(localPlayerLabel, Integer.valueOf(2));


        // 4. 聊天区域和操作按钮 (放在棋盘右侧或下方)
        JPanel controlPanel = new JPanel(new BorderLayout(5, 5));
        controlPanel.setOpaque(false);
        controlPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY), "游戏控制与聊天", 0,0, null, Color.WHITE));

        chatDisplayArea = new JTextArea(10, 25);
        chatDisplayArea.setEditable(false);
        chatDisplayArea.setLineWrap(true);
        chatDisplayArea.setWrapStyleWord(true);
        JScrollPane chatScrollPane = new JScrollPane(chatDisplayArea);
        controlPanel.add(chatScrollPane, BorderLayout.CENTER);

        JPanel chatInputPanel = new JPanel(new BorderLayout(5,0));
        chatInputPanel.setOpaque(false);
        chatInputField = new JTextField();
        sendChatButton = new JButton("发送");
        chatInputPanel.add(chatInputField, BorderLayout.CENTER);
        chatInputPanel.add(sendChatButton, BorderLayout.EAST);
        controlPanel.add(chatInputPanel, BorderLayout.SOUTH);

        JPanel gameActionsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        gameActionsPanel.setOpaque(false);
        surrenderButton = new JButton("投降");
        requestDrawButton = new JButton("求和");
        requestUndoButton = new JButton("悔棋");
        gameActionsPanel.add(surrenderButton);
        gameActionsPanel.add(requestDrawButton);
        gameActionsPanel.add(requestUndoButton);
        controlPanel.add(gameActionsPanel, BorderLayout.NORTH); // 按钮放聊天区上面

        // 设置controlPanel的位置和大小
        controlPanel.setBounds(BOARD_OFFSET_X + boardDisplayWidth + 10, BOARD_OFFSET_Y,
                250, boardDisplayHeight); // 放在棋盘右侧
        layeredPane.add(controlPanel, Integer.valueOf(2));

        setupActionListeners();
    }

    private void setupActionListeners() {
        sendChatButton.addActionListener(e -> {
            String message = chatInputField.getText().trim();
            if (!message.isEmpty()) {
                client.sendMessageToServer(new Info(Process.chat, message));
                chatInputField.setText("");
                appendChatMessage(localPlayerName + " (我): " + message); // 本地也显示，服务器也会广播回来
            }
        });
        chatInputField.addActionListener(sendChatButton.getActionListeners()[0]); // 回车发送

        surrenderButton.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(this, "您确定要投降吗？", "确认投降", JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) {
                client.sendMessageToServer(new Info(Process.want_lose, "我选择投降。"));
            }
        });
        requestDrawButton.addActionListener(e -> client.sendMessageToServer(new Info(Process.want_even, "我请求和棋。")));
        requestUndoButton.addActionListener(e -> client.sendMessageToServer(new Info(Process.withdraw, "我请求悔棋。")));
    }

    public void askForWithdrawConfirmation(String requestMessage) {
        int choice = JOptionPane.showConfirmDialog(this, requestMessage, "悔棋请求", JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            client.sendMessageToServer(new Info(Process.withdraw_accept, "同意悔棋。"));
        } else {
            client.sendMessageToServer(new Info(Process.withdraw_reject, "拒绝悔棋。"));
        }
        requestUndoButton.setEnabled(true); // 响应后可以再次发起
    }

    public void askForEvenConfirmation(String requestMessage) {
        int choice = JOptionPane.showConfirmDialog(this, requestMessage, "和棋请求", JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            client.sendMessageToServer(new Info(Process.even_accept, "同意和棋。"));
        } else {
            client.sendMessageToServer(new Info(Process.even_reject, "拒绝和棋。"));
        }
        requestDrawButton.setEnabled(true); // 响应后可以再次发起
    }

    public void initializeGame(String[][] board, boolean amIRed, String message) {
        this.currentBoardModel = board;
        this.isLocalPlayerRed = amIRed;
        chessBoardPanel.updateBoardPieces(currentBoardModel, isLocalPlayerRed);
        appendChatMessage(message);
        localPlayerLabel.setText(localPlayerName + (isLocalPlayerRed ? " (红方)" : " (黑方)"));
        opponentPlayerLabel.setText(opponentPlayerName + (!isLocalPlayerRed ? " (红方)" : " (黑方)"));
    }

    public void updateBoard(String[][] newBoard) {
        this.currentBoardModel = newBoard;
        chessBoardPanel.updateBoardPieces(currentBoardModel, isLocalPlayerRed);
    }

    public void updateTurnStatus(boolean isItMyGameTurn) {
        if (isItMyGameTurn) {
            turnIndicatorLabel.setText("轮到您 (" + (this.isLocalPlayerRed ? "红方" : "黑方") + ") 行棋");
            turnIndicatorLabel.setForeground(new Color(0, 100, 0));
            requestDrawButton.setEnabled(true);
            // requestUndoButton is enabled when it's NOT your turn (after you've moved)
            requestUndoButton.setEnabled(false);
        } else {
            turnIndicatorLabel.setText("等待对方 (" + (this.isLocalPlayerRed ? "黑方" : "红方") + ") 行棋");
            turnIndicatorLabel.setForeground(Color.RED.darker());
            requestDrawButton.setEnabled(false); // Cannot request draw when not your turn
            requestUndoButton.setEnabled(true); // Can request undo after your move
        }
    }

    public void setOpponentName(String name) {
        if (name != null && !name.isEmpty()) {
            this.opponentPlayerName = name;
            // 在initializeGame后更新标签
            opponentPlayerLabel.setText(opponentPlayerName + (!isLocalPlayerRed ? " (红方)" : " (黑方)"));
        }
    }

    public void appendChatMessage(String message) {
        chatDisplayArea.append(message + "\n");
        chatDisplayArea.setCaretPosition(chatDisplayArea.getDocument().getLength()); // 自动滚动到底部
    }

    public void displayMessage(String message) { // 用于显示系统消息或服务器的通用消息
        appendChatMessage("[系统]: " + message);
    }

    public void promptGameModeSelection() {
        // 简单的模式选择对话框
        String[] modes = {"普通模式", "快速模式"};
        String selectedMode = (String) JOptionPane.showInputDialog(
                this,
                "请选择游戏模式:",
                "模式选择",
                JOptionPane.PLAIN_MESSAGE,
                null,
                modes,
                modes[0]
        );

        if (selectedMode != null) {
            if (selectedMode.equals("普通模式")) {
                client.sendMessageToServer(new Info(Process.normal_game, "选择普通模式"));
                appendChatMessage("已选择普通模式，正在等待对手...");
            } else {
                client.sendMessageToServer(new Info(Process.short_game, "选择快速模式"));
                appendChatMessage("已选择快速模式，正在等待对手...");
            }
        } else {
            appendChatMessage("未选择模式，请选择一个模式开始游戏。");
            promptGameModeSelection(); // 必须选择一个
        }
    }


    public void showEndGameDialog(String title, String message) {
        SwingUtilities.invokeLater(() -> {
            // JOptionPane.showMessageDialog(this, message, "游戏结束 - " + title.toUpperCase(), JOptionPane.INFORMATION_MESSAGE);
            //surrenderButton.setEnabled(false);
            requestDrawButton.setEnabled(false);
            requestUndoButton.setEnabled(false);
            turnIndicatorLabel.setText("游戏结束 - " + title.toUpperCase());
            turnIndicatorLabel.setForeground(Color.BLUE);

            Object[] options = {"再来一局", "直接退出"};
            int choice = JOptionPane.showOptionDialog(this,
                    message + "\n您想做什么？",
                    "游戏结束 - " + title.toUpperCase(),
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.INFORMATION_MESSAGE,
                    null,
                    options,
                    options[0]);

            if (choice == JOptionPane.YES_OPTION) { // 再来一局
                appendChatMessage("[系统]: 准备开始新的一局...");
                resetForNewGame();
                promptGameModeSelection();
            } else { // 直接退出或关闭对话框
                client.sendMessageToServer(new Info(Process.unconnecting, "客户端退出")); // 通知服务器
                System.exit(0);
            }
        });
    }

    private void resetForNewGame() {
        // 清理棋盘模型 (可选，因为服务器会发送新的)
        // this.currentBoardModel = null;
        // chessBoardPanel.updateBoardState(null, true); // 清空棋盘显示

        // 重置UI元素状态
        chatDisplayArea.setText(""); // 清空聊天记录
        turnIndicatorLabel.setText("等待游戏开始...");
        opponentPlayerLabel.setText("对方"); // 重置对手名称
        // 按钮状态会在initializeGame中根据回合重新设置
        //surrenderButton.setEnabled(false);
        requestDrawButton.setEnabled(false);
        requestUndoButton.setEnabled(false);
        clockPanel.stop(); // 停止并重置计时器
        clockPanel.reinitializeTimers(0,0); // 清零显示
        appendChatMessage("[系统]: 请选择新的游戏模式。");
    }

    public void showConnectionError(String message) {
        JOptionPane.showMessageDialog(this, message, "连接错误", JOptionPane.ERROR_MESSAGE);
        // 游戏过程中断线，禁用所有游戏操作按钮
        surrenderButton.setEnabled(false);
        requestDrawButton.setEnabled(false);
        requestUndoButton.setEnabled(false);
        turnIndicatorLabel.setText("连接已断开");
        turnIndicatorLabel.setForeground(Color.RED);
        clockPanel.stop();
    }

    // --- 计时器控制 ---
    public void setTimers(int gameTotalSeconds, int turnTotalSeconds) {
        clockPanel.reinitializeTimers(gameTotalSeconds, turnTotalSeconds);
    }
    public void startGameTimers() {
        clockPanel.resume();
    }
    public void stopTimers() {
        clockPanel.stop();
    }
    public void resetMoveTimer() {
        clockPanel.resetTurn();
        clockPanel.resume(); // 确保在自己回合计时器是运行的
    }
    public void pauseMoveTimer(){
        clockPanel.pause(); // 对方回合，自己的步时计时器应该暂停，但总局时继续
        //ClockPanel的逻辑是两个都倒计时，可能需要调整ClockPanel或在这里只重置对方的
    }


    // --- 内部棋盘面板类 ---
    class ChessBoardPanel extends JPanel {
        private ImageIcon boardImage;
        private String[][] boardState; // board[row][col]
        private boolean playerIsRedPerspective; // 当前是否以红方视角显示 (红在下)
        private ImageIcon selectedPieceMarker; // 选中棋子的标记 (例如一个框)

        public ChessBoardPanel() {
            this.boardImage = new ImageIcon(Objects.requireNonNull(getClass().getResource("./img/board.png")));
            this.selectedPieceMarker = new ImageIcon(Objects.requireNonNull(getClass().getResource("./img/selected_frame.png"))); // 假设有一个选中框图片
            setOpaque(false);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (!client.isMyTurn() || currentBoardModel == null) return; // 不是自己的回合或棋盘未初始化

                    // 将屏幕点击坐标转换为棋盘逻辑行列 (0-8列, 0-9行)
                    // 注意：这里的转换需要非常小心，确保与棋子绘制逻辑一致
                    int clickedCol = (e.getX() - (PIECE_WIDTH/2 -10)) / (CELL_SIZE-4); // 估算，需要精确调整
                    int clickedRow = (e.getY() - (PIECE_HEIGHT/2-10)) / CELL_SIZE;

                    // 根据视角调整逻辑行列
                    int logicalRow, logicalCol;
                    if (playerIsRedPerspective) {
                        logicalRow = clickedRow;
                        logicalCol = clickedCol;
                    } else { // 黑方视角，屏幕坐标需要翻转
                        logicalRow = LOGICAL_ROWS - 1 - clickedRow;
                        logicalCol = LOGICAL_COLS - 1 - clickedCol;
                    }

                    // 边界检查
                    if (logicalRow < 0 || logicalRow >= LOGICAL_ROWS || logicalCol < 0 || logicalCol >= LOGICAL_COLS) {
                        selectedPieceRow = -1; // 点击棋盘外，取消选择
                        selectedPieceCol = -1;
                        repaint();
                        return;
                    }

                    System.out.println("Clicked screen: ("+e.getX()+","+e.getY()+") -> Approx grid: ("+clickedRow+","+clickedCol+") -> Logical: ("+logicalRow+","+logicalCol+")");


                    String pieceAtClick = boardState[logicalRow][logicalCol];

                    if (selectedPieceRow == -1) { // 还没有选中棋子
                        if (!pieceAtClick.equals("---")) { // 点击的位置有棋子
                            boolean pieceIsRed = pieceAtClick.startsWith("R_");
                            if (playerIsRedPerspective == pieceIsRed) { // 是自己的棋子
                                selectedPieceRow = logicalRow;
                                selectedPieceCol = logicalCol;
                                System.out.println("Selected piece: " + pieceAtClick + " at (" + selectedPieceRow + "," + selectedPieceCol + ")");
                                repaint(); // 重绘以显示选中标记
                            }
                        }
                    } else { // 已经选中了一个棋子，现在是选择目标位置
                        System.out.println("Attempting move from (" + selectedPieceRow + "," + selectedPieceCol + ") to (" + logicalRow + "," + logicalCol + ")");
                        //将步骤是否合法的判定改为client执行，避免不合法步骤影响计时为0的惩罚
                        GameLogic gameLogic = new GameLogic(boardState, playerIsRedPerspective);
                        boolean moveValid = gameLogic.isValidMove(selectedPieceRow, selectedPieceCol, logicalRow, logicalCol, playerIsRedPerspective);

                        if(moveValid) {
                            client.sendMessageToServer(new Info(Process.gaming, selectedPieceRow, selectedPieceCol, logicalRow, logicalCol, null));
                        } else {
                            appendChatMessage("不合法的走棋！");
                        }
                        selectedPieceRow = -1; // 重置选择
                        selectedPieceCol = -1;
                        // repaint(); // 服务器响应后会更新棋盘并重绘
                    }
                }
            });
        }

        public void updateBoardPieces(String[][] newBoardModel, boolean isRedPerspective) {
            this.boardState = newBoardModel;
            this.playerIsRedPerspective = isRedPerspective;
            selectedPieceCol = -1; // 清除选择
            selectedPieceRow = -1;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (boardImage != null) {
                g.drawImage(boardImage.getImage(), 0, 0, getWidth(), getHeight(), this);
            }

            if (boardState == null) return;

            // 绘制棋子
            for (int r = 0; r < LOGICAL_ROWS; r++) {
                for (int c = 0; c < LOGICAL_COLS; c++) {
                    String pieceStr = boardState[r][c];
                    if (!pieceStr.equals("---")) {
                        // 计算棋子在屏幕上的绘制坐标 (sx, sy)
                        int screenCol, screenRow;
                        if (playerIsRedPerspective) { // 红方在下
                            screenCol = c;
                            screenRow = r;
                        } else { // 黑方在下 (棋盘逻辑坐标需要翻转来对应屏幕)
                            screenCol = LOGICAL_COLS - 1 - c;
                            screenRow = LOGICAL_ROWS - 1 - r;
                        }

                        // 将棋盘格子行列转换为像素坐标的左上角
                        // 这些偏移量需要根据棋盘背景图精确测量
                        int sx = screenCol * (CELL_SIZE-5) + (CELL_SIZE - PIECE_WIDTH) / 2 + 27; // 额外偏移
                        int sy = screenRow * (CELL_SIZE) + (CELL_SIZE - PIECE_HEIGHT) / 2 + 10; // 额外偏移

                        ImageIcon pieceIcon = getPieceIcon(pieceStr);
                        if (pieceIcon != null) {
                            g.drawImage(pieceIcon.getImage(), sx, sy, PIECE_WIDTH, PIECE_HEIGHT, this);
                        }

                        // 如果是选中的棋子，绘制标记
                        if (r == selectedPieceRow && c == selectedPieceCol && selectedPieceMarker != null) {
                            g.drawImage(selectedPieceMarker.getImage(), sx -5 , sy -5 , PIECE_WIDTH + 10, PIECE_HEIGHT + 10, this); // 选中框比棋子稍大
                        }
                    }
                }
            }
        }

        private ImageIcon getPieceIcon(String pieceStr) {
            // 红方: R_G (帅), R_A (仕), R_E (相), R_H (馬), R_R (車), R_C (炮), R_P (兵)
            // 黑方: B_G (将), B_A (士), B_E (象), B_H (馬), B_R (車), B_C (炮), B_P (卒)
            String fileName = "";
            char color = pieceStr.charAt(0); // 'R' or 'B'
            char type = pieceStr.charAt(2);  // 'G', 'A', etc.

            fileName += (color == 'R' ? "r" : "b");

            switch (type) {
                case 'G': fileName += "b"; break; // 帅/将 (用 's' 代表 king/boss)
                case 'A': fileName += "s"; break; // 仕/士
                case 'E': fileName += "x"; break; // 相/象
                case 'H': fileName += "m"; break; // 马
                case 'R': fileName += "j"; break; // 车
                case 'C': fileName += "p"; break; // 炮
                case 'P': fileName += "z"; break; // 兵/卒
                default: return null;
            }
            fileName += ".png";
            // System.out.println("Loading piece image: /img/" + fileName);
            return new ImageIcon(Objects.requireNonNull(getClass().getResource("./img/" + fileName)));
        }
    }
    // 用于GameView.java (ClockPanel的reinitializeTimers)
    public void reinitializeClockPanel(int gameTotalSeconds, int turnTotalSeconds) {
        if (clockPanel != null) {
            clockPanel.reinitializeTimers(gameTotalSeconds, turnTotalSeconds);
        }
    }
}
