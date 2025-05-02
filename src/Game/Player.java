package Game;


public class Player {
    int remain;
    boolean red;
    String[][] board = new String[9][10];

    Player(int remain,boolean red) {
        this.remain = remain;
        this.red = red;

        for(int i=0;i<9;i++)
            for(int j=0;j<10;j++)
                board[i][j]="";

        board[0][0]="车";board[1][0]="马";board[2][0]="相";
        board[3][0]="士";board[4][0]="帅";board[5][0]="士";
        board[6][0]="相";board[7][0]="马";board[8][0]="车";

        board[0][9]="车";board[1][9]="马";board[2][9]="相";
        board[3][9]="士";board[4][9]="帅";board[5][9]="士";
        board[6][9]="相";board[7][9]="马";board[8][9]="车";

        board[0][3]=board[2][3]=board[4][3]=board[6][3]=board[8][3]="兵";
        board[0][6]=board[2][6]=board[4][6]=board[6][6]=board[8][6]="卒";

        board[1][2]=board[7][2]=board[1][7]=board[7][7]="炮";
    }


}
