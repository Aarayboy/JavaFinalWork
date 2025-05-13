package Game.Server;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class ClientServer extends Thread{
    private Socket socket;
    private Info info;
    private Account account;
    private Boolean login;

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
                        break;
                    case short_game:
                        if(!login) break;
                        Match.short_match_queue.add(this.socket);
                        break;
                    case connecting:
                        break;
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean login(String account, String password){
        this.account = Account.find(account,password);
        return true;
    };

}
