package Game.Server;

import java.io.*;
import java.util.*;

//状态
enum Process
{

    connecting,             //常态


    //客户端
    login,                  //客户端发送登录信号
    normal_game,            //客户端发送正常模式信号
    short_game,             //客户端发送快速模式信号
    withdraw,               //客户端发送悔棋信号
    want_even,              //客户端发送请求和棋信号

    //服务器
    login_success,          //服务器向客户端发送登录结果
    login_fail,             //服务器向客户端发送登录结果
    game_start,             //服务器向客户端发送游戏开始信号
    win,                    //服务器向客户端发送游戏结果
    lose,                   //服务器向客户端发送游戏结果
    want_lose,              //服务器向客户端发送游戏结果

};


public class Info implements Serializable{
    @Serial
    private static final long serialVersionUID = -2095916884810199532L;

    //时间戳
    Calendar calendar;
    long start_time;

    Process process;    //游戏进程以及游戏结果


    //Server 向 Client 发送
    String[][] board;   //棋盘信息
    boolean red;        //Client的红黑身份
    boolean turn;       //当前是否轮到Client下棋


    //Client 向 Server 发送

    //下棋时
    int ox,oy,nx,ny;    //(ox,oy) -> (nx,ny)

    //登录时
    String account,password;

    //客户端对话
    String chat;



    //服务器使用,游戏时
    Info(Process process, String[][] board, boolean red, boolean turn, String chat){
        this.process = process;
        this.board = board;
        this.red = red;
        this.turn = turn;
        this.chat = chat;
    }

    //客户端使用,游戏时
    Info(Process process, int ox,int oy,int nx,int ny){
        this.process = process;
        this.ox = ox;
        this.oy = oy;
        this.nx = nx;
        this.ny = ny;
        this.chat = chat;
    }

    //客户端使用,登陆时
    Info(Process process, String account, String password){
        this.process = process;
        this.account = account;
        this.password = password;
    }




}
