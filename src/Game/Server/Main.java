package Game.Server;

import java.io.*;
import java.net.*;


public class Main {

    final int max_number = 100;

    public void main(String[] args) throws IOException {
        //端口号
        int port = 55533;



        //监听端口
        ServerSocket server = new ServerSocket(port);

        Socket socket_1 = server.accept();
        Socket socket_2 = server.accept();

        InputStreamReader isr_1 = new InputStreamReader(socket_1.getInputStream());
        InputStreamReader isr_2 = new InputStreamReader(socket_2.getInputStream());

        BufferedReader br_1 = new BufferedReader(isr_1);
        BufferedReader br_2 = new BufferedReader(isr_2);


        System.out.println(br_1.readLine());
        System.out.println(br_1.readLine());






    }



}
