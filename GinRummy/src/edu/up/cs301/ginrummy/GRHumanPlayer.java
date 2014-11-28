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
	Button exitButton;

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

	// protected static PointF playerHandPos[] = new PointF[2];

	// player hand positions
	protected ArrayList<PointF> p1handPos, p2handPos;


	//ERIC: Player 1's melds
	private ArrayList<Meld> p1Melds;

	//whether the GUI is locked or not
	private boolean lockGUI;	

	/**
	 * constructor
	 * 
	 * @param name
	 *            the player's name
	 */
	public GRHumanPlayer(String name) {
		super(name);
		p1handPos = new ArrayList<PointF>();
		p2handPos = new ArrayList<PointF>();
		// ERIC
		// touchedCard = new backCard();
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
			oppScore.setText("Opponent Score: "
					+ ((Integer) state.getp2score()).toString());
			myScore.setText("Your Score: "
					+ ((Integer) state.getp1score()).toString());

			//if hand is over show an appropriate message
			if (state.isEndOfRound) {
				//lock the gui so cards cannot be moved
				lockGUI = true;
				messagePane.setText("Round over.\nTouch anywhere to see scores!");
				return;
			}

			lockGUI = false;
			//state messages
			if (state.whoseTurn() == 0){
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

		// initialize the text fields
		oppScore = (TextView) activity.findViewById(R.id.opponentScore);
		myScore = (TextView) activity.findViewById(R.id.playerScore);
		messagePane = (TextView) activity.findViewById(R.id.messagePane);


		// set up exit button listener
		exitButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				MessageBox.popUpChoice("Are you sure?", 
						"Yes, I admit that I am a poor sport and still want to continue.", 
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

		// read in the card images
		backCard.initImages(activity);
		Card.initImages(activity);

		// set the location of the decks
		knockPos = new PointF(0.1f, 0.25f);
		stockPos = new PointF(0.3f, 0.25f);
		discardPos = new PointF(0.5f, 0.25f);

		//initially unlock the GUI
		lockGUI = false;

		// playerHandPos[0] = new PointF(0.0f,0.75f);
		// playerHandPos[1] = new PointF(0.6f,-0.25f);

		// if the state is not null, simulate having just received the state so
		// that
		// any state-related processing is done
		if (state != null) {
			receiveInfo(state);
		}
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
		// 1/20 of a second
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
		if(false){
		//ERIC: START: IF THE END OF ROUND, SHOW MELDS 
//		if (state.isEndOfRound) {
//			p1Melds = state.getMeldsForPlayer(0);		
//			p1handPos.clear();
//			synchronized (this) {
//				//Iterate through each group of melds
//				//"melds" is a meld in "p1Melds"
//				for (Meld meld : p1Melds) {
//					int indexOfMeld = p1Melds.indexOf(meld);
//					//Iterate through each card in a meld
//					//"meldCard" is a card in "melds"
//					for (Card meldCard : meld.getMeldCards()) {
//
//						int indexOfMeldCard = meld.getMeldCards().indexOf(meldCard);							
//						p1handPos.add(new PointF(0.05f + HAND_CARD_OFFSET*indexOfMeldCard,
//								0.75f +HAND_CARD_OFFSET*indexOfMeld));
//						meldCard.drawOn(canvas, adjustDimens(p1handPos.get(indexOfMeldCard)));							
//					}						
//				}								
//			}
		}
		else{
			// draw the player's hands
			p1handPos.clear();
			ArrayList<Card> hand = stateCopy.getHand(0).cards;
			synchronized (this) {
				for (Card card : hand) {

					int n = hand.indexOf(card);
					p1handPos.add(new PointF(0.05f + HAND_CARD_OFFSET * n, 0.75f));

					// draw the card, if it is not being dragged or animated
					if ((touchedCard != null && touchedCard.equals(card))
							|| (path != null && path.getCard().equals(card))) {
						// don't draw the card
					} else {
						// draw the card
						card.drawOn(canvas, adjustDimens(p1handPos.get(n)));
					}
				}
			}

			p2handPos.clear();
			// draw the opponent's hand
			synchronized (this) {
				Deck copy = stateCopy.getHand(1);
				ArrayList<PointF> copypos = p2handPos;
				for (Card card : copy.cards) {
					int n = copy.cards.indexOf(card);
					copypos.add(new PointF(0.55f - HAND_CARD_OFFSET * n, -0.25f));
					// add a few pixels to the position

					// draw the card
					// TODO thread bug is here
					card.drawOn(canvas, adjustDimens(copypos.get(n)));

				}
			}
		}


		// get the information from the state
		Deck decks[] = { stateCopy.getStock(), stateCopy.getDiscard() };
		PointF deckPos[] = { stockPos, discardPos };

		// draw the stock and discard piles
		for (int idx = decks.length - 1; idx >= 0; idx--) {
			Deck deck = decks[idx];
			for (Card card : deck.cards) {
				int n = deck.cards.indexOf(card);

				// add a few pixels to the position
				RectF position = adjustDimens(deckPos[idx]);
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

		// draw the knocking box
		Paint p = new Paint();
		p.setStyle(Paint.Style.STROKE);
		p.setColor(Color.GREEN);
		p.setTextSize(24);
		canvas.drawText("KNOCK", knockPos.x * surface.getWidth()
				+ getCardDimensions().x / 6, knockPos.y * surface.getHeight()
				+ getCardDimensions().y / 2, p);
		canvas.drawRect(adjustDimens(knockPos), p);

		if (path != null) {
			// advance the card along the path
			PointF newPos = path.advance();

			// draw the moving card
			if (newPos != null)
				path.getCard().drawOn(canvas, adjustDimens(newPos));

			// if the animation is done, remove the animation
			if (path != null && path.isComplete())
				path = null;
		}

		// draw the card being dragged
		if (touchedCard != null && touchedPos != null) {
			touchedCard.drawOn(canvas, adjustDimens(touchedPos));
		}

	}//tick

	/**
	 * callback method: we have received a touch on the animation surface
	 * 
	 * @param event
	 *            the motion-event
	 * @return
	 */
	public void onTouch(MotionEvent event) {

		//if the GUI is locked, it means we are at the end of the round
		//and touching the board anywhere should show the round end dialog
		if (lockGUI) {
			String msg = String.format("Round Over.\nYour Score: %d\n Your Opponent's Score: %d" , 
					state.getp1score(), state.getp2score());

			if (event.getAction() != MotionEvent.ACTION_DOWN) return;
			//message box to show at the end of the round
			//TODO: Get the message from the state;
			//TODO: get john to put a message in the state
			synchronized(this){
			MessageBox.popUpChoice(msg, "Next Round", "View Table",

					//listener for when the "next round" button is pressed
					new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface dialog, int which) {
					// start a new round
					nextRound();
				}},

				//listener for when the "View Table" button is pressed 
				new DialogInterface.OnClickListener(){
					public void onClick(DialogInterface dialog, int which) {
						//do nothing, return to table
					}},
					myActivity); //pop-up choice

			}
			return;
		}

		// get the location of the touch on the surface
		int touchX = (int) event.getX();
		int touchY = (int) event.getY();

		// on down touch events:
		if (event.getAction() == MotionEvent.ACTION_DOWN) {

			// check for dragging
			if (state.getPhase() == GRState.DISCARD_PHASE) {

			}// discard_phase

			// check for draw
			else if (state.getPhase() == GRState.DRAW_PHASE) {

				// draw from the stock pile
				if (adjustDimens(stockPos).contains(touchX, touchY)) {
					game.sendAction(new GRDrawAction(this, true));
				}

				// draw from the discard pile
				else if (adjustDimens(discardPos).contains(touchX, touchY)) {
					game.sendAction(new GRDrawAction(this, false));
				}

			}// draw_phase

			// check each card hand position to see if it was touched
			for (PointF p : p1handPos) {

				if (adjustDimens(p).contains(touchX, touchY)) {
					int i = p1handPos.indexOf(p);

					// select the card
					touchedCard = state.getHand(0).cards.get(i);
					touchedPos = new PointF(
							((float) touchX - getCardDimensions().x / 2)
							/ (float) surface.getWidth(),
							((float) touchY - getCardDimensions().y / 2)
							/ (float) surface.getHeight());
					originPos = p;

					// cancel any animation
					path = null;
				}
			}
		}// ACTION_DOWN

		// When we release our finger and the card is hovered over the discard
		else if (event.getAction() == MotionEvent.ACTION_UP) {
			if (touchedCard != null && touchedPos != null) {



				//START: ERIC: Implementing rearranging cards in hand
				int i = state.getHand(0).cards.indexOf(touchedCard);
				state.getHand(0).cards.remove(touchedCard);
				synchronized(this) {
					for (PointF p : p1handPos) {

						if (adjustDimens(p).contains(touchX, touchY)) {
							//find index of card that we're dragging card to
							i = p1handPos.indexOf(p);	

							//create new path to new location for card
							originPos.x = touchX;
							originPos.y = touchY;
						}
					}

					//ERIC: Replace card being hovered over with dragged card				
					state.getHand(0).cards.add(i, touchedCard);
				}
				//END: ERIC: Implementing rearranging cards in hand


				// check for discard
				if (adjustDimens(discardPos).contains(touchX, touchY)) {
					// discard the selected card
					game.sendAction(new GRDiscardAction(this, touchedCard));
				}

				// check for knock
				else if (adjustDimens(knockPos).contains(touchX, touchY)) {
					// knock with the selected card
					game.sendAction(new GRKnockAction(this, touchedCard));
				}

				else {
					// move the touched card back to origin
					CardPath newPath = new CardPath(touchedCard, touchedPos,
							originPos);
					newPath.setAnimationSpeed(5);
					path = newPath;
				}

				// reset the dragged card
				touchedCard = null;
			}
		} else {
			// ERIC: when we move a card, move it from its center
			// touchedX = touchX - (int)getCardDimensions().x/2;
			// touchedY = touchY - (int)getCardDimensions().y/2;
			// set screen relative position of the dragged card
			touchedPos = new PointF(
					((float) touchX - getCardDimensions().x / 2)
					/ (float) surface.getWidth(),
					((float) touchY - getCardDimensions().y / 2)
					/ (float) surface.getHeight());
		}
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
}
