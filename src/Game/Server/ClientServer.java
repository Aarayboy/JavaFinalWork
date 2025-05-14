package Game.Server;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class ClientServer extends Thread{
    private Socket socket;          //客户端通信
    private Info info;              //通信使用的类
    private Account account;        //账号
    private Boolean login;          //是否完成登录

    public ClientServer(Socket socket){
        this.socket = socket;
        this.account = null;
    }


    @Override
    public void run(){
        ObjectInputStream ois = null;
        ObjectOutputStream oos = null;

        try {
            ois = new ObjectInputStream(this.socket.getInputStream());
            oos = new ObjectOutputStream(this.socket.getOutputStream());
            while(true){
                info = (Info) ois.readObject();
                switch (info.process){
                    case login:
                        if(login(info.account, info.password)){
                            oos.write("登陆成功".getBytes("UTF-8"));
                        }
                        break;
                    case normal_game:
                        if(!login) break;
                        Match.normal_match_queue.add(this.socket);
                        //添加进入匹配队列后需要一个函数来保证该进程卡在这里
                        //或者添加进入匹配队列这个函数本身会使这个进程不再继续接受信息
                        //todo
                        break;
                    case short_game:
                        if(!login) break;
                        Match.short_match_queue.add(this.socket);
                        //添加进入匹配队列后需要一个函数来保证该进程卡在这里
                        //或者添加进入匹配队列这个函数本身会使这个进程不再继续接受信息
                        //todo
                        break;
                    case connecting:
                        break;
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    //登陆函数
    //todo
    public boolean login(String account, String password){
        this.account = Account.find(account,password);
        return true;
    };

}
