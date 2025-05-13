## 游戏架构

1. 登录/创建账户
   1. 服务器任务：根据客户端发送的用户名和密码检查是否有这个账户，若没有则需要创建
      1. 有数据库支持查询、插入等
   2. 客户端任务：发送用户名和密码
   3. GUI任务：读取用户输入的用户名和密码

2. 选择游戏模式

   1. 正常模式、快速模式
   2. 服务器任务：根据模式选择对应的游戏流程
   3. 客户端任务：发送相应的模式信号
   4. GUI任务：制作相应的GUI

3. 游戏

   1. 服务器和客户端任务：通信棋盘、下子情况

      1. 服务器需要负责判断胜负
      2. 客户端能够进行投降、悔棋操作

   2. GUI任务：

      1. 显示账户头像、昵称
      2. 有聊天框
         1. 添加常用语句，比如“棋逢对手，将遇良才”、“我等的花儿都谢了”等
         2. 支持自定义输入
      3. 棋子在下方（若本地为红方则红色棋子在下方，反之黑色棋子在下方）
      4. 有胜负结算画面
      5. 有选项框包括
         1. 投降
         2. 悔棋
         3. 请求和棋

      6. 显示局时，步时



## Info



```java

//状态
enum Process
{

    connecting,             //常态


    //客户端
    login,                  //客户端发送登录信号
    normal_game,            //客户端发送正常模式信号
    short_game,             //客户端发送快速模式信号
    withdraw,               //客户端发送悔棋信号
    want_even,              //客户端发送请求和棋信号

    //服务器
    login_success,          //服务器向客户端发送登录结果
    login_fail,             //服务器向客户端发送登录结果
    game_start,             //服务器向客户端发送游戏开始信号
    win,                    //服务器向客户端发送游戏结果
    lose,                   //服务器向客户端发送游戏结果
    want_lose,              //服务器向客户端发送游戏结果

};


public class Info implements Serializable{
    @Serial
    private static final long serialVersionUID = -2095916884810199532L;

    //时间戳
    Calendar calendar;
    long start_time;
    
    Process process;    //游戏进程以及游戏结果


    //Server 向 Client 发送
    String[][] board;   //棋盘信息
    boolean red;        //Client的红黑身份
    boolean turn;       //当前是否轮到Client下棋


    //Client 向 Server 发送

    //下棋时
    int ox,oy,nx,ny;    //(ox,oy) -> (nx,ny)

    //登录时
    String account,password;

    //客户端对话
    String chat;



    //服务器使用,游戏时
    Info(Process process, String[][] board, boolean red, boolean turn, String chat){
        this.process = process;
        this.board = board;
        this.red = red;
        this.turn = turn;
        this.chat = chat;
    }

    //客户端使用,游戏时
    Info(Process process, int ox,int oy,int nx,int ny){
        this.process = process;
        this.ox = ox;
        this.oy = oy;
        this.nx = nx;
        this.ny = ny;
        this.chat = chat;
    }

    //客户端使用,登陆时
    Info(Process process, String account, String password){
        this.process = process;
        this.account = account;
        this.password = password;
    }




}

```





