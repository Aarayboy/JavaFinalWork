package Game;

import Game.Client.Process; // 从Client包导入Process枚举

/**
 * GameLogic 类负责处理中国象棋的游戏规则、走法验证和胜负判断。
 */
public class GameLogic {
    private GameBoard gameBoard;
    private boolean isRedTurn; // true 表示轮到红方，false 表示轮到黑方

    public GameLogic() {
        this.gameBoard = new GameBoard();
        this.isRedTurn = true; // 通常红方先行
    }

    public GameLogic(GameBoard board, boolean RedTurn) {
        this.gameBoard = board;
        this.isRedTurn = RedTurn;
    }

    public GameLogic(String[][] boardState, boolean playerIsRedPerspective) {
        this.gameBoard = new GameBoard(boardState);
        this.isRedTurn = playerIsRedPerspective;
    }

    public GameBoard getGameBoard() {
        return gameBoard;
    }

    public boolean isRedTurn() {
        return isRedTurn;
    }

    public void switchTurn() {
        isRedTurn = !isRedTurn;
    }

    /**
     * 检查选中的棋子是否属于当前行棋方。
     * @param r 行
     * @param c 列
     * @return boolean
     */
    private boolean isCurrentPlayerPiece(int r, int c) {
        String piece = gameBoard.getPieceAt(r, c);
        if (piece == null || piece.equals("---")) return false;
        return (isRedTurn && piece.startsWith("R_")) || (!isRedTurn && piece.startsWith("B_"));
    }

    /**
     * 验证一步棋是否合法。
     * @param or 起始行
     * @param oc 起始列
     * @param nr 目标行
     * @param nc 目标列
     * @param playerIsRed 当前玩家是否为红方 (用于判断棋子归属)
     * @return boolean 是否为有效移动
     */
    public boolean isValidMove(int or, int oc, int nr, int nc, boolean playerIsRed) {
        // 基础检查
        if (or < 0 || or >= GameBoard.ROWS || oc < 0 || oc >= GameBoard.COLS ||
            nr < 0 || nr >= GameBoard.ROWS || nc < 0 || nc >= GameBoard.COLS) {
            System.out.println("逻辑: 目标超出棋盘范围。");
            return false; // 超出棋盘范围
        }

        String piece = gameBoard.getPieceAt(or, oc);
        if (piece == null || piece.equals("---")) {
            System.out.println("逻辑: 起始位置没有棋子。");
            return false; // 起始位置没有棋子
        }

        // 检查是否是当前玩家的棋子
        if ((playerIsRed && !piece.startsWith("R_")) || (!playerIsRed && !piece.startsWith("B_"))) {
            System.out.println("逻辑: 不是你的棋子。");
            return false;
        }

        String targetPiece = gameBoard.getPieceAt(nr, nc);
        // 目标位置不能是己方棋子
        if (targetPiece != null && !targetPiece.equals("---")) {
            if ((playerIsRed && targetPiece.startsWith("R_")) || (!playerIsRed && targetPiece.startsWith("B_"))) {
                System.out.println("逻辑: 不能吃自己的棋子。");
                return false;
            }
        }
        
        // 具体棋子走法规则 (简化版，需要大量扩充)
        char pieceType = piece.charAt(2); // R_G -> G

        switch (pieceType) {
            case 'G': // 将/帅 (General)
                return isValidGeneralMove(or, oc, nr, nc, playerIsRed);
            case 'A': // 士/仕 (Advisor)
                return isValidAdvisorMove(or, oc, nr, nc, playerIsRed);
            case 'E': // 象/相 (Elephant)
                return isValidElephantMove(or, oc, nr, nc, playerIsRed);
            case 'H': // 马 (Horse)
                return isValidHorseMove(or, oc, nr, nc);
            case 'R': // 车 (Rook)
                return isValidRookMove(or, oc, nr, nc);
            case 'C': // 炮 (Cannon)
                return isValidCannonMove(or, oc, nr, nc);
            case 'P': // 兵/卒 (Pawn)
                return isValidPawnMove(or, oc, nr, nc, playerIsRed);
            default:
                return false; // 未知棋子类型
        }
    }

    // --- 具体棋子移动规则的辅助方法 (示例) ---
    private boolean isInPalace(int r, int c, boolean isRed) {
        if (c < 3 || c > 5) return false; // 列必须在 3, 4, 5
        if (isRed) return r >= 7 && r <= 9; // 红方九宫格
        return r >= 0 && r <= 2; // 黑方九宫格
    }

