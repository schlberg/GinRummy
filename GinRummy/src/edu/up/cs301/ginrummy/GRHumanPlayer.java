package edu.up.cs301.ginrummy;

import java.util.ArrayList;
import android.app.Activity;
import android.content.DialogInterface;
import android.graphics.*;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import edu.up.cs301.animation.*;
import edu.up.cs301.card.*;
import edu.up.cs301.game.*;
import edu.up.cs301.game.infoMsg.*;
import edu.up.cs301.game.util.MessageBox;

/**
 * A GUI that allows a human to play Gin Rummy. Moves are made by clicking
 * regions on a surface. Presently, it is laid out for landscape orientation. If
 * the device is held in portrait mode, the cards will be very long and skinny.
 * 
 * @author Steven R. Vegdahl
 * @version July 2013
 * 
 * @author Jaimiey Sears
 * @author Eric Tsai
 * @version November 2014
 */
public class GRHumanPlayer extends GameHumanPlayer implements Animator {

	// how much a card on top of another should be offset by
	private final static float STACKED_CARD_OFFSET = 0.005F;  	
	private final static float HAND_CARD_OFFSET = 0.06F;			//ERIC: originally 0.055F

	// the width and height of the card images
	private final static PointF CARD_DIMENSIONS = new PointF(500, 726);
	// the size a card should be grown or shrunk by
	// TODO: for device cross-compatibility, make this change based on canvas
	// size
	private static float CARD_DIMENSION_MODIFIER = 0.4f;

	// colors used
	public static final int FELT_GREEN = 0xff277714;
	public static final int LAKE_ERIE = 0xff6183A6;

	// our game state
	protected GRState state;

	// our activity
	private Activity myActivity;

	// the animation surface
	private AnimationSurface surface;

	// the knock and exit buttons
	Button exitButton, newGame;

	// the score and message pane text fields
	private TextView oppScore, myScore, messagePane; 

	// moving card information
	CardPath path;

	// ERIC: card being moved
	private Card touchedCard;

	// ERIC: touched Coordinates
	private PointF touchedPos, originPos;

	// the positions of the decks
	protected static PointF stockPos, discardPos, knockPos;

	//positions of the players' hands
	protected static ArrayList<ArrayList<PointF>> playerHandPos = new ArrayList<ArrayList<PointF>>();

	//card order in this player's hand
	private ArrayList<Card> handOrder;

	//ERIC: Player 1's melds
	private ArrayList<Meld> p1Melds;

	//whether the GUI is locked or not
	private boolean lockGUI;
	
	//what are the player indices
	private int myIdx, otherIdx;

	/**
	 * constructor
	 * 
	 * @param name
	 *            the player's name
	 */
	public GRHumanPlayer(String name) {
		super(name);

		//initialize the hand-positions of the players
		playerHandPos = new ArrayList<ArrayList<PointF>>();
		playerHandPos.add(new ArrayList<PointF>());
		playerHandPos.add(new ArrayList<PointF>());

		//initialize the card order
		handOrder = new ArrayList<Card>();
	}

