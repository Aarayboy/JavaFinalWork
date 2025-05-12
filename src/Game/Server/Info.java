package Game.Server;

import java.io.*;
import java.util.*;

//类别
enum Process
{   login,
    connecting,
    normal_game,
    short_game,
    gaming,
    win,
    lose,
    want_lose
};


public class Info implements Serializable{
    @Serial
    private static final long serialVersionUID = -2095916884810199532L;


    //时间戳
    Calendar calendar;

    //Server 向 Client 发送
    String[][] board;   //棋盘信息
    boolean red;        //Client的红黑身份
    boolean turn;       //当前是否轮到Client下棋
    Process process;    //游戏进程



    //Client 向 Server 发送
    int ox,oy,nx,ny;    //(ox,oy) -> (nx,ny)
    String account,password;

}