    private boolean isValidGeneralMove(int or, int oc, int nr, int nc, boolean isRed) {
        if (!isInPalace(nr, nc, isRed)) return false; // 目标必须在九宫内
        int dr = Math.abs(nr - or);
        int dc = Math.abs(nc - oc);
        if (!((dr == 1 && dc == 0) || (dr == 0 && dc == 1))) return false; // 只能走一格直线

        // "王不见王"规则 (飞将)
        String opponentGeneral = isRed ? "B_G" : "R_G";
        if (oc == nc) { // 在同一列
            boolean clearPath = true;
            for (int r = Math.min(or, nr) + 1; r < Math.max(or, nr); r++) {
                if (!gameBoard.getPieceAt(r, oc).equals("---")) {
                    // 检查这条路径上是否有其他棋子，如果有，则不是直接对将
                    // 但如果这条路径上遇到的第一个非空子就是对方的将，那也是允许的（被吃）
                    // 这里的逻辑是：如果两将之间有子，则不能飞。
                    // 如果两将之间无子，则可以飞（即吃掉对方的将，如果对方的将正好在路径上）
                    // 但通常飞将是直接移动到对方将的位置，如果中间无子。
                    // 简单起见，如果目标是对方的将且在同列无阻挡，则允许。
                }
            }
            // 简化：如果目标是对方将军，并且在同一直线上且中间没有棋子，则允许。
            // 实际的飞将规则是：如果两将在同一直线且中间无子，当前走棋一方的将可以“飞”过去吃掉对方的将。
            // 这里的isValidMove是判断一个普通的移动是否合法，而不是将军是否能“飞”。
            // 将军的普通移动不能直接导致“王不见王”的局面（即移动后，两将在同一直线且中间无子）。
            // 这个检查应该在尝试移动后，判断是否会导致己方将军被对方将军“照面”。
            // 暂时简化，只判断基本移动。
        }
        return true;
    }

    private boolean isValidAdvisorMove(int or, int oc, int nr, int nc, boolean isRed) {
        if (!isInPalace(nr, nc, isRed)) return false; // 必须在九宫内
        int dr = Math.abs(nr - or);
        int dc = Math.abs(nc - oc);
        return dr == 1 && dc == 1; // 只能斜走一格
    }

    private boolean isValidElephantMove(int or, int oc, int nr, int nc, boolean isRed) {
        // 不能过河
        if (isRed && nr < 5) return false;
        if (!isRed && nr > 4) return false;

        int dr = Math.abs(nr - or);
        int dc = Math.abs(nc - oc);
        if (!(dr == 2 && dc == 2)) return false; // 必须走田字

        // 象眼不能有子
        int block_r = or + (nr - or) / 2;
        int block_c = oc + (nc - oc) / 2;
        if (!gameBoard.getPieceAt(block_r, block_c).equals("---")) return false;

        return true;
    }
    
    private boolean isValidHorseMove(int or, int oc, int nr, int nc) {
        int dr = Math.abs(nr - or);
        int dc = Math.abs(nc - oc);
        if (!((dr == 1 && dc == 2) || (dr == 2 && dc == 1))) return false; // 日字

        // 别马腿
        if (dr == 2) { // 直走两步的方向
            if (!gameBoard.getPieceAt(or + (nr - or) / 2, oc).equals("---")) return false;
        } else { // dc == 2, 横走两步的方向
            if (!gameBoard.getPieceAt(or, oc + (nc - oc) / 2).equals("---")) return false;
        }
        return true;
    }

    private boolean isValidRookMove(int or, int oc, int nr, int nc) {
        if (or != nr && oc != nc) return false; // 必须走直线

        // 检查路径上是否有子
        if (or == nr) { // 横向移动
            for (int c = Math.min(oc, nc) + 1; c < Math.max(oc, nc); c++) {
                if (!gameBoard.getPieceAt(or, c).equals("---")) return false;
            }
        } else { // 纵向移动
            for (int r = Math.min(or, nr) + 1; r < Math.max(or, nr); r++) {
                if (!gameBoard.getPieceAt(r, oc).equals("---")) return false;
            }
        }
        return true;
    }
    
    private boolean isValidCannonMove(int or, int oc, int nr, int nc) {
        if (or != nr && oc != nc) return false; // 必须走直线

        String targetPiece = gameBoard.getPieceAt(nr, nc);
        int piecesInPath = 0;
        if (or == nr) { // 横向
            for (int c = Math.min(oc, nc) + 1; c < Math.max(oc, nc); c++) {
                if (!gameBoard.getPieceAt(or, c).equals("---")) piecesInPath++;
            }
        } else { // 纵向
            for (int r = Math.min(or, nr) + 1; r < Math.max(or, nr); r++) {
                if (!gameBoard.getPieceAt(r, oc).equals("---")) piecesInPath++;
            }
        }

        if (targetPiece.equals("---")) { // 移动
            return piecesInPath == 0;
        } else { // 吃子
            return piecesInPath == 1;
        }
    }

