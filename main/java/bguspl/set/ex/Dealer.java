package bguspl.set.ex;

//import bguspl.set.Config;
import bguspl.set.Env;
//import bguspl.set.WindowManager;

import java.util.Iterator;
//import java.util.Collections;
//import java.util.LinkedList;
import java.util.List;
//import java.util.Queue;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

//import javax.swing.text.html.HTMLDocument.Iterator;

/**
 * This class manages the dealer's threads and data
 */
public class Dealer implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;
    public final Player[] players;// changed to public

    /**
     * The list of card ids that are left in the dealer's deck.
     */
    protected final List<Integer> deck;

    /**
     * True iff game should be terminated due to an external event.
     */
    protected volatile boolean terminate;

    /**
     * The time when the dealer needs to reshuffle the deck due to turn timeout.
     */
    private long reshuffleTime = Long.MAX_VALUE;

    /**
     * array for each player thread
     */
    private Thread[] playerThreads;

    // *******************************************************************************************************//

    public Dealer(Env env, Table table, Player[] players) {
        this.env = env;
        this.table = table;
        this.players = players;
        deck = IntStream.range(0, env.config.deckSize).boxed().collect(Collectors.toList());
        playerThreads = new Thread[env.config.players];

    }

    /**
     * The dealer thread starts here (main loop for the dealer thread).
     */
    @Override
    public void run() {
        env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " starting.");
        createAPlayerThread();
        while (!shouldFinish()) {
            placeCardsOnTable();
            timerLoop();
            updateTimerDisplay(false);
            removeAllCardsFromTable();
        }
        table.removeAllTokens();
        announceWinners();
        terminate();
        env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * The inner loop of the dealer thread that runs as long as the countdown did
     * not time out.
     */
    private void timerLoop() {
        reshuffleTime = System.currentTimeMillis() + env.config.turnTimeoutMillis;
        for (int i = 0; i < players.length; i++) {
            if (!(players[i].tokensPlacement.isEmpty())) {
                players[i].tokensPlacement.clear();
            }
        }
        while (!terminate && System.currentTimeMillis() < reshuffleTime) {
            if (table.PlayersToCheck.size() != 0) {
                checkSet();
            }
            sleepUntilWokenOrTimeout();
            updateTimerDisplay(false);
            removeCardsFromTable();
            placeCardsOnTable();
        }
    }

    /**
     * Called when the game should be terminated due to an external event.
     */
    public void terminate() {

        for (Player p : players) {
            p.terminate();
        }
        terminate = true;
    }

    /**
     * Check if the game should be terminated or the game end conditions are met.
     *
     * @return true iff the game should be finished.
     */
    protected boolean shouldFinish() {
        return terminate || env.util.findSets(deck, 1).size() == 0;
    }

    /**
     * Checks cards should be removed from the table and removes them.
     */
    private void removeCardsFromTable() {

    }

    /**
     * Check if any cards can be removed from the deck and placed on the table.
     */
    private void placeCardsOnTable() {
        table.isplacing = true;
        Random rand = new Random();
        Random crandom = new Random();
        while (table.emptyspots() != 0 & deck.size() > 0) {
            int place = rand.nextInt(env.config.tableSize);
            if (table.slotToCard[place] == null) {
                int cardindex = crandom.nextInt(deck.size());
                int card = deck.get(cardindex);
                table.placeCard(card, place);
                deck.remove(cardindex);
            }
        }
        table.isplacing = false;
    }

    /**
     * Sleep for a fixed amount of time or until the thread is awakened for some
     * purpose.
     */
    private void sleepUntilWokenOrTimeout() {

    }

    /**
     * Reset and/or update the countdown and the countdown display.
     */
    private void updateTimerDisplay(boolean reset) {
        long remainingTime = reshuffleTime - System.currentTimeMillis();
        if (remainingTime < 0) {

            remainingTime = 0;
        }

        if (reset == true) {
            env.ui.setCountdown(env.config.turnTimeoutMillis, false);
            reshuffleTime = System.currentTimeMillis() + env.config.turnTimeoutMillis;
        } else if (remainingTime < env.config.turnTimeoutWarningMillis) {
            env.ui.setCountdown(remainingTime, true);
        } else {
            env.ui.setCountdown(remainingTime, false);
        }

    }

    /**
     * Returns all the cards from the table to the deck.
     */
    public void removeAllCardsFromTable() {

        Random rand = new Random(); // randomly removes all cards from table
        while (table.emptyspots() != 12) {
            int place = rand.nextInt(env.config.tableSize);
            if (table.slotToCard[place] != null) {
                int card = table.slotToCard[place];
                table.removeCard(place);
                deck.add(card);
            }
        }

    }

    /**
     * Check who is/are the winner/s and displays them.
     */
    private void announceWinners() {
        int maxscore = players[0].score(); // first we need to know the max score
        for (int i = 0; i < players.length; i++) {
            if (players[i].score() > maxscore)
                maxscore = players[i].score();
        }
        // count num of winners
        int winnersNum = 0;
        for (int i = 0; i < players.length; i++) {
            if (players[i].score() == maxscore)
                winnersNum++;
        }
        int[] winners = new int[winnersNum];
        int index = 0;
        for (int i = 0; i < players.length & index < winnersNum; i++) {
            if (players[i].score() == maxscore) {
                winners[index] = players[i].id;
                index++;
            }
        }
        env.ui.announceWinner(winners);
    }

    /**
     * creates and starts a thread for each player
     */
    private void createAPlayerThread() {
        for (int i = 0; i < env.config.players; i++) {
            playerThreads[i] = new Thread(this.players[i], "Player's Id is: " + players[i].id);
        }
        for (int i = 0; i < playerThreads.length; i++) {
            playerThreads[i].start();
        }
    }

    /**
     * dealer checks if the player who made 3 tokens has a set or not
     * the player gets a point or a penalty
     * 
     */
    public synchronized void checkSet() {

        if (!(table.PlayersToCheck.isEmpty())) {
            Player playerInCheck = table.PlayersToCheck.poll();
            if (playerInCheck.tokensPlacement.size() == 3) {
                int[] cards = TokensactionsArray(playerInCheck.tokensPlacement);
                boolean Set = env.util.testSet(cards);
                if (Set) {
                    removingCards(cards);
                    removeWinningCardsTokens(cards);
                    playerInCheck.setPoint(true);
                    reshuffleTime = System.currentTimeMillis() + env.config.turnTimeoutMillis;
                } else {
                    if (!(Set)) {
                        playerInCheck.setPenalty(true);
                    }
                }
            }
        }

    }

    ////////////////////////////////////////////////////////////////////////////////
    public void removeWinningCardsTokens(int[] cards) {
        for (int i = 0; i < players.length; i++) {
            for (int j = 0; j < cards.length; j++) {
                if (players[i].tokensPlacement.contains(cards[j]))
                    players[i].tokensPlacement.remove(cards[j]);
            }
        }

    }

    public void removingCards(int[] cards) {
        for (int i = 0; i < cards.length; i++) {
            if (table.cardToSlot[cards[i]] != null) {
                int slot = table.cardToSlot[cards[i]];
                table.removeCard(slot);
            }
        }
    }

    public int[] TokensactionsArray(BlockingQueue<Integer> queue) {
        int[] output = new int[3];
        Iterator<Integer> itr = queue.iterator();
        int index = 0;
        while (itr.hasNext() & index < output.length) {
            output[index] = itr.next();
            index = index + 1;
        }

        return output;
    }

}