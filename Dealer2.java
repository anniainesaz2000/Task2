package bguspl.set.ex;
import java.util.*;

import bguspl.set.Env;

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
    public Queue<Player> playersQueue;
    public Object isThereSet;




    public Dealer(Env env, Table table, Player[] players) {
        this.env = env;
        this.table = table;
        this.players = players;
        deck = IntStream.range(0, env.config.deckSize).boxed().collect(Collectors.toList());
        this.terminate = false;
        this.reshuffleTime = System.currentTimeMillis();
        this.playersQueue = new LinkedList<>();
        this.isThereSet = new Object();
    }

    /**
     * The dealer thread starts here (main loop for the dealer thread).
     */
    @Override
    public void run() {
        while(!Thread.currentThread().isInterrupted()){
            env.logger.info("thread " + Thread.currentThread().getName() + " starting.");
            for (Player player: players){
                new Thread(player).start();
            }
            while (!shouldFinish()) {
                placeCardsOnTable();
                timerLoop();
                updateTimerDisplay(true);
                removeCardsFromTable();
            }
            announceWinners();
            env.logger.info("thread " + Thread.currentThread().getName() + " terminated.");
            terminate();
        }

    }

    /**
     * The inner loop of the dealer thread that runs as long as the countdown did not time out.
     */
    private void timerLoop() {
        System.out.println("enters timerLoop");
        reshuffleTime = env.config.turnTimeoutMillis + System.currentTimeMillis();
        while (!terminate && System.currentTimeMillis() < reshuffleTime) {
            System.out.println("enters timerLoop while");
            sleepUntilWokenOrTimeout();
            updateTimerDisplay(false);
            removeCardsFromTable();
            System.out.println("before placeCardsOnTable");
            placeCardsOnTable();
        }
    }

    /**
     * Called when the game should be terminated.
     */
    public void terminate() {//x
        this.terminate=true;
        for(Player player : players) {
            player.terminate();
            // terminate the player threads, and the dealer thread
        }
         Thread.currentThread().interrupt();
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
    private void removeCardsFromTable() {
        blockPlayersTokens();
        System.out.println("removeCardsFromTable");
        returnAllToDeck();
        table.removeAllCardsFromTable();
        getSetToTest();
        for(Player player : players){
            player.setsQueue.clear();
        }
        unblockPlayersTokens();
    }

    public void returnAllToDeck(){
        Integer[] array =this.table.getSlotToCard();
        for(Integer card: array){
            if(card!=null){
                this.deck.add(card);
            }

        }

    }

    /**
     * Check if any cards can be removed from the deck and placed on the table.
     */
    private void placeCardsOnTable() {
        blockPlayersTokens();
        System.out.println("placeCardsOnTable");
        shuffleDeck();
        if(env.util.findSets(deck, 1).size() == 0){
            terminate();
        }
        int slot = 0;

        for (int i = 0 ; i < this.table.grid.length; i++){
            for(int j = 0; j < this.table.grid[i].length; j++){
                if(!this.deck.isEmpty()){
                    int card = this.deck.remove(0);
                    this.table.grid[i][j] = card;
                    this.table. placeCard(card, slot);
                    slot++;
                }
            }
        }
        unblockPlayersTokens();

    }

    private void placeSpecificCardOnTable(int row, int column){
            int slot = column + this.table.grid[0].length * row;
            if(!this.deck.isEmpty()){
                int card = this.deck.remove(0);
                this.table.placeCard(card, slot);
                env.ui.placeCard(card, slot);
            }

    }

    /**
     * Sleep for a fixed amount of time or until the thread is awakened for some purpose.
     */
    private void sleepUntilWokenOrTimeout() {
        System.out.println("enter sleepUntilWokenOrTimeout");
        synchronized (this.isThereSet){
                try{
                    //while(this.playersQueue.isEmpty()) {
                        this.isThereSet.wait(10);
                    //}
                    //getSetToTest();
                }catch(InterruptedException e){
                    Thread.currentThread().interrupt();
                }
        }

    }

    /**
     * Reset and/or update the countdown and the countdown display.
     */
    private void updateTimerDisplay(boolean reset) {
       System.out.println("enters updateTimerDisplay");
        //reshuffleTime = System.currentTimeMillis() + env.config.turnTimeoutMillis;
        while(!Thread.currentThread().isInterrupted() && reshuffleTime - System.currentTimeMillis()>=0) {
            if (!reset){
                if (reshuffleTime - System.currentTimeMillis() > env.config.turnTimeoutWarningMillis) {
                    env.ui.setCountdown(Math.max(reshuffleTime - System.currentTimeMillis(), 0), false);

                    try {
                        Thread.sleep(1000);  // Pause for 1 second
                        getSetToTest();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();

                    }
                }
                else {
                    env.ui.setCountdown(reshuffleTime - System.currentTimeMillis(), true);
                    try {
                        Thread.sleep(500);  // Pause
                        getSetToTest();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();

                    }
                }

            }
            else{
                reshuffleTime = env.config.turnTimeoutMillis + System.currentTimeMillis();
                env.ui.setCountdown(env.config.turnTimeoutMillis, false);
            }
            }

        env.ui.setCountdown(0,true);

    }
    /**
     * Returns all the cards from the table to the deck.
     *

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

        int counter = 0;
        for (Player player : players) {
            if (player.score() == maxScore) {
                counter++;
            }
        }

        int [] winners = new int[counter];
        int numOfWinners = 0;
        for (Player player : players) {
            if (player.score() == maxScore) {
                winners[numOfWinners] = player.id;
                numOfWinners = numOfWinners + 1;
            }
        }
        env.ui.announceWinner(winners);

    }




    // The following methods are added for the purpose of the exercise.
    protected boolean testSet(int[] cards) {//we added it

        if(cards.length != 3) {
            return false;
        }
        boolean isSet = env.util.testSet(cards);
        System.out.println("player to testSet!" );
        if(isSet) {
            blockPlayersTokens();
            for (int i = 0; i < 3; i++) {
                int card = cards[i];
                int slot = this.table.cardToSlot[card];
                for (int row = 0; row < this.table.grid.length; row++) {
                    for (int col = 0; col < this.table.grid[0].length; col++) {
                        if (this.table.grid[row][col] == card) {
                            table.removeCard(slot);
//
//                            for (int j = 0; j < this.players.length; j++) {
//                                if (players[j].setsQueue.contains(card)) {
//                                    players[j].setsQueue.remove(card);
//                                    table.removeToken(players[j].id, slot);
//                                }
//                            }

                            if (!this.deck.isEmpty()) {
                                placeSpecificCardOnTable(row, col);
                            }

                        }

                    }
                }
            }
            unblockPlayersTokens();
            return true;
        }

        return false;

        }

    private void shuffleDeck(){
        Collections.shuffle(this.deck);
    }
    public void playerToQueue(Player player){
        System.out.println("player to Queue!" );
        this.playersQueue.add(player);
    }

    public void getSetToTest(){

        Iterator<Player> iterator = this.playersQueue.iterator();
        while (iterator.hasNext()) {
            Player player = iterator.next();
            List<Integer> tokens = this.table.playersToSlots[player.id];
            if(tokens.size()==3){
                if(testSet(player.setListToArray())){
                    player.point();
                    updateTimerDisplay(true);
                }

                else{
                    player.penalty();
                }
                iterator.remove();
            }
            else{
                iterator.remove();
            }

        }

    }

    private void blockPlayersTokens() {
        for (int i = 0; i < players.length; i++) {
            players[i].setLockTable(true);
        }
    }

    private void unblockPlayersTokens() {
        for (int i = 0; i < players.length; i++) {
            players[i].setLockTable(false);
        }
    }


}
