package bguspl.set.ex;

import bguspl.set.Env;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
    private final Player[] players;

    /**
     * The list of card ids that are left in the dealer's deck.
     */
    private final List<Integer> deck;

    /**
     * True iff game should be terminated.
     */
    private volatile boolean terminate;

    /**
     * The time when the dealer needs to reshuffle the deck due to turn timeout.
     */
    private long reshuffleTime = Long.MAX_VALUE;




    public Dealer(Env env, Table table, Player[] players) {
        this.env = env;
        this.table = table;
        this.players = players;
        deck = IntStream.range(0, env.config.deckSize).boxed().collect(Collectors.toList());
    }

    /**
     * The dealer thread starts here (main loop for the dealer thread).
     */
    @Override
    public void run() {//Anni asks a question
        env.logger.info("thread " + Thread.currentThread().getName() + " starting.");
        while (!shouldFinish()) {
            //should we create players threads here? according to config?
            placeCardsOnTable();
            //how to shuffle cards?
            timerLoop();
            updateTimerDisplay(false);
            removeAllCardsFromTable();
        }
        announceWinners();
        env.logger.info("thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * The inner loop of the dealer thread that runs as long as the countdown did not time out.
     */
    private void timerLoop() {
        while (!terminate && System.currentTimeMillis() < reshuffleTime) {
            sleepUntilWokenOrTimeout();
            updateTimerDisplay(false);
            removeCardsFromTable();
            placeCardsOnTable();
        }
    }

    /**
     * Called when the game should be terminated.
     */
    public void terminate() {
        // TODO implement
    }

    /**
     * Check if the game should be terminated or the game end conditions are met.
     *
     * @return true iff the game should be finished.
     */
    private boolean shouldFinish() {
        return terminate || env.util.findSets(deck, 1).size() == 0;
    }

    /**
     * Checks cards should be removed from the table and removes them.
     */
    private void removeCardsFromTable() {//Anni did it - should be only cards that needs to be removed (not all)


        // TODO implement

    }

    /**
     * Check if any cards can be removed from the deck and placed on the table.
     */
    private void placeCardsOnTable() {//Anni asks a question
        //is the table we get in the constructor full or empty? if empty how do we know what cards to put (config)?
        // TODO implement
    }

    /**
     * Sleep for a fixed amount of time or until the thread is awakened for some purpose.
     */
    private void sleepUntilWokenOrTimeout() {
        // TODO implement
    }

    /**
     * Reset and/or update the countdown and the countdown display.
     */
    private void updateTimerDisplay(boolean reset) {
        // TODO implement
    }

    /**
     * Returns all the cards from the table to the deck.
     */
    private void removeAllCardsFromTable() {//Anni made some changes + asks a question

//        for (int i = 0; i < table.grid.length; i++) {
//            for (int j = 0; j < table.grid[i].length; j++) {
//                if (table.grid[i][j] != null) {
//                    deck.add(table.grid[i][j]);
//                    table.removeCard(table.grid[i][j]);
//                    table.grid[i][j] = null;
//                }
//            }
//        }
        //do we need to check first if there is any legal set in deck/table?
        //is there another occasion in which we need to remove all? maybe every 60 sec?
        table.removeAllCardsFromTable();
        //do we need to change the players table too or is it the same table?
        // TODO implement
    }

    /**
     * Check who is/are the winner/s and displays them.
     */
    private void announceWinners() {

        int maxScore = 0;
        for (Player player : players) {
            if (player.score() > maxScore) {
                maxScore = player.score();
            }
        }

        int [] winners = new int[players.length]; // try to find a better way to do this - Dynamic array
        int numOfWinners = 0;
        for (Player player : players) {
            if (player.score() == maxScore) {
                winners[numOfWinners] = player.id;
                numOfWinners = numOfWinners+1;
            }
        }
        env.ui.announceWinner(winners);  // what happens if there is fewer winners than the array length?

        for(Player player : players) {
            player.terminate();
            // terminate the player threads, and the dealer thread table etc
        }
    }




    // The following methods are added for the purpose of the exercise.
    protected synchronized boolean testSet(int[] cards) {
        // need to implement as system function
        if(cards.length != 3) {
            return false;
        }
        boolean isSet = env.util.testSet(cards);
        if(isSet) {
            for (int i = 0; i < 3; i++) {
                table.removeCard(cards[i]);
            }
            placeCardsOnTable(); // TODO implement
            //check if there is sets on the table

            return true;
        }
        return false;

    }

    // helper function that checks if there is a set on the table and deck
}
