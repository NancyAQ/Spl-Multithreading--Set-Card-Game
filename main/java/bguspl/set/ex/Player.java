package bguspl.set.ex;

import java.lang.Thread.State;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

import bguspl.set.Env;

/**
 * This class manages the players' threads and data
 *
 * @inv id >= 0
 * @inv score >= 0 line
 */
public class Player implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;

    /**
     * The id of the player (starting from 0).
     */
    public final int id;

    /**
     * The thread representing the current player.
     */
    public Thread playerThread;//changed to

    /**
     * The thread of the AI (computer) player (an additional thread used to generate
     * key presses).
     */
    private Thread aiThread;

    /**
     * True iff the player is human (not a computer player).
     */
    protected final boolean human; //change back to private

    /**
     * True iff game should be terminated due to an external event.
     */
    private volatile boolean terminate;

    /**
     * The current score of the player.
     */
    private int score;
    /**
     * saves a what a player's tokens placement
     */
      protected volatile BlockingQueue<Integer> tokensPlacement;
    /**
     * boolean permission to give apoint or penalty and a key lock
     */
    protected boolean point;
    protected boolean penalty;
    private volatile boolean lock;
    // *************************************************************************//
    /* boolean variable to make sure the player removes one of the tokens after losing before sending the set to recheck it */
    private volatile boolean didRemove; 

    /**
     * The class constructor.
     *
     * @param env    - the environment object.
     * @param dealer - the dealer object.
     * @param table  - the table object.
     * @param id     - the id of the player.
     * @param human  - true iff the player is a human player (i.e. input is provided
     *               manually, via the keyboard).
     */
    public Player(Env env, Dealer dealer, Table table, int id, boolean human) {
        this.env = env;
        this.table = table;
        this.id = id;
        this.human = human;
        this.tokensPlacement= new LinkedBlockingQueue<Integer>();
        this.point = false;
        this.penalty = false;
        this.lock=false;
        this.didRemove=true;

    }

    // line 89
    /**
     * The main player thread of each player starts here (main loop for the player
     * thread).
     */
    @Override
    public void run() {
        playerThread = Thread.currentThread();
        env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + "starting.");
        if (!human)
            createArtificialIntelligence();

        while (!terminate) {
            if (this.tokensPlacement.size() == env.config.featureSize) {
                while (!point && !penalty) {
                    synchronized (this) {
                        try {
                            this.wait();
                        } catch (Exception e) {
                          
                        }
                    }
                }
                if (point) { //change back the fields after so player doesnt stay sleeping
                    point();
                    point = false;
                    penalty = false;
                 
                } else if (penalty) {
                    penalty();
                    point = false;
                    penalty = false;
                    
                   
                }
            }
            
        }
        if (!human)
            try {
                aiThread.join();
            } catch (InterruptedException ignored) {
            }
        env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * Creates an additional thread for an AI (computer) player. The main loop of
     * this thread repeatedly generates
     * key presses. If the queue of key presses is full, the thread waits until it
     * is not full.
     */
    private void createArtificialIntelligence() {
        // note: this is a very very smart AI (!)
        aiThread = new Thread(() -> {
            env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " starting.");
            while (!terminate) {
                Random rand=new Random();
                int slot=rand.nextInt(env.config.tableSize);
                keyPressed(slot);
            }
            env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " terminated.");
        }, "computer-" + id);
        aiThread.start();
    }

    /**
     * Called when the game should be terminated due to an external event.
     */
    public void terminate() {
        this.terminate = true;
    }

    /**
     * This method is called when a key is pressed.
     *
     * @param slot - the slot corresponding to the key pressed.
     */
    public void keyPressed(int slot) {
       if(table.slotToCard[slot] != null&&this.playerThread.getState()!=State.BLOCKED && !lock && !terminate&&!table.isplacing){
        boolean notNUll=table.slotToCard[slot]!=null;
        if(notNUll){
        int card = table.slotToCard[slot]; 
        boolean contain = tokensPlacement.contains(card);

        if (contain) {
            table.removeToken(this.id, slot);
            tokensPlacement.remove(card);
            didRemove=true;
        }
        if ((tokensPlacement.size() < env.config.featureSize) && (!contain)) {
            tokensPlacement.add(card);
            table.placeToken(this.id, slot);
    
        }
        if ((tokensPlacement.size() == env.config.featureSize)& (this.didRemove)) {
            table.PlayersToCheck.add(this);
        }
    }
        }
    }

    /**
     * Award a point to a player and perform other related actions.
     *
     * @post - the player's score is increased by 1.
     * @post - the player's score is updated in the ui.
     */
    public void point() {
      
        // make the ui timer in red beside the player in a better way!!!!!!!!!!!!!
        // ******************************************************************* */
        this.score++;
        this.tokensPlacement.clear(); 
        int ignored = table.countCards(); // this part is just for demonstration in the unit tests

        env.ui.setScore(id, score);

        synchronized (this) {
            try {
                if(env.config.pointFreezeMillis>0){
                timethread timer=new timethread(env.config.pointFreezeMillis,this.id,env);
                timer.start();
                this.lock=true;
                this.wait(env.config.pointFreezeMillis);
                this.lock=false;
            }
            } 
            catch (InterruptedException e) {
              
            }
        }
       

    }

    /**
     * Penalize a player and perform other related actions.
     */
    public void penalty() {
       
        synchronized (this) {
            try {
                if(env.config.penaltyFreezeMillis>0){
               timethread timer=new timethread(env.config.penaltyFreezeMillis,this.id,env);
               timer.start();
               this.lock=true;
              this.wait(env.config.penaltyFreezeMillis);}
                if(this.tokensPlacement.size()==env.config.featureSize)
                this.didRemove=false;
                this.lock=false;
              
               
            } catch (InterruptedException e) {
            
            }
        }
    }

    public int score() {
        return score;
    }
  /**
     * sets point to set
     *
     * @pre - false or true
     * @post - set
     */
    public void setPoint(boolean set) {
        this.point = set;
        synchronized (this) {
            this.notify();
        }
    }
    /**
     * sets penalty to set
     *
     * @pre - false or true
     * @post - set
     */

    public void setPenalty(boolean set) {
        this.penalty = set;
        synchronized (this) {
            this.notify();
        }
    }

   
    
 
 
}