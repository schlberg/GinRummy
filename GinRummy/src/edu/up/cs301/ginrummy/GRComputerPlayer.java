package edu.up.cs301.ginrummy;

import java.util.Random;
import edu.up.cs301.card.Card;
import edu.up.cs301.game.GameComputerPlayer;
import edu.up.cs301.game.infoMsg.GameInfo;

/**
 * This is a computer player that slaps at an average rate given
 * by the constructor parameter.
 * 
 * @author Steven R. Vegdahl
 * @version December 2014
 * 
 * @author John Allen
 * @author Matthew Wellnitz
 * @author Eric Tsai
 * @author Jaimiey Sears
 */
public class GRComputerPlayer extends GameComputerPlayer {	
	public static final int THIS_PLAYER = 1;
	// the most recent state of the game
	private GRState savedState;
	private Random randDeck;
	private Random randCard;
	
	private int thisPlayer;
	
    /**
     * Constructor for the SJComputerPlayer class; creates an "average"
     * player.
     * 
     * @param name
     * 		the player's name
     */
    
    /*
     * Constructor for the SJComputerPlayer class
     */
    public GRComputerPlayer(String name) {
        // invoke superclass constructor
        super(name);
        randDeck = new Random();
        randCard = new Random();
        
    }
    
    
    /**
     * callback method, called when we receive a message, typically from
     * the game
     */
    @Override
    protected void receiveInfo(GameInfo info) {
    	
    	// if we don't have a game-state, ignore
    	if (!(info instanceof GRState)) {
    		return;
    	}
    	
    	// update our state variable
    	savedState = (GRState)info;
    	
    	thisPlayer = savedState.yourId;
    	
    	//Check if it's this players turn
    	if(savedState.whoseTurn() == thisPlayer){
    		//DRAW PHASE
    		if(savedState.getPhase() == GRState.DRAW_PHASE){
    			
    			if (randDeck == null) return;
    			// Draw a card from a random pile
    			boolean rd = randDeck.nextBoolean();
    			
    			try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    			game.sendAction(new GRDrawAction(this,rd));
    		} 
    		//DISCARD PHASE
    		else if (savedState.getPhase() == GRState.DISCARD_PHASE){
    			// Discard a random card
    			int rc = randCard.nextInt(10);
    			Card randomCard = savedState.getHand(thisPlayer).cards.get(rc);
    			
    			game.sendAction(new GRKnockAction(this,randomCard));
    			try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    			game.sendAction(new GRDiscardAction(this, randomCard));
    		}
    	}
    }
}
