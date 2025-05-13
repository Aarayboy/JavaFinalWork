package Game.Server;


import java.net.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Match {
    public static ConcurrentLinkedQueue<Socket> normal_match_queue = new ConcurrentLinkedQueue<>();
    public static ConcurrentLinkedQueue<Socket> short_match_queue = new ConcurrentLinkedQueue<>();

    public static void match(){
        while(true){
            if(normal_match_queue.size() > 1){
                Socket red = normal_match_queue.peek();
                normal_match_queue.remove();
                Socket black = normal_match_queue.peek();
                normal_match_queue.remove();
                new Game(red, black, 15).run();
            }

            if(short_match_queue.size() > 1){
                Socket red = short_match_queue.peek();
                short_match_queue.remove();
                Socket black = short_match_queue.peek();
                short_match_queue.remove();
                new Game(red, black, 5).run();
            }
        }
    }



}