	/**
	 * callback method: we have received a message from the game
	 * 
	 * @param info
	 *            the message we have received from the game
	 */
	@Override
	public void receiveInfo(GameInfo info) {
		Log.i("GRComputerPlayer", "receiving updated state (" + info.getClass()
				+ ")");
		if (info instanceof IllegalMoveInfo || info instanceof NotYourTurnInfo) {
			// if we had an out-of-turn or illegal move, flash the screen
			surface.flash(Color.RED, 10);
		} else if (!(info instanceof GRState)) {
			// otherwise, if it's not a game-state message, ignore
			return;
		} else {
			// it's a game-state object: update the state. Since we have an
			// animation
			// going, there is no need to explicitly display anything. That will
			// happen
			// at the next animation-tick, which should occur within 1/20 of a
			// second
			this.state = (GRState) info;
			Log.i("human player", "receiving");

			//score messages
			int score1 = 
					(myIdx == 0 ? state.getp1score() : state.getp2score());
			int score2 = 
					(otherIdx == 0 ? state.getp1score() : state.getp2score());
			oppScore.setText("Opponent Score: " + Integer.toString(score2));
			myScore.setText("Your Score: " + Integer.toString(score1));
			//make the new game invisible
			newGame.setVisibility(View.INVISIBLE);

			//if hand is over show an appropriate message
			if (state.isEndOfRound) {
				//lock the gui so cards cannot be moved
				lockGUI = true;
				messagePane.setText("Round over.\nTouch anywhere to see scores!");
				return;
			}

			lockGUI = false;
			//state messages
			if (state.whoseTurn() == myIdx){
				if (state.getPhase() == GRState.DRAW_PHASE) {
					messagePane.setText("It's Your Turn:\nDraw a card.");
				}
				else if (state.getPhase() == GRState.DISCARD_PHASE) {
					messagePane.setText("It's Your Turn:\nDiscard a Card.");
				}
			}else{
				messagePane.setText("Your opponent is taking their turn.");
			}
		}
	}

