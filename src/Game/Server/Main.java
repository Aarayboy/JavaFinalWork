package Game.Server;

import java.io.*;
import java.net.*;
import java.util.*;


//服务器程序入口

public class Main {

    //最大连接数量
    final int max_number = 100;

    public void main(String[] args) throws IOException {
        //端口号
        int port = 55533;

        //监听端口
        ServerSocket server = new ServerSocket(port);

        //打开匹配机制
        new Thread(()->{
            Match.match();
        }).run();

        //不停的接受客户端请求并交由线程服务
        while(true)
        {
            Socket socket = server.accept();
            new ClientServer(socket).run();
        }








    }



}
