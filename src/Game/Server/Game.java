package Game.Server;

import java.net.Socket;
import java.net.SocketTimeoutException;

public class Game extends Thread {
    Socket red, black;
    int time;

    Game(Socket red, Socket black,int time){
        this.black = black;
        this.red = red;
        this.time = time;
    }

    @Override
    public void run(){

    }






}