	/**
	 * call-back method: called whenever the GUI has changed (e.g., at the
	 * beginning of the game, or when the screen orientation changes).
	 * 
	 * @param activity
	 *            the current activity
	 */
	public void setAsGui(GameMainActivity activity) {

		this.myActivity = activity;

		// Load the layout resource for the new configuration
		activity.setContentView(R.layout.activity_gin_rummy);

		// link the animator to the animation surface
		surface = (AnimationSurface) activity
				.findViewById(R.id.animationSurface);
		surface.setAnimator(this);

		// initialize the buttons
		exitButton = (Button) activity.findViewById(R.id.exitButton);
		newGame = (Button) activity.findViewById(R.id.newGame);

		// initialize the text fields
		oppScore = (TextView) activity.findViewById(R.id.opponentScore);
		myScore = (TextView) activity.findViewById(R.id.playerScore);
		messagePane = (TextView) activity.findViewById(R.id.messagePane);

		// set up exit button listener
		exitButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				MessageBox.popUpChoice("Are you sure?", 
						"Yes, I admit that I am a poor sport and still want to exit.", 
						"No, keep playing the awesome game!",

						//listener for "yes"
						new DialogInterface.OnClickListener(){
					public void onClick(DialogInterface dialog, int which) {
						// quit the game
						System.exit(0);
					}},

					//listener for "no"
					new DialogInterface.OnClickListener(){
						public void onClick(DialogInterface dialog, int which) {
							//do nothing, return to game
						}},
						myActivity); //pop-up choice
			}
		});

		//set up new game button listener
		newGame.setOnClickListener(new OnClickListener(){
			public void onClick(View v) {
				// TODO Auto-generated method stub
				newGame();
			}
		});

		// read in the card images
		backCard.initImages(activity);
		Card.initImages(activity);

		// set the location of the decks
		knockPos = new PointF(0.1f, 0.25f);
		stockPos = new PointF(0.3f, 0.25f);
		discardPos = new PointF(stockPos.x + 0.2f, 0.25f);

		//initially unlock the GUI
		lockGUI = false;

		// if the state is not null, simulate having just received the state so
		// that any state-related processing is done
		if (state != null) {
			receiveInfo(state);
		}
	}

	@Override
	protected void gameIsOver(String msg) {
		super.gameIsOver(msg);

		//cause the game button to be visible
		newGame.setVisibility(View.VISIBLE);
	}

	/**
	 * @return the top GUI view
	 */
	@Override
	public View getTopView() {
		return myActivity.findViewById(R.id.top_gui_layout);
	}

	/**
	 * @return the animation interval, in milliseconds
	 */
	public int interval() {
		// 1/200 of a second
		return 5;
	}

	/**
	 * @return the background color
	 */
	public int backgroundColor() {
		return FELT_GREEN;
	}

	/**
	 * @return whether the animation should be paused
	 */
	public boolean doPause() {
		return false;
	}

	/**
	 * @return whether the animation should be terminated
	 */
	public boolean doQuit() {
		return false;
	}



	/**
	 * callback-method: we have gotten an animation "tick"; redraw the screen
	 * image: - the middle deck, with the top card face-up, others face-down -
	 * the two players' decks, with all cards face-down - a red bar to indicate
	 * whose turn it is
	 * 
	 * @param g
	 *            the canvas on which we are to draw
	 */
	public void tick(Canvas canvas) {
		// ignore if we have not yet received the game state
		if (state == null)
			return;

		GRState stateCopy = new GRState(state);

		//find out my id
		myIdx = state.yourId;
		otherIdx = (myIdx == 0 ? 1 : 0);

		//		if(false){
		//			//ERIC: START: IF THE END OF ROUND, SHOW MELDS 
		//			//		if (state.isEndOfRound) {
		//			//			p1Melds = state.getMeldsForPlayer(0);		
		//			//			p1handPos.clear();
		//			//			synchronized (this) {
		//			//				//Iterate through each group of melds
		//			//				//"melds" is a meld in "p1Melds"
		//			//				for (Meld meld : p1Melds) {
		//			//					int indexOfMeld = p1Melds.indexOf(meld);
		//			//					//Iterate through each card in a meld
		//			//					//"meldCard" is a card in "melds"
		//			//					for (Card meldCard : meld.getMeldCards()) {
		//			//
		//			//						int indexOfMeldCard = meld.getMeldCards().indexOf(meldCard);							
		//			//						p1handPos.add(new PointF(0.05f + HAND_CARD_OFFSET*indexOfMeldCard,
		//			//								0.75f +HAND_CARD_OFFSET*indexOfMeld));
		//			//						meldCard.drawOn(canvas, adjustDimens(p1handPos.get(indexOfMeldCard)));							
		//			//					}						
		//			//				}								
		//			//			}
		//		}


		ArrayList<Card> myHand = state.getHand(myIdx).cards;
		//if card is not in hand Order, but is supposed to be, add it
		for (Card c : myHand) { 
			if (!handOrder.contains(c)) handOrder.add(c);
		}

		//if card is in hand order which is not supposed to be, remove it
		for (Card c : new ArrayList<Card>(handOrder)) {
			if (!myHand.contains(c)) handOrder.remove(c);
		}

		//empty the hand positions for repopulation
		playerHandPos.get(myIdx).clear();
		playerHandPos.get(otherIdx).clear();

		//set up the position of all the cards in the hands
		for (int i = 0; i < handOrder.size(); i++) {
			playerHandPos.get(myIdx).add(new PointF(0.05f + HAND_CARD_OFFSET*i, 0.75f));
		}
		for (int i = 0; i < stateCopy.getHand(1).cards.size(); i++) {
			playerHandPos.get(otherIdx).add(new PointF(0.55f - HAND_CARD_OFFSET*i, -0.25f));
		}

		//draw the hands
		drawHand(canvas, new ArrayList<Card>(handOrder), playerHandPos.get(myIdx));
		drawHand(canvas, stateCopy.getHand(1).cards, playerHandPos.get(otherIdx));

		//draw the stock and discard piles
		drawDeck(canvas, stateCopy.getStock(), stockPos);
		drawDeck(canvas, stateCopy.getDiscard(), discardPos);

		// draw the knocking box
		drawKnockBox(canvas, Color.GREEN);

		if (path != null) {
			// advance the card along the path
			PointF newPos = path.advance();

			// draw the moving card
			if (newPos != null)
				path.getCard().drawOn(canvas, adjustDimens(newPos));

			// if the animation is done, remove the animation
			if (path != null && path.isComplete()) path = null;
		}

		// draw the card being dragged
		if (touchedCard != null && touchedPos != null) {
			touchedCard.drawOn(canvas, adjustDimens(touchedPos));
		}

	}//tick

	/**
	 * Draw the knocking box onto the canvas
	 * @param canvas
	 * 			the Canvas to paint on
	 * @param col
	 * 			the integer color the box should be
	 */
	private void drawKnockBox(Canvas canvas, int col) {
		Paint p = new Paint();
		p.setStyle(Paint.Style.STROKE);
		p.setColor(col);
		p.setTextSize(24);

		canvas.drawText("KNOCK", knockPos.x * surface.getWidth()
				+ getCardDimensions().x / 6, knockPos.y * surface.getHeight()
				+ getCardDimensions().y / 2, p);

		canvas.drawRoundRect(adjustDimens(knockPos), 10F, 10F, p);		
	}

	/**
	 * Draw the specified deck as a stack of cards
	 * @param canvas
	 * 			the canvas to paint on
	 * @param deck
	 * 			the Deck of cards to draw
	 * @param pos
	 * 			a PointF of the location to draw the deck
	 */
	synchronized private void drawDeck(Canvas canvas, Deck deck, PointF pos) {
		// draw the stack of cards
		for (Card card : deck.cards) {
			int n = deck.cards.indexOf(card);

			// add a few pixels to the position
			RectF position = adjustDimens(pos);
			position.set(new RectF(position.left + STACKED_CARD_OFFSET
					* position.width() * n, position.top
					- STACKED_CARD_OFFSET * position.height() * n,
					position.right + STACKED_CARD_OFFSET * position.width()
					* n, position.bottom - STACKED_CARD_OFFSET
					* position.height() * n));

			// draw the card
			card.drawOn(canvas, position);
		}

	}

	/**
	 * Draws the specified arrayList of Cards onto the specified arrayList of positions
	 * @param canvas
	 * 			The canvas to draw on
	 * @param hand
	 * 			The hand to draw
	 * @param pos
	 * 			Where to draw the hand
	 */
	synchronized private void drawHand(Canvas canvas, ArrayList<Card> hand, ArrayList<PointF> pos) {
		for (PointF p : pos) {
			int n = pos.indexOf(p);
			Card card = hand.get(n);

			// draw the card, if it is not being dragged or animated
			if (card != null && (touchedCard != null && touchedCard.equals(card))
					|| (path != null && path.getCard().equals(card))) {
				// don't draw the card
			} else {
				// draw the card
				card.drawOn(canvas, adjustDimens(p));
			}
		}
	}

	/**
	 * callback method: we have received a touch on the animation surface
	 * 
	 * @param event
	 *            the motion-event
	 * @return
	 */
	public void onTouch(MotionEvent event) {

		GRState stateCopy = new GRState(state);
		//if the GUI is locked, it means we are at the end of the round
		//and touching the board anywhere should show the round end dialog
		if (lockGUI) {
			String msg = String.format("Round Over.\nYour Score: %d\n Your Opponent's Score: %d" , 
					stateCopy.getp1score(), stateCopy.getp2score());

			if (event.getAction() != MotionEvent.ACTION_DOWN) return;
			//message box to show at the end of the round
			//TODO: Get the message from the state;
			//TODO: get john to put a message in the state
			MessageBox.popUpChoice(msg, "Next Round", "Back to Melds",

					//listener for when the "next round" button is pressed
					new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface dialog, int which) {
					// start a new round
					nextRound();
					handOrder.clear();
				}},

				//listener for when the "Back to Melds" button is pressed 
				new DialogInterface.OnClickListener(){
					public void onClick(DialogInterface dialog, int which) {
						//do nothing, return to table
					}},
					myActivity); //pop-up choice

			return;
		}

		// get the location of the touch on the surface
		int touchX = (int) event.getX();
		int touchY = (int) event.getY();

		// on down touch events:
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			poke(touchX, touchY);
		} // ACTION_DOWN

		// on finger-lift events
		else if (event.getAction() == MotionEvent.ACTION_UP) {
			if (touchedCard != null) drop(touchedCard, touchX, touchY);
		} // ACTION_UP

		else {
			// when we move a card, move it (from its center)
			// set screen relative position of the dragged card
			touchedPos = new PointF(
					((float) touchX - getCardDimensions().x / 2)
					/ (float) surface.getWidth(),
					((float) touchY - getCardDimensions().y / 2)
					/ (float) surface.getHeight());
		}
	}// onTouch

	/**
	 * Handles dropping a card onto the specified location
	 * @param card
	 * 			the Card object to drop
	 * @param x
	 * 			the integer x-coordinate of the drop 
	 * @param touchY
	 * 			the integer y-coordinate of the drop
	 */
	synchronized private void drop(Card card, int x, int y) {
		if (card != null) {
			//dropped on discard pile
			if (adjustDimens(discardPos).contains(x, y)) {
				//discard the card
				game.sendAction(new GRDiscardAction(this, card));
			}

			//dropped on knock box
			else if (adjustDimens(knockPos).contains(x, y)) {
				game.sendAction(new GRKnockAction(this, card));
			}

			//dropped on hand
			else if (handContains(playerHandPos.get(myIdx), x, y)){
				//rearrange cards in hand
				int dest = playerHandPos.get(myIdx).indexOf(card);
				for (PointF p : playerHandPos.get(myIdx)) {
					//if this card was touched
					if (adjustDimens(p).contains(x, y))
						dest = playerHandPos.get(myIdx).indexOf(p);
				}

				//move the card
				handOrder.remove(card);
				handOrder.add(dest, card);
			}

			//dropped somewhere else
			else{
				// have the card move back to its origin
				CardPath newPath = new CardPath(card, touchedPos, originPos);
				newPath.setAnimationSpeed(5);
				path = newPath;
			}

			//nullify the touched card so we don't draw it
			touchedCard = null;
			touchedPos = null;
		}
	}

	/**
	 *TODO
	 * @param touchX
	 * @param touchY
	 */
	synchronized private void poke(int x, int y) {

		//stock poked
		if (adjustDimens(stockPos).contains(x, y)) {
			if (state.getPhase() == GRState.DRAW_PHASE)
				game.sendAction(new GRDrawAction(this, true));
		}

		//discard poked
		else if (adjustDimens(discardPos).contains(x, y)){
			if (state.getPhase() == GRState.DRAW_PHASE)
				game.sendAction(new GRDrawAction(this, false));
		}

		//hand poked
		else if (handContains(playerHandPos.get(myIdx), x, y)) {
			int origin = 0;
			for (PointF p : playerHandPos.get(myIdx)) {
				//if this card was touched
				if (adjustDimens(p).contains(x, y)) {
					origin = playerHandPos.get(myIdx).indexOf(p);
					originPos = p;
				}
			}

			//pick up the card
			touchedCard = handOrder.get(origin);
		}
	}//poke

	/**
	 * TODO
	 * @param posList
	 * @param x
	 * @param y
	 * @return
	 */
	synchronized private boolean handContains(ArrayList<PointF> posList, int x, int y) {
		for (PointF p : posList) {
			if (adjustDimens(p).contains(x, y)) return true;
		}
		
		return false;
	}

	/**
	 * gets the size (in pixels) of our cards
	 * 
	 * @return a PointF containing the scaled size of the cards.
	 */
	public static PointF getCardDimensions() {
		return new PointF(CARD_DIMENSIONS.x * CARD_DIMENSION_MODIFIER,
				CARD_DIMENSIONS.y * CARD_DIMENSION_MODIFIER);
	}

	/**
	 * 
	 * @param location
	 *            a PointF which describes the location(in screen percent) where
	 *            the card will be drawn
	 * @return a RectF describing the boundary where the card will be drawn
	 */
	private RectF adjustDimens(PointF location) {

		// get the relative position of the card
		float x = location.x * surface.getWidth();
		float y = location.y * surface.getHeight();

		// get the size of the card
		PointF dimens = getCardDimensions();

		// set the card boundary and return
		RectF adjustedRect = new RectF(x, y, x + dimens.x, y + dimens.y);
		return adjustedRect;
	}

	/**
	 * requests to move to the next round
	 */
	private void nextRound() {
		// TODO Auto-generated method stub
		game.sendAction(new GRNextRoundAction(this));
	}

	/**
	 * requests to start a new game
	 */
	private void newGame() {
		game.sendAction(new GRNewGameAction(this));
	}
}
