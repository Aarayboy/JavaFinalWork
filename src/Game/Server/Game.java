package Game.Server;

import java.net.Socket;
import java.net.SocketTimeoutException;

//实施游戏进程的类
public class Game extends Thread {
    Socket red, black;
    int time;

    Game(Socket red, Socket black,int time){
        this.black = black;     //黑方的Socket
        this.red = red;         //红方的Socket
        this.time = time;       //本局游戏的总时长，即局时
    }

    //游戏进程，由 Match.match() 调用
    @Override
    public void run(){

    }






}
