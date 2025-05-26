package Game.Client;

/**
 * 游戏进程状态枚举
 * 根据 Readme.md 定义
 */
public enum Process {
    // 连接状态
    connecting,     // 连接中或等待状态
    gaming,         // 游戏中 (特指客户端发送走棋操作)
    unconnecting,   // 连接断开 (异常)

    // 客户端 -> 服务器
    login,          // 客户端请求登录
    register,
    normal_game,    // 客户端选择正常模式
    short_game,     // 客户端选择快速模式 (暂未实现具体逻辑区分)
    withdraw,       // 客户端请求悔棋
    want_even,      // 客户端请求和棋
    want_lose,      // 客户端请求投降
    chat,           // 客户端发送聊天消息 (新增)

    // 服务器 -> 客户端
    login_success,  // 服务器通知登录成功
    login_fail,     // 服务器通知登录失败
    register_success,
    register_fail,
    game_start,     // 服务器通知游戏开始
    game_update,    // 服务器发送游戏状态更新 (棋盘、轮到谁等) (新增，替代部分 gaming 用途)
    win,            // 服务器通知胜利
    lose,           // 服务器通知失败
    draw,           // 服务器通知和棋 (新增)
    opponent_disconnected, // 服务器通知对手断开连接 (新增)
    message,        // 服务器发送通用消息 (新增，例如等待玩家、无效操作等)

    //悔棋和棋
    ask_withdraw,
    ask_even,
    withdraw_result,
    even_result,
    withdraw_accept,
    withdraw_reject,
    even_accept,
    even_reject,

    //超时
    timeout_lose,

}
