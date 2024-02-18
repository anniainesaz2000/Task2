package bguspl.set.ex;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import bguspl.set.Env;

import java.util.concurrent.BlockingDeque;

/**
 * This class manages the players' threads and data
 *
 * @inv id >= 0
 * @inv score >= 0
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
    private Thread playerThread;

    /**
     * The thread of the AI (computer) player (an additional thread used to generate key presses).
     */
    private Thread aiThread;

    /**
     * True iff the player is human (not a computer player).
     */
    private final boolean human;

    /**
     * True iff game should be terminated.
     */
    private volatile boolean terminate;

    /**
     * The current score of the player.
     */
    private int score;



    // our new fields
    private Dealer dealer;

    public BlockingQueue<Integer> SetsQueue; // i added this line

    /**
     * The class constructor.
     *
     * @param env    - the environment object.
     * @param dealer - the dealer object.
     * @param table  - the table object.
     * @param id     - the id of the player.
     * @param human  - true iff the player is a human player (i.e. input is provided manually, via the keyboard).
     */
    public Player(Env env, Dealer dealer, Table table, int id, boolean human) {
        this.env = env;
        this.dealer = dealer;
        this.table = table;
        this.id = id;
        this.human = human;
        this.terminate = false;

//        if(human){
//            this.playerThread = new Thread(this, "player-" + id);
//        }
//        else {
//            this.aiThread = new Thread(this, "computer-" + id);
//        }

        SetsQueue = new ArrayBlockingQueue<Integer>(3);
    }

    /**
     * The main player thread of each player starts here (main loop for the player thread).
     */
    @Override
    public void run() {
        this.playerThread = Thread.currentThread();
        env.logger.info("thread " + Thread.currentThread().getName() + " starting.");
        if (!human) createArtificialIntelligence();

        while (!terminate) {
            //get input from player (create input manager?)
            // TODO implement main player loop
        }
        if (!human) try { aiThread.join(); } catch (InterruptedException ignored) {}
        env.logger.info("thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * Creates an additional thread for an AI (computer) player. The main loop of this thread repeatedly generates
     * key presses. If the queue of key presses is full, the thread waits until it is not full.
     */
    private void createArtificialIntelligence() {
        // note: this is a very, very smart AI (!)
        this.aiThread = new Thread(() -> {
            env.logger.info("thread " + Thread.currentThread().getName() + " starting.");
            while (!terminate) {
                // TODO implement player key press simulator
                try {
                    synchronized (this) { wait(); }
                } catch (InterruptedException ignored) {}
            }
            env.logger.info("thread " + Thread.currentThread().getName() + " terminated.");
        }, "computer-" + id);
        aiThread.start();
    }

    /**
     * Called when the game should be terminated.
     */
    public void terminate() {
        // TODO implement

    }

    /**
     * This method is called when a key is pressed.
     *
     * @param slot - the slot corresponding to the key pressed.
     */
    public void keyPressed(int slot) {
        // needed to use with thread

        Integer card = table.getCard(slot);
        if(SetsQueue.size() < 3 && !SetsQueue.contains(slot) && card!=null) {
            SetsQueue.add(slot);
            table.placeToken(this.id, slot);
        } else if (SetsQueue.contains(slot)) {
            SetsQueue.remove(slot);
            table.removeToken(this.id, slot);
        }

        //should be done in table?
        if (SetsQueue.size() == 3) {
            Integer[] testSet = SetsQueue.toArray(new Integer[3]);
            int[] testSetInt = new int[3];
            for (int i = 0; i < 3; i++) {
                testSetInt[i] = testSet[i].intValue();
            }
            if (dealer.testSet(testSetInt)) {
                point();
            } else {
                penalty();
//                for(Integer token: SetsQueue){
//                    env.ui.removeToken(this.id, this.table.getSlot(token));
//                }
//                SetsQueue.clear();
            }

        }

    }

    /**
     * Award a point to a player and perform other related actions.
     *
     * @post - the player's score is increased by 1.
     * @post - the player's score is updated in the ui.
     */
    public void point() {//Anni asks a question
        // TODO implement
        // the player getting frozen fo 1 second
        try {
            env.ui.setFreeze(this.id,env.config.pointFreezeMillis);
            Thread.sleep(env.config.pointFreezeMillis);
        } catch (InterruptedException e) {
            env.ui.setFreeze(id,0);
            Thread.currentThread().interrupt();
        }

        int ignored = table.countCards(); // this part is just for demonstration in the unit tests
        env.ui.setScore(id, ++score);

        //should we add here dealer.removeCardsFromTable()


    }

    /**
     * Penalize a player and perform other related actions.
     */
    public void penalty() {
        // TODO implement

        try {
            env.ui.setFreeze(id,env.config.penaltyFreezeMillis);
            Thread.sleep(env.config.penaltyFreezeMillis);
        } catch (InterruptedException e) {
            env.ui.setFreeze(id,0);
            Thread.currentThread().interrupt();
        }
    }

    public int score() {
        return score;
    }


    public int [] generateRandomNumber() {
        int [] randomSet = new int[2];
        int a = (int) (Math.random() * table.grid.length);
        int b = (int) (Math.random() * table.grid[0].length);
        randomSet[0] = a;
        randomSet[1] = b;
        return randomSet;
    }

    public void generateRandomSet() {
        while (SetsQueue.size() < 3) {
            int [] randomSet = generateRandomNumber();
            int card = table.getCard(table.getCardgrid(randomSet[0], randomSet[1]));
            int slot = table.getSlot(card);
            if(SetsQueue.size() < 3 && !SetsQueue.contains(card)) {
                SetsQueue.add(card);
                table.placeToken(id, table.getSlot(card));
                env.ui.placeToken(this.id,slot);
            } else if (SetsQueue.contains(card) == true) {
                SetsQueue.remove(card);
                table.removeToken(id, table.getSlot(card));
                env.ui.removeToken(this.id,slot);
            }
            if (SetsQueue.size() == 3) {
                Integer[] testSet = SetsQueue.toArray(new Integer[3]);
                int[] testSetInt = new int[3];
                for (int i = 0; i < 3; i++) {
                    testSetInt[i] = testSet[i].intValue();
                }
                if (dealer.testSet(testSetInt)) {
                    point();
                } else {
                    penalty();
                }
                for(int i = 0; i < 3; i++) {
                    table.removeToken(id, table.getSlot(testSetInt[i]));
                    env.ui.removeToken(this.id,slot);
                }
                SetsQueue.clear();

            }

        }
    }
}


