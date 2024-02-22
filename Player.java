package bguspl.set.ex;

import bguspl.set.Env;

import java.util.concurrent.ArrayBlockingQueue;

import java.util.concurrent.BlockingQueue;

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

    public ArrayBlockingQueue<Integer> setsQueue;
    Boolean isFrozen;


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
        setsQueue = new ArrayBlockingQueue<Integer>(3);
        this.isFrozen = false;
    }

    /**
     * The main player thread of each player starts here (main loop for the player thread).
     */
    @Override
    public void run() {
        this.playerThread = Thread.currentThread();
        while(!this.playerThread.isInterrupted()){
            env.logger.info("thread " + Thread.currentThread().getName() + " starting.");
            if (!human) createArtificialIntelligence();

            while (!terminate) {

                // TODO implement main player loop
            }
            if (!human) try { aiThread.join(); } catch (InterruptedException ignored) {}
            env.logger.info("thread " + Thread.currentThread().getName() + " terminated.");
        }
    }

    /**
     * Creates an additional thread for an AI (computer) player. The main loop of this thread repeatedly generates
     * key presses. If the queue of key presses is full, the thread waits until it is not full.
     */
    private void createArtificialIntelligence() {
        // note: this is a very, very smart AI (!)
        while(!this.aiThread.isInterrupted()){
            this.aiThread = new Thread(() -> {
                env.logger.info("thread " + Thread.currentThread().getName() + " starting.");
                while (!terminate) {
                    generateRandomSet();
                    try {
                        aiThread.sleep(1000);
                        this.isFrozen = true;
                    } catch (InterruptedException ignored) {}//need to change in case of interrupt

                }
                env.logger.info("thread " + Thread.currentThread().getName() + " terminated.");
            }, "computer-" + id);
            aiThread.start();
        }

    }

    /**
     * Called when the game should be terminated.
     */
    public void terminate() {
        this.terminate = true;
        if(!this.human){
            this.aiThread.interrupt();
        }

        this.playerThread.interrupt();

    }

    /**
     * This method is called when a key is pressed.
     *
     * @param slot - the slot corresponding to the key pressed.
     */
    public void keyPressed(int slot) {

        try{
            while(this.playerThread.getState() != Thread.State.RUNNABLE){
                this.table.wait();
            }
        }catch(InterruptedException e){
            this.playerThread.interrupt();
        }

        Integer card = table.getCard(slot);
        if(setsQueue.size() < 3 && !setsQueue.contains(slot) && card!=null) {
            //synchronized (this.dealer.lockTable) {
            setsQueue.offer(slot);
            table.placeToken(this.id, slot);
            //}
        } else if (setsQueue.contains(slot)) {
            setsQueue.remove(slot);
            table.removeToken(this.id, slot);
        }

        if (setsQueue.size() == 3) {
            this.dealer.playerToQueue(this);
            this.dealer.isThereSet.notifyAll();

        }


    }

    public int[] setQueueToArray(){
        Integer[] testSet = setsQueue.toArray(new Integer[3]);
        int[] testCards = new int[3];
        for (int i = 0; i < 3; i++) {
            testCards[i] = this.table.getCard(testSet[i]);
             }

        return testCards;
    }

    /**
     * Award a point to a player and perform other related actions.
     *
     * @post - the player's score is increased by 1.
     * @post - the player's score is updated in the ui.
     */
    public void point() {//sould we differ between Ai and human?

        try {
            env.ui.setFreeze(this.id,env.config.pointFreezeMillis);
            Thread.sleep(env.config.pointFreezeMillis);
            this.isFrozen = true;
        } catch (InterruptedException e) {
            env.ui.setFreeze(id,0);
            Thread.currentThread().interrupt();
        }

        int ignored = table.countCards(); // this part is just for demonstration in the unit tests
        env.ui.setScore(id, ++score);


    }

    /**
     * Penalize a player and perform other related actions.
     */
    public void penalty() {//sould we differ between Ai and human?

        try {
            env.ui.setFreeze(id,env.config.penaltyFreezeMillis);
            this.playerThread.sleep(env.config.penaltyFreezeMillis);
            this.isFrozen = true;
            env.ui.setFreeze(id,0);
        } catch (InterruptedException e) {
            this.playerThread.interrupt();
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
        while (setsQueue.size() < 3) {
            int [] randomSet = generateRandomNumber();
            int slot = randomSet[1] + this.table.grid[0].length * randomSet[0];
            Integer card = this.table.getCard(slot);

            if(setsQueue.size() < 3 && !setsQueue.contains(slot) && card!=null) {
                setsQueue.offer(slot);
                table.placeToken(this.id, slot);
            } else if (setsQueue.contains(slot)) {
                setsQueue.remove(slot);
                table.removeToken(this.id, slot);
            }

            //should be done in table?
            if (setsQueue.size() == 3) {
                this.dealer.playerToQueue(this);
            }


        }
    }
}
