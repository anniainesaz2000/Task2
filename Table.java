package bguspl.set.ex;

import bguspl.set.Env;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * This class contains the data that is visible to the player.
 *
 * @inv slotToCard[x] == y iff cardToSlot[y] == x
 */
public class Table {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Mapping between a slot and the card placed in it (null if none).
     */
    protected final Integer[] slotToCard; // card per slot (if any)

    /**
     * Mapping between a card and the slot it is in (null if none).
     */
    protected final Integer[] cardToSlot; // slot per card (if any)
    public Integer [] [] grid;
    protected ArrayList[] slotsToPlayers;
    //protected List<Integer>[] playersToSlots;


    /**
     * Constructor for testing.
     *
     * @param env        - the game environment objects.
     * @param slotToCard - mapping between a slot and the card placed in it (null if none).
     * @param cardToSlot - mapping between a card and the slot it is in (null if none).
     */
    public Table(Env env, Integer[] slotToCard, Integer[] cardToSlot) {

        this.env = env;
        this.slotToCard = slotToCard;
        this.cardToSlot = cardToSlot;
        this.slotsToPlayers = new ArrayList[slotToCard.length];  //array of slots which contains array of players
        //this.playersToSlots = new ArrayList[this.de.length];


        this.grid = new Integer[env.config.rows][env.config.columns];

    }

    /**
     * Constructor for actual usage.
     *
     * @param env - the game environment objects.
     */
    public Table(Env env) {

        this(env, new Integer[env.config.tableSize], new Integer[env.config.deckSize]);
    }

    /**
     * This method prints all possible legal sets of cards that are currently on the table.
     */
    public void hints() {
        List<Integer> deck = Arrays.stream(slotToCard).filter(Objects::nonNull).collect(Collectors.toList());
        env.util.findSets(deck, Integer.MAX_VALUE).forEach(set -> {
            StringBuilder sb = new StringBuilder().append("Hint: Set found: ");
            List<Integer> slots = Arrays.stream(set).mapToObj(card -> cardToSlot[card]).sorted().collect(Collectors.toList());
            int[][] features = env.util.cardsToFeatures(set);
            System.out.println(sb.append("slots: ").append(slots).append(" features: ").append(Arrays.deepToString(features)));
        });
    }

    /**
     * Count the number of cards currently on the table.
     *
     * @return - the number of cards on the table.
     */
    public int countCards() {
        int cards = 0;
        for (Integer card : slotToCard)
            if (card != null)
                ++cards;
        return cards;
    }

    /**
     * Places a card on the table in a grid slot.
     * @param card - the card id to place in the slot.
     * @param slot - the slot in which the card should be placed.
     *
     * @post - the card placed is on the table, in the assigned slot.
     */
    public void placeCard(int card, int slot) {//maybe synchronized

        cardToSlot[card] = slot;
        slotToCard[slot] = card;
        env.ui.placeCard(card, slot);

        boolean keepLoop = true;
        for(int row = 0; row < this.grid.length && keepLoop; row++) {
            for (int col = 0; col < this.grid[0].length && keepLoop; col++) {
                if (this.grid[row][col] == null) {
                    this.grid[row][col] = card;
                    keepLoop = false;
                }

            }

        }
    }

    /**
     * Removes a card from a grid slot on the table.
     * @param slot - the slot from which to remove the card.
     */
    public void removeCard(int slot) {//maybe synchronized

        int card = slotToCard[slot];
        slotToCard[slot] = null;
        cardToSlot[card] = null;
        slotsToPlayers[slot]=null;
        env.ui.removeTokens(slot);
        env.ui.removeCard(slot);

        boolean keepLoop = true;
        for(int row = 0; row < this.grid.length && keepLoop; row++){
            for(int col = 0; col < this.grid[0].length && keepLoop; col++){
                if(this.grid[row][col] == card){
                    this.grid[row][col] = null;
                    keepLoop = false;
                }

            }

        }

    }

    /**
     * Places a player token on a grid slot.
     * @param player - the player the token belongs to.
     * @param slot   - the slot on which to place the token.
     */
    public void placeToken(int player, int slot) {//check about player who place token on empty slot
        if(slotToCard[slot] != null){
            if(slotsToPlayers[slot] == null){
                slotsToPlayers[slot] = new ArrayList();
            }
            slotsToPlayers[slot].add(player);
            env.ui.placeToken(player, slot);
        }


    }

    /**
     * Removes a token of a player from a grid slot.
     * @param player - the player the token belongs to.
     * @param slot   - the slot from which to remove the token.
     * @return       - true iff a token was successfully removed.
     */
    public boolean removeToken(int player, int slot) {//check about player who place token on empty slot
        if (slotsToPlayers[slot] != null) {
            slotsToPlayers[slot].remove(player);
            env.ui.removeToken(player, slot);
            return true;
        }
        //should we have playersTSlot? if yes then update it here too
        return false;
    }


    // getCard method
    public Integer getCard(int slot) {
        return this.slotToCard[slot];
    }

    public Integer getSlot(int card) {
        return this.cardToSlot[card];
    }

    public void removeAllCardsFromTable(){

        for(int slot = 0; slot < cardToSlot.length; slot++){
            removeCard(slot);
        }
    }

    public Integer[] getSlotToCard(){
        return this.slotToCard;
    }




}
