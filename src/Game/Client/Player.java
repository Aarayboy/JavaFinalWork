package Game.Client;


public class Player {
    int remain_all,remain;

    boolean red;
    String[][] board = new String[9][10];
    Piece[] pieces = new Piece[16];

    Player(int remain_all,int remain,boolean red) {
        this.remain_all = remain_all;
        this.remain = remain;
        this.red = red;

        for(int i=0;i<9;i++)
            for(int j=0;j<10;j++)
                board[i][j]="";

        board[0][0]="车";board[1][0]="马";board[2][0]="相";
        board[3][0]="士";board[4][0]="帅";board[5][0]="士";
        board[6][0]="相";board[7][0]="马";board[8][0]="车";

        board[0][9]="车";board[1][9]="马";board[2][9]="相";
        board[3][9]="士";board[4][9]="将";board[5][9]="士";
        board[6][9]="相";board[7][9]="马";board[8][9]="车";

        board[0][3]=board[2][3]=board[4][3]=board[6][3]=board[8][3]="兵";
        board[0][6]=board[2][6]=board[4][6]=board[6][6]=board[8][6]="卒";

        board[1][2]=board[7][2]=board[1][7]=board[7][7]="炮";

        pieces[0]=new Car(0,0,this.red);
        pieces[1]=new Horse(1,0,this.red);
        pieces[2]=new Minister(2,0,this.red);
        pieces[3]=new Mandarin(3,0,this.red);
        pieces[4]=new King(4,0,this.red);
        pieces[5]=new Car(8,0,this.red);
        pieces[6]=new Horse(7,0,this.red);
        pieces[7]=new Minister(6,0,this.red);
        pieces[8]=new Mandarin(5,0,this.red);
        pieces[9]=new Solider(0,3,this.red);
        pieces[10]=new Solider(2,3,this.red);
        pieces[11]=new Solider(4,3,this.red);
        pieces[12]=new Solider(6,3,this.red);
        pieces[13]=new Solider(8,3,this.red);
        pieces[14]=new Cannon(1,2,this.red);
        pieces[15]=new Cannon(7,2,this.red);
    }


    //己方棋子从(ox,oy)移动到(nx,ny)是否可行
    boolean movable(int ox,int oy,int nx,int ny){
        for(Piece piece : pieces){
            //(nx,ny)存在己方棋子
            if(piece.x == nx && piece.y == ny){
                return false;
            }
        }

        for(Piece piece : pieces){
            //找到要移动的棋子
            if(piece.x == ox && piece.y == ny){
                //判断移动是否可行
                return piece.movable(nx, ny, this.board);
            }
        }

        //(ox,oy)处不存在己方棋子
        return false;
    }

    //根据服务器信息更新棋盘
    void update_board(String[][] new_board){
        for(int i=0;i<9;i++)
            for(int j=0;j<10;j++)
                this.board[i][j] = new_board[i][j];
    }

    //发送信息
    void send(){

    }
}