    private boolean isValidPawnMove(int or, int oc, int nr, int nc, boolean isRed) {
        int dr = nr - or; // 红兵向上是负，黑卒向下是正
        int dc = Math.abs(nc - oc);

        if (isRed) { // 红兵
            if (dr > 0) return false; // 红兵不能后退
            if (or >= 5) { // 在己方阵地，未过河
                return dr == -1 && dc == 0; // 只能向前一步
            } else { // 已过河
                return (dr == -1 && dc == 0) || (dr == 0 && dc == 1); // 可前可横
            }
        } else { // 黑卒
            if (dr < 0) return false; // 黑卒不能后退
            if (or <= 4) { // 在己方阵地，未过河
                return dr == 1 && dc == 0; // 只能向前一步
            } else { // 已过河
                return (dr == 1 && dc == 0) || (dr == 0 && dc == 1); // 可前可横
            }
        }
    }


    /**
     * 处理走棋请求。如果有效，则更新棋盘并切换回合。
     * @param or 起始行
     * @param oc 起始列
     * @param nr 目标行
     * @param nc 目标列
     * @param playerIsRed 当前玩家是否为红方
     * @return boolean 移动是否成功执行
     */
    public boolean processMove(int or, int oc, int nr, int nc, boolean playerIsRed) {
        if (isValidMove(or, oc, nr, nc, playerIsRed)) {
            gameBoard.makeMove(or, oc, nr, nc);
            // 切换回合的逻辑应该在 GameRoom 中，在确认移动成功后
            // switchTurn(); // 不在这里切换，由 GameRoom 控制
            return true;
        }
        return false;
    }

    /**
     * 检查胜负条件。
     * @return Process 游戏状态 (win, lose, draw, gaming)
     * 这里返回的 win/lose 是相对于当前行棋方的。
     * 例如，如果红方行棋，然后黑将被吃，则返回 Process.win。
     */
    public Process checkWinLossCondition() {
        boolean redGeneralFound = false;
        boolean blackGeneralFound = false;
        String[][] currentBoard = gameBoard.getBoardState();

        for (int r = 0; r < GameBoard.ROWS; r++) {
            for (int c = 0; c < GameBoard.COLS; c++) {
                if ("R_G".equals(currentBoard[r][c])) redGeneralFound = true;
                if ("B_G".equals(currentBoard[r][c])) blackGeneralFound = true;
            }
        }

        if (!redGeneralFound) { // 红帅被吃
            return isRedTurn ? Process.lose : Process.win; // 如果轮到红方但红帅没了(不可能)，或者轮到黑方且红帅没了
        }
        if (!blackGeneralFound) { // 黑将被吃
            return isRedTurn ? Process.win : Process.lose; // 如果轮到红方且黑将没了，红方赢
        }

        // TODO: 实现更复杂的胜负判断，如困毙、长将、长捉等。
        // TODO: 判断是否将死 (checkmate) - 对方无路可走且被将军
        // TODO: 判断是否困毙 (stalemate) - 对方无路可走但未被将军

        return Process.gaming; // 游戏继续
    }

    // 辅助方法：检查一方是否被将军 (isKingInCheck)
    public boolean isKingInCheck(boolean isRedKing) {
        int kingR = -1, kingC = -1;
        String kingPiece = isRedKing ? "R_G" : "B_G";
        String[][] currentBoard = gameBoard.getBoardState();

        // 找到王的位置
        for (int r = 0; r < GameBoard.ROWS; r++) {
            for (int c = 0; c < GameBoard.COLS; c++) {
                if (kingPiece.equals(currentBoard[r][c])) {
                    kingR = r;
                    kingC = c;
                    break;
                }
            }
            if (kingR != -1) break;
        }

        if (kingR == -1) return true; // 王都没了，肯定是输了，也算被"将军"的一种极端情况

        // 遍历对方所有棋子，看是否能攻击到王
        for (int r = 0; r < GameBoard.ROWS; r++) {
            for (int c = 0; c < GameBoard.COLS; c++) {
                String piece = currentBoard[r][c];
                if (piece.equals("---")) continue;

                boolean pieceIsRed = piece.startsWith("R_");
                if (pieceIsRed == isRedKing) continue; // 是己方棋子，跳过

                // 检查这个对方棋子 (r,c) 是否能攻击到 (kingR, kingC)
                // 注意：调用isValidMove时，第四个参数 playerIsRed 应该是 pieceIsRed
                if (isValidMove(r, c, kingR, kingC, pieceIsRed)) {
                    // System.out.println((isRedKing ? "红" : "黑") + "王被将军 by " + piece + " at (" + r + "," + c + ")");
                    return true;
                }
            }
        }
        return false;
    }


    public void setTurn(boolean isRedTurn) {
        this.isRedTurn = isRedTurn;
    }
}