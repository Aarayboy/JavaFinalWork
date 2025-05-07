package Game.Client;

import java.io.*;
import java.util.*;



public class Info implements Serializable{
    @Serial
    private static final long serialVersionUID = -2095916884810199532L;


    //时间戳
    Calendar calendar;

    //Server 向 Client 发送
    String[][] board;   //棋盘信息
    boolean red;        //Client的红黑身份
    boolean turn;       //当前是否轮到Client下棋
    Boolean winner;     //null表示尚未决出胜负,1表示获胜,0表示失败



    //Client 向 Server 发送
    int ox,oy,nx,ny;    //(ox,oy) -> (nx,ny)


}
