package bguspl.set.ex;

import bguspl.set.Config;
import bguspl.set.Env;
import bguspl.set.UserInterface;
import bguspl.set.Util;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DealerTest {

    Dealer dealer;
    Table table;
    private Integer[] slotToCard;
    private Integer[] cardToSlot;
    private Player[] players;

    @BeforeEach
    void setUp() {

        Properties properties = new Properties();
        properties.put("Rows", "2");
        properties.put("Columns", "2");
        properties.put("FeatureSize", "3");
        properties.put("FeatureCount", "4");
        properties.put("TableDelaySeconds", "0");
        properties.put("PlayerKeys1", "81,87,69,82");
        properties.put("PlayerKeys2", "85,73,79,80");
        MockLogger logger = new MockLogger();
        Config config = new Config(logger, properties);

        slotToCard = new Integer[config.tableSize];
        cardToSlot = new Integer[config.deckSize];
        players = new Player[config.players];
        Env env = new Env(logger, config, new MockUserInterface(), new MockUtil());
        table = new Table(env, slotToCard, cardToSlot);
        dealer = new Dealer(env, table, players);
        Player player0 = new Player(env, dealer, table, 0, false);
        Player player1 = new Player(env, dealer, table, 1, false);
        players[0] = player0;
        players[1] = player1;
    }

    ///////////////////////////////////////////////////////////

    // Auxiliary function:

    private void placeSomeTokens() throws InterruptedException {

        // place some tokens on some slots,
        // and add them to the tokensPlacement queue for the player in order to:
        // check that removeWinningCardsTokens is working correctly and is returnig
        // cards

        table.placeToken(players[0].id, 1);
        players[0].tokensPlacement.add(13);
        table.placeToken(players[0].id, 2);
        players[0].tokensPlacement.add(9);
        table.placeToken(players[0].id, 3);
        players[0].tokensPlacement.add(27);

        table.placeToken(players[1].id, 2);
        players[1].tokensPlacement.add(9);
        table.placeToken(players[1].id, 3);
        players[1].tokensPlacement.add(27);

    }

    ///////////////////////////////////////////////////////////

    // tests:

    // test 1 made by us is for the method: removeAllCardsFromTable

    @Test
    public void testShouldFinish_shouldReturnTrueIfTerminateIsTrue() {
        dealer.terminate = true;
        assertTrue(dealer.shouldFinish());

    }

    // test 2 made by us is for the method: removeWinningCardsTokens

    @Test
    void removeWinningCardsTokensCheck() throws InterruptedException {

        // array with cards that will have some token to be removed
        int[] cards = { 9, 13, 27 };

        // placing some tokens (to be removed later)
        placeSomeTokens();

        // check that the tokens were placed correctly ans successfully
        assertEquals(3, players[0].tokensPlacement.size());
        assertEquals(13, players[0].tokensPlacement.peek());

        assertEquals(2, players[1].tokensPlacement.size());
        assertEquals(9, players[1].tokensPlacement.peek());

        // call the method we are testing
        dealer.removeWinningCardsTokens(cards);

        // check that the tokens were removed correctly ans successfully
        assertEquals(0, players[0].tokensPlacement.size());
        assertEquals(null, players[0].tokensPlacement.peek());
        assertEquals(0, players[1].tokensPlacement.size());
        assertEquals(null, players[1].tokensPlacement.peek());

    }

    ///////////////////////////////////////////////////////////

    static class MockUserInterface implements UserInterface {
        @Override
        public void dispose() {
        }

        @Override
        public void placeCard(int card, int slot) {
        }

        @Override
        public void removeCard(int slot) {
        }

        @Override
        public void setCountdown(long millies, boolean warn) {
        }

        @Override
        public void setElapsed(long millies) {
        }

        @Override
        public void setScore(int player, int score) {
        }

        @Override
        public void setFreeze(int player, long millies) {
        }

        @Override
        public void placeToken(int player, int slot) {
        }

        @Override
        public void removeTokens() {
        }

        @Override
        public void removeTokens(int slot) {
        }

        @Override
        public void removeToken(int player, int slot) {
        }

        @Override
        public void announceWinner(int[] players) {
        }
    };

    static class MockUtil implements Util {
        @Override
        public int[] cardToFeatures(int card) {
            return new int[0];
        }

        @Override
        public int[][] cardsToFeatures(int[] cards) {
            return new int[0][];
        }

        @Override
        public boolean testSet(int[] cards) {
            return false;
        }

        @Override
        public List<int[]> findSets(List<Integer> deck, int count) {
            return null;
        }

        @Override
        public void spin() {
        }
    }

    static class MockLogger extends Logger {
        protected MockLogger() {
            super("", null);
        }
    }
}