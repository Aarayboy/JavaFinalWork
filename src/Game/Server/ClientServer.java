package Game.Server;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class ClientServer extends Thread{
    private Socket socket;
    private Info info;

    public ClientServer(Socket socket){
        this.socket = socket;
    }


    @Override
    public void run(){
        ObjectInputStream ois = null;
        ObjectOutputStream oos = null;

        try {
            ois = new ObjectInputStream(this.socket.getInputStream());
            oos = new ObjectOutputStream(this.socket.getOutputStream());
            info = (Info) ois.readObject();
            switch (info.process){
                case login:
                    if(login(info.account, info.password)){
                        oos.write("登陆成功".getBytes("UTF-8"));
                    }
                    break;
                case connecting:
                    break;
                case normal_game:
                    break;
                case short_game:
                    break;
                case gaming:
            }






        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean login(String account, String password){
        return true;
    };

}
