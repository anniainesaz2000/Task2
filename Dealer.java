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
    private long reshuffleTime = 60000;
    public Queue<Player> playersQueue;




    public Dealer(Env env, Table table, Player[] players) {
        this.env = env;
        this.table = table;
        this.players = players;
        deck = IntStream.range(0, env.config.deckSize).boxed().collect(Collectors.toList());
        this.terminate = false;
        this.playersQueue = new LinkedList<>();
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
            //if timer == 0 and, break
            //how to shuffle cards?
            //should we add timer field?
            timerLoop();
            //check if there are legal sets deck + table (60 sec or less?)
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
            //sleepUntilWokenOrTimeout(); should happen in threads
            updateTimerDisplay(false);
            removeCardsFromTable();
            //if should finish
            placeCardsOnTable();
        }
    }

    /**
     * Called when the game should be terminated.
     */
    public void terminate() {//x
        List<Integer> cardsOnTable = createListFromArray(this.table.grid);
        if((env.util.findSets(cardsOnTable, 1).isEmpty()) && (env.util.findSets(this.deck, 1).isEmpty())) {
            terminate = true;
        }

        // TODO implement
    }

    public static List<Integer> createListFromArray(Integer[][] array) {
        List<Integer> list = new ArrayList<>();
        for (Integer[] row : array) {
            for (Integer element : row) {
                list.add(element);
            }
        }
        return list;
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
        //why do we need it if we remove it in testSet

        // TODO implement

    }

    /**
     * Check if any cards can be removed from the deck and placed on the table.
     */
    private void placeCardsOnTable() {
        shuffleDeck();
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
        // TODO implement
    }

    /**
     * Reset and/or update the countdown and the countdown display.
     */
    private void updateTimerDisplay(boolean reset) {//should we update current time?
        int currentTime = 60000;  // Initial time in milliseconds (60 seconds)

        // Update time each second
        for (int i = 0; i < 60; i++) {
            env.ui.setCountdown(currentTime, reset);
            currentTime -= 1000;  // Subtract 1000 milliseconds (1 second)
            try {
                Thread.sleep(1000);  // Pause for 1 second
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Returns all the cards from the table to the deck.
     */
    private void removeAllCardsFromTable() {//Anni made some changes + asks a question

        //is there another occasion in which we need to remove all? maybe every 60 sec?
        table.removeAllCardsFromTable();
        for(Player player : players){
            player.setsQueue.clear();
        }
        //do we need to change the players table too or is it the same table?

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
    protected synchronized boolean testSet(int[] cards) {//we added it

        if(cards.length != 3) {
            return false;
        }
        boolean isSet = env.util.testSet(cards);
        if(isSet) {
                for (int i = 0; i < 3; i++) {
                    int card = cards[i];
                    int slot = this.table.cardToSlot[card];
                    for(int row = 0; row < this.table.grid.length; row++){
                        for(int col = 0; col < this.table.grid[0].length; col++){
                            if(this.table.grid[row][col] == card){
                                table.removeCard(slot);
                                for(int j = 0; j<this.players.length; j++){
                                    if(players[j].setsQueue.contains(card)){
                                        players[j].setsQueue.remove(card);
                                        table.removeToken(players[j].id, slot);
                                    }
                                }
                                if (!this.deck.isEmpty()){
                                    placeSpecificCardOnTable(row, col);
                                }

                            }
                        }
                    }

                }
            return true;
            }
        return false;

        }

    private void shuffleDeck(){//Anni
        Collections.shuffle(this.deck);
    }
    public void playerToQueue(Player player){
        playersQueue.add(player);
    }

    public void getSetToTest(){

        Iterator<Player> iterator = this.playersQueue.iterator();
        while (iterator.hasNext()) {
            Player player = iterator.next();
            if(player.setsQueue.size()==3){
                if(testSet(player.setQueueToArray())){
                    player.point();
                }

                else{
                    player.penalty();
                }
            }
            else{
                iterator.remove();
            }

        }

    }


}
