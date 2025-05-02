package Game;


public class Board {
    String[][] board = new String[9][10];
    Piece[] pieces = new Piece[32];

    {
        //red
        pieces[0]=new Car(0,0,true);
        pieces[1]=new Horse(1,0,true);
        pieces[2]=new Minister(2,0,true);
        pieces[3]=new Mandarin(3,0,true);
        pieces[4]=new King(4,0,true);
        pieces[5]=new Car(8,0,true);
        pieces[6]=new Horse(7,0,true);
        pieces[7]=new Minister(6,0,true);
        pieces[8]=new Mandarin(5,0,true);
        pieces[9]=new Solider(0,3,true);
        pieces[10]=new Solider(2,3,true);
        pieces[11]=new Solider(4,3,true);
        pieces[12]=new Solider(6,3,true);
        pieces[13]=new Solider(8,3,true);
        pieces[14]=new Cannon(1,2,true);
        pieces[15]=new Cannon(7,2,true);

        //black
        pieces[16]=new Car(0,9,false);
        pieces[17]=new Horse(1,9,false);
        pieces[18]=new Minister(2,9,false);
        pieces[19]=new Mandarin(3,9,false);
        pieces[20]=new King(4,9,false);
        pieces[21]=new Car(8,9,false);
        pieces[22]=new Horse(7,9,false);
        pieces[23]=new Minister(6,9,false);
        pieces[24]=new Mandarin(5,9,false);
        pieces[25]=new Solider(0,6,false);
        pieces[26]=new Solider(2,6,false);
        pieces[27]=new Solider(4,6,false);
        pieces[28]=new Solider(6,6,false);
        pieces[29]=new Solider(8,6,false);
        pieces[30]=new Cannon(1,7,false);
        pieces[31]=new Cannon(7,7,false);
    }

    Board(){
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

    }

    //将位于（ox，oy）的棋子移动到（nx,ny）
    void move(int ox,int oy,int nx,int ny){
        for (Piece piece : pieces) {
            if(piece.x==ox && piece.y == oy){
                if(piece.move(nx,ny,this.board)) {
                    //若新位置有棋子,则被吃
                    if(board[nx][ny] != "") {
                        for(Piece ppiece : pieces){
                            if(ppiece.x == nx && ppiece.y == ny){
                                ppiece.alive = false;
                            }
                        }
                    }

                    board[nx][ny] = board[ox][oy];
                    board[ox][ox] = "";
                }
            }
        }
    }

}
