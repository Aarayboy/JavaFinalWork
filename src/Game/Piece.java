package Game;

import static java.lang.Math.*;

abstract public class Piece{
    String name = null;
    int x,y;
    boolean red,alive;

    Piece(String name,int x,int y,boolean red){
        this.name=name;
        this.x=x;
        this.y=y;
        this.red=red;
        this.alive = true;
    }
    
    abstract boolean movale(int x,int y,String[][] board);
    
    //修改该棋子的坐标
    void set_loc(int x,int y){
        this.x=x;
        this.y=y;
    }
    
    //检查坐标是否在棋盘内，以及该棋子是否存活
    boolean inboard(int x,int y){
        if(x<0 || x>8 || y<0 || y>9 || !this.alive) return false;
        return true;
    }

    //检查该坐标对于该棋子是否过河
    boolean pass(int x,int y){
        if(red && y>4) return true;
        else if(!red && y<5) return true;
        return false;
    }

    //检查该坐标对于该棋子是否出了九宫格
    boolean out(int x,int y){
        if(red && abs(x-4)<=1 && abs(y-1) <=1) return false;
        if(!red && abs(x-4) <=1 && abs(y-8)<=1) return false;
        return true;
    }

    //尝试将该棋子移动到（x，y）
    boolean move(int x,int y,String[][] board){
        if(movale(x,y,board)){
            set_loc(x,y);
            return true;
        }
        return false;
    }
}

class Car extends Piece {
    Car(int x, int y, boolean red) {
        super("车", x, y, red);
    }

    @Override
    boolean movale(int x, int y, String[][] board) {
        if(!inboard(x,y)) return false;
        if(this.x == x){
            for(int i=min(this.y,y);i<max(this.y,y);i++)
                if(board[x][i] != "") return false;
        }
        else if(this.y == y){
            for(int i=min(this.x,x);i<max(this.x,x);i++)
                if(board[i][y] != "") return false;
        }
        return true;
    }
}

class Horse extends Piece{
    Horse(int x, int y, boolean red) {
        super("马", x, y, red);
    }

    @Override
    boolean movale(int x, int y, String[][] board) {
        if(!inboard(x,y) ||abs(x-this.x)+abs(y-this.y) != 5) return false;
        if(abs(x-this.x) == 2 && board[(this.x+x)/2][y] != "")
            return false;
        else if (abs(y - this.y) == 2 && board[x][(y+this.y)/2] != "")
            return false;
        return true;
    }
}

class Cannon extends Piece{
    Cannon(int x, int y, boolean red) {
        super("炮", x, y, red);
    }

    @Override
    boolean movale(int x, int y, String[][] board) {
        if(!inboard(x,y)) return false;
        int barrier=0;
        if(this.x == x){
            for(int i=min(this.y,y);i<max(this.y,y);i++)
                if(board[x][i] != "") barrier++;
        }
        else if(this.y == y){
            for(int i=min(this.x,x);i<max(this.x,x);i++)
                if(board[i][y] != "") barrier++;
        }
        if(barrier == 0) return true;
        else if(barrier == 1){
            if(board[x][y] != "") return true;
        }
        return false;
    }
}

class Solider extends Piece{
    boolean pass=false;
    Solider(int x, int y, boolean red) {
        super("兵", x, y, red);
        if(!red) this.name = "卒";
    }

    @Override
    boolean movale(int x, int y, String[][] board) {
        if(!inboard(x,y)) return false;
        if(y-this.y == -1) return false;
        if(pass && abs(x-this.x) + abs(y-this.y) == 1) return true;
        else if (!pass && y-this.y == 1 && x==this.x) {
            if(y == 5) pass = true;
            return true;
        }
        return false;
    }
}

class Minister extends Piece{

    Minister(int x, int y, boolean red) {
        super("相", x, y, red);
        if (!red) this.name = "象";
    }

    @Override
    boolean movale(int x, int y, String[][] board) {
        if(!inboard(x,y) && !pass(x,y)) return false;

        if(abs(x-this.x)==2 && abs(y-this.y)==2) return true;
        return false;
    }
}

class Mandarin extends Piece{
    Mandarin(int x, int y, boolean red) {
        super("士", x, y, red);
    }



    @Override
    boolean movale(int x, int y, String[][] board) {
        if(!inboard(x,y) || out(x,y)) return false;
        if(abs(x-this.x)==1 && abs(y-this.y)==1) return true;
        return false;
    }
}

class King extends Piece{
    King(int x, int y, boolean red) {
        super("帅", x, y, red);
        if(!red) this.name = "将";
    }


    @Override
    boolean movale(int x, int y, String[][] board) {
        if(!inboard(x,y) || out(x,y)) return false;
        if((x-this.x)*(x-this.x) + (y-this.y)*(y-this.y) == 1) return true;
        return false;
    }
}