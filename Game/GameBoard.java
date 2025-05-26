package Game;

import Game.Server.GameRoom;

import java.util.logging.Logger;

/**
 * GameBoard 类负责管理中国象棋的棋盘状态。
 * 棋盘大小为 10行 x 9列。
 * 行 (rank): 0-9
 * 列 (file): 0-8
 * 红方棋子通常在行索引较大的区域 (例如 5-9)，黑方在行索引较小的区域 (例如 0-4)。
 */
public class GameBoard {
    private static final Logger LOGGER = Logger.getLogger(GameBoard.class.getName());

    private String[][] board; // 存储棋子标识符, board[row][column]

    // 棋子标识符示例 (与 Readme 一致)
    // 红方: R_G (帅), R_A (仕), R_E (相), R_H (馬), R_R (車), R_C (炮), R_P (兵)
    // 黑方: B_G (将), B_A (士), B_E (象), B_H (馬), B_R (車), B_C (炮), B_P (卒)
    // 空位: "---"

    public static final int ROWS = 10;
    public static final int COLS = 9;

    public GameBoard() {
        board = new String[ROWS][COLS];
        initializeBoard();
    }

    public GameBoard(String[][] boardState) {
        board = boardState;
    }

    /**
     * 初始化棋盘到标准开局状态。
     */
    public void initializeBoard() {
        // 清空棋盘
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                board[r][c] = "---"; // 空位
            }
        }

        // 放置黑方棋子 (上方, 小行索引)
        board[0][0] = "B_R"; board[0][1] = "B_H"; board[0][2] = "B_E"; board[0][3] = "B_A";
        board[0][4] = "B_G"; board[0][5] = "B_A"; board[0][6] = "B_E"; board[0][7] = "B_H";
        board[0][8] = "B_R";
        board[2][1] = "B_C"; board[2][7] = "B_C";
        board[3][0] = "B_P"; board[3][2] = "B_P"; board[3][4] = "B_P"; board[3][6] = "B_P";
        board[3][8] = "B_P";

        // 放置红方棋子 (下方, 大行索引)
        board[9][0] = "R_R"; board[9][1] = "R_H"; board[9][2] = "R_E"; board[9][3] = "R_A";
        board[9][4] = "R_G"; board[9][5] = "R_A"; board[9][6] = "R_E"; board[9][7] = "R_H";
        board[9][8] = "R_R";
        board[7][1] = "R_C"; board[7][7] = "R_C";
        board[6][0] = "R_P"; board[6][2] = "R_P"; board[6][4] = "R_P"; board[6][6] = "R_P";
        board[6][8] = "R_P";
    }

    /**
     * 获取当前棋盘状态的副本。
     * @return String[][] 棋盘状态的二维数组副本。
     */
    public String[][] getBoardState() {
        String[][] copy = new String[ROWS][COLS];
        for (int i = 0; i < ROWS; i++) {
            System.arraycopy(board[i], 0, copy[i], 0, COLS);
        }
        return copy;
    }

    /**
     * 获取指定位置的棋子。
     * @param r 行索引 (0-9)
     * @param c 列索引 (0-8)
     * @return String 棋子标识符，如果越界或空位则为 "---" 或 null (具体取决于实现)
     */
    public String getPieceAt(int r, int c) {
        if (r < 0 || r >= ROWS || c < 0 || c >= COLS) {
            // System.err.println("GameBoard.getPieceAt: Out of bounds (" + r + "," + c + ")");
            return null; // 或者抛出异常
        }
        return board[r][c];
    }

    /**
     * 在棋盘上移动棋子。
     * 此方法不进行移动规则验证，验证应在 GameLogic 中完成。
     * @param or 起始行
     * @param oc 起始列
     * @param nr 目标行
     * @param nc 目标列
     */
    public void makeMove(int or, int oc, int nr, int nc) {
        if (or < 0 || or >= ROWS || oc < 0 || oc >= COLS || nr < 0 || nr >= ROWS || nc < 0 || nc >= COLS) {
            System.err.println("GameBoard.makeMove: Out of bounds move attempt.");
            return;
        }
        board[nr][nc] = board[or][oc];
        board[or][oc] = "---"; // 原位置变为空
    }

    /**
     * 在控制台打印棋盘，用于调试。
     * @param perspectiveRed 如果为true，红方在下方；否则黑方在下方。
     */
    public void printBoard(boolean perspectiveRed) {
        System.out.println("  0  1  2  3  4  5  6  7  8  (列)");
        System.out.println(" +----------------------------+");
        if (perspectiveRed) { // 红方视角 (红在下)
            for (int r = 0; r < ROWS; r++) {
                System.out.print(r + "|");
                for (int c = 0; c < COLS; c++) {
                    System.out.printf("%-3s", board[r][c]); // 左对齐，宽度3
                }
                System.out.println("|" + r);
                if (r == 4) {
                    System.out.println(" |---------楚 河 汉 界---------|");
                }
            }
        } else { // 黑方视角 (黑在下, 即棋盘上下翻转显示)
             for (int r = ROWS - 1; r >= 0; r--) {
                System.out.print(r + "|");
                for (int c = 0; c < COLS; c++) { // 列的顺序不变，或者也可以翻转 c = COLS - 1; c >=0
                    System.out.printf("%-3s", board[r][c]);
                }
                System.out.println("|" + r);
                if (r == 5) { // 注意楚河汉界的位置也相对调整
                    System.out.println(" |---------楚 河 汉 界---------|");
                }
            }
        }
        System.out.println(" +----------------------------+");
        System.out.println("  0  1  2  3  4  5  6  7  8  (列)");
    }

    public void setBoardState(String[][] newState) {
        if (newState.length == ROWS && newState[0].length == COLS) {
            for (int i = 0; i < ROWS; i++) {
                System.arraycopy(newState[i], 0, this.board[i], 0, COLS);
            }
        } else {
            LOGGER.info("setBoardState: newState dimensions do not match.");
        }
    }
}