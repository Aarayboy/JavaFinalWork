package Game.Server;

import org.jetbrains.annotations.NotNull;

enum Props{
    changing_name,  //改名卡
    adding_time,    //加时卡
}



public class Account {
    String name;        //昵称
    String account;     //用户名
    int[] props;        //道具情况
    final int props_number = 2; //道具种类
    int level;          //技术水平（可以据此在 Match 的队列中选择水平相近的对战）
                        //也可以不继续完善匹配机制，使用目前比较粗糙的匹配机制



    Account(@NotNull Account account_1){
        name = account_1.name;
        account = account_1.account;
        for(int i = 0;i<props_number;i++){
            props[i] = account_1.props[i];
        }
        level = 0;

    }

    Account(String name, String account, int[] props){
        this.name = name;
        this.account = account;
        for(int i = 0; i < props_number;i++){
            this.props[i] = props[i];
        }
        level = 0;
    }


    //从数据库中找到该账号
    //依赖于MySqlite类(todo)
    public static Account find(String account, String password) {
        // todo
        return null;
    }


    //在数据库中创建新账号
    //依赖于MySqlite类(todo)
    public static boolean create(String account, String password){
        // todo
        return true;
    }

    //修改名字，需要写入数据库中
    public static boolean changing_name(){
        //todo
        return false;
    }



}
