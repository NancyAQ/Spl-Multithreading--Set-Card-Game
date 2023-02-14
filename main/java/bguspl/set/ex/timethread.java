package bguspl.set.ex;
import bguspl.set.Env;

public class timethread extends Thread{
    private long penaltyTime;
    private int playerId;
    private final Env env;
    private long startTime=System.currentTimeMillis();
    private long remainingTime;
    private volatile boolean terminate;

public timethread(long penaltyTime,int playerId,Env env){
    this.penaltyTime=penaltyTime;
    this.playerId=playerId;
    this.env=env;
    this.remainingTime=this.penaltyTime;
    this.terminate=false;
}
@Override
public void run(){
   try {
    while (remainingTime>0&!terminate) {
        env.ui.setFreeze(playerId, remainingTime);
        long elapsedTime = System.currentTimeMillis() - startTime;
         remainingTime = penaltyTime - elapsedTime; 
        
      }
      if (remainingTime <= 0) {
        env.ui.setFreeze(playerId, 0);
        this.terminate=true;
        
      } 
      
   } catch (Exception e) {

   }
}
}
