package Game.Client;

import java.io.*;
import java.net.*;
import java.util.*;


public class Main {
    public void main(String[] args) throws Exception {
        // 要连接的服务端IP地址和端口
        String host= "10.129.205.13";
        int port = 55533;

        // 与服务端建立连接
        Socket socket = new Socket(host, port);

        // 建立连接后获得输入输出流
        OutputStreamWriter Writer = new OutputStreamWriter(socket.getOutputStream(),"UTF-8");
        InputStreamReader Reader = new InputStreamReader(socket.getInputStream(), "UTF-8");

        InetAddress localHost = InetAddress.getLocalHost();


        // 创建读取缓冲区
        char[] buffer = new char[1024];
//        int length = Reader.read(buffer);
        Writer.write("Successfully Connected from " + localHost.getHostAddress());
        Writer.flush();



        //服务器分配红/黑方,初始时间
        //String response = new String(buffer, 0, length);


//        while(true){
//            length = Reader.read(buffer);
//            response = new String(buffer, 0, length);
//
//
//            // 模拟发送新消息
//            String message = "New message from client";
//            Writer.write(message);
//            Writer.flush(); // 刷新缓冲区，确保数据发送
//
//            if(response == "end") break;
//        }



        //游戏结束，关闭链接
        Writer.close();
        Reader.close();
        socket.close();





    }
}
