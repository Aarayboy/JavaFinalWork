package Game.Server;

import org.jetbrains.annotations.NotNull;

enum Props{
    changing_name,  //改名卡
    adding_time,    //加时卡
}



public class Account {
    String name;        //昵称
    String account;     //用户名
    int[] props;          //道具情况
    final int props_number = 2;



    Account(@NotNull Account account_1){
        name = account_1.name;
        account = account_1.account;
        for(int i = 0;i<props_number;i++){
            props[i] = account_1.props[i];
        }

    }

    Account(String name, String account, int[] props){
        this.name = name;
        this.account = account;
        for(int i = 0; i < props_number;i++){
            this.props[i] = props[i];
        }
    }





}
