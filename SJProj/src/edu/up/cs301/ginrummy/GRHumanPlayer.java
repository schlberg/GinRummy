package edu.up.cs301.ginrummy;

import java.util.ArrayList;

import android.app.Activity;
import android.graphics.*;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.LinearLayout;
import edu.up.cs301.animation.*;
import edu.up.cs301.card.*;
import edu.up.cs301.game.GameHumanPlayer;
import edu.up.cs301.game.GameMainActivity;
import edu.up.cs301.game.R;
import edu.up.cs301.game.infoMsg.GameInfo;
import edu.up.cs301.game.infoMsg.GameState;
import edu.up.cs301.game.infoMsg.IllegalMoveInfo;
import edu.up.cs301.game.infoMsg.NotYourTurnInfo;

/**
 * A GUI that allows a human to play Slapjack. Moves are made by clicking
 * regions on a surface. Presently, it is laid out for landscape orientation.
 * If the device is held in portrait mode, the cards will be very long and
 * skinny.
 * 
 * @author Steven R. Vegdahl
 * @version July 2013
 */
public class GRHumanPlayer extends GameHumanPlayer implements Animator {

	// sizes and locations of card decks and cards, expressed as percentages
	// of the screen height and width
	private final static float CARD_HEIGHT_PERCENT = 50; // height of a card
	private final static float CARD_WIDTH_PERCENT = 17; // width of a card
	private final static float LEFT_BORDER_PERCENT = 4; // width of left border
	private final static float RIGHT_BORDER_PERCENT = 20; // width of right border
	private final static float VERTICAL_BORDER_PERCENT = 4; // width of top/bottom borders
	
	private final static float STACKED_CARD_OFFSET = 0.001F;

	// our game state
	protected GRState state;

	// our activity
	private Activity myActivity;

	// the animation surface
	private AnimationSurface surface;
	int width = 0;
	int height = 0;

	//card information
	private ArrayList<CardPath> paths;

	//the width and hieght of the card
	private static PointF cardDimensions;
	private static float cardDimensionModifier = 0.75f;

	//the positions of the decks
	protected static PointF stockPos;
	protected static PointF discardPos;
	protected static PointF p1hand;
	protected static PointF p2hand;

	// the background color
	private int backgroundColor;

	/**
	 * constructor
	 * 
	 * @param name
	 * 		the player's name
	 * @param bkColor
	 * 		the background color
	 */
	public GRHumanPlayer(String name) {
		super(name);
		backgroundColor = 0xff278734;
	}

	/**
	 * callback method: we have received a message from the game
	 * 
	 * @param info
	 * 		the message we have received from the game
	 */
	@Override
	public void receiveInfo(GameInfo info) {
		Log.i("SJComputerPlayer", "receiving updated state ("+info.getClass()+")");
		if (info instanceof IllegalMoveInfo || info instanceof NotYourTurnInfo) {
			// if we had an out-of-turn or illegal move, flash the screen
			surface.flash(Color.RED, 50);
		}
		else if (!(info instanceof GRState)) {
			// otherwise, if it's not a game-state message, ignore
			return;
		}
		else {
			// it's a game-state object: update the state. Since we have an animation
			// going, there is no need to explicitly display anything. That will happen
			// at the next animation-tick, which should occur within 1/20 of a second
			this.state = (GRState)info;
			Log.i("human player", "receiving");
		}
	}

	/**
	 * call-back method: called whenever the GUI has changed (e.g., at the beginning
	 * of the game, or when the screen orientation changes).
	 * 
	 * @param activity
	 * 		the current activity
	 */
	public void setAsGui(GameMainActivity activity) {

		// Load the layout resource for the new configuration
		activity.setContentView(R.layout.activity_gin_rummy);

		// link the animator to the animation surface
		surface = (AnimationSurface) activity.findViewById(R.id.animationSurface);
		surface.setAnimator(this);

		//ERIC
		// read in the card images
		backCard.initImages(activity);
		Card.initImages(activity);

		paths = new ArrayList<CardPath>();

		//set the default size of the card
		cardDimensions = new PointF(261*cardDimensionModifier, 379*cardDimensionModifier);

		//set the location of the decks
		stockPos = new PointF(0.3f,0.25f);
		discardPos = new PointF(0.5f,0.25f);
		p1hand = new PointF(0.0f,0.75f);
		p2hand = new PointF(0.0f,-0.25f);

		// if the state is not null, simulate having just received the state so that
		// any state-related processing is done
		if (state != null) {
			state.getStock().add(new backCard());
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
	 * @return
	 * 		the amimation interval, in milliseconds
	 */
	public int interval() {
		// 1/20 of a second
		return 50;
	}

	/**
	 * @return
	 * 		the background color
	 */
	public int backgroundColor() {
		return backgroundColor;
	}

	/**
	 * @return
	 * 		whether the animation should be paused
	 */
	public boolean doPause() {
		return false;
	}

	/**
	 * @return
	 * 		whether the animation should be terminated
	 */
	public boolean doQuit() {
		return false;
	}

	/**
	 * callback-method: we have gotten an animation "tick"; redraw the screen image:
	 * - the middle deck, with the top card face-up, others face-down
	 * - the two players' decks, with all cards face-down
	 * - a red bar to indicate whose turn it is
	 * 
	 * @param g
	 * 		the canvas on which we are to draw
	 */
	public void tick(Canvas canvas) {


		//TODO Remove this!
		state = new GRState();
		state.getDiscard().cards.add(new backCard());

		// ignore if we have not yet received the game state
		if (state == null) return;

		// get the height and width of the animation surface
		height = surface.getHeight();
		width = surface.getWidth();

		//get the information from the state
		Deck decks[] = {state.getStock(),state.getDiscard(),state.getHand(0),state.getHand(1)};
		PointF deckPos[] = {stockPos, discardPos, p1hand, p2hand};

		//draw the static cards
		for (int idx = 0; idx < decks.length; idx++){
			Deck deck = decks[idx];
			for (Card card : deck.cards) {
				int n = deck.cards.indexOf(card);
				
				//add a few pixels to the position
				RectF position = adjustDimens(deckPos[idx]);
				position.set(new RectF(position.left + STACKED_CARD_OFFSET*position.width()*n, position.top - STACKED_CARD_OFFSET*position.height()*n,
						position.right + STACKED_CARD_OFFSET*position.width()*n, position.bottom - STACKED_CARD_OFFSET*position.height()*n));
				
				//draw the card
				card.drawOn(canvas, position);
			}
		}

		//advance and draw the card paths
		for (CardPath path : paths)  {
			int idx = paths.indexOf(path);

			//advance the card along the path
			RectF newPos = path.advance();

			//draw the card
			path.drawOn(canvas);

			if (path.isComplete()) {
				//if the animation is done, get the card back and end the animation
				path.getOriginDeck().add(path.getCard());
				paths.remove(idx);
			}
		}


		//		// draw the opponent's cards, face down
		//		RectF oppTopLocation = opponentTopCardLocation(); // drawing size/location
		//		drawCardBacks(g, oppTopLocation,
		//				0.0025f*width, -0.01f*height, state.getDeck(1-this.playerNum).size());

		//		// draw my cards, face down
		//		RectF thisTopLocation = thisPlayerTopCardLocation(); // drawing size/location
		//		drawCardBacks(g, thisTopLocation,
		//				0.0025f*width, -0.01f*height, state.getDeck(this.playerNum).size());

		//		// draw a red bar to denote which player is to play (flip) a card
		//		RectF currentPlayerRect =
		//				state.toPlay() == this.playerNum ? thisTopLocation : oppTopLocation;
		//		RectF turnIndicator =
		//				new RectF(currentPlayerRect.left,
		//						currentPlayerRect.bottom,
		//						currentPlayerRect.right,
		//					height);
		//		Paint paint = new Paint();
		//		paint.setColor(Color.RED);
		//		g.drawRect(turnIndicator, paint);
	}

	//	/**
	//	 * @return
	//	 * 		the rectangle that represents the location on the drawing
	//	 * 		surface where the top card in the opponent's deck is to
	//	 * 		be drawn
	//	 */
	//	private RectF opponentTopCardLocation() {
	//		// near the left-bottom of the drawing surface, based on the height
	//		// and width, and the percentages defined above
	//		int width = surface.getWidth();
	//		int height = surface.getHeight();
	//		return new RectF(LEFT_BORDER_PERCENT*width/100f,
	//				(100-VERTICAL_BORDER_PERCENT-CARD_HEIGHT_PERCENT)*height/100f,
	//				(LEFT_BORDER_PERCENT+CARD_WIDTH_PERCENT)*width/100f,
	//				(100-VERTICAL_BORDER_PERCENT)*height/100f);
	//	}

	//	/**
	//	 * @return
	//	 * 		the rectangle that represents the location on the drawing
	//	 * 		surface where the top card in the current player's deck is to
	//	 * 		be drawn
	//	 */	
	//	private RectF thisPlayerTopCardLocation() {
	//		// near the right-bottom of the drawing surface, based on the height
	//		// and width, and the percentages defined above
	//		int width = surface.getWidth();
	//		int height = surface.getHeight();
	//		return new RectF((100-RIGHT_BORDER_PERCENT-CARD_WIDTH_PERCENT)*width/100f,
	//				(100-VERTICAL_BORDER_PERCENT-CARD_HEIGHT_PERCENT)*height/100f,
	//				(100-RIGHT_BORDER_PERCENT)*width/100f,
	//				(100-VERTICAL_BORDER_PERCENT)*height/100f);
	//	}

	//	/**
	//	 * @return TODO DELETE WHEN DONE
	//	 * 		the rectangle that represents the location on the drawing
	//	 * 		surface where the top card in the middle pile is to
	//	 * 		be drawn
	//	 */	
	//	private RectF middlePileTopCardLocation() {
	//		// near the middle-bottom of the drawing surface, based on the height
	//		// and width, and the percentages defined above
	//		int height = surface.getHeight();
	//		int width = surface.getWidth();
	//		float rectLeft = (100-CARD_WIDTH_PERCENT+LEFT_BORDER_PERCENT-RIGHT_BORDER_PERCENT)*width/200;
	//		float rectRight = rectLeft + width*CARD_WIDTH_PERCENT/100;
	//		float rectTop = (100-VERTICAL_BORDER_PERCENT-CARD_HEIGHT_PERCENT)*height/100f;
	//		float rectBottom = (100-VERTICAL_BORDER_PERCENT)*height/100f;
	//		return new RectF(rectLeft, rectTop, rectRight, rectBottom);
	//	}

	//	/** TODO delete when done
	//	 * draws a sequence of card-backs, each offset a bit from the previous one, so that all can be
	//	 * seen to some extent
	//	 * 
	//	 * @param g
	//	 * 		the canvas to draw on
	//	 * @param topRect
	//	 * 		the rectangle that defines the location of the top card (and the size of all
	//	 * 		the cards
	//	 * @param deltaX
	//	 * 		the horizontal change between the drawing position of two consecutive cards
	//	 * @param deltaY
	//	 * 		the vertical change between the drawing position of two consecutive cards
	//	 * @param numCards
	//	 * 		the number of card-backs to draw
	//	 */
	//	private void drawCardBacks(Canvas g, RectF topRect, float deltaX, float deltaY,
	//			int numCards) {
	//		// loop through from back to front, drawing a card-back in each location
	//		for (int i = numCards-1; i >= 0; i--) {
	//			// determine theh position of this card's top/left corner
	//			float left = topRect.left + i*deltaX;
	//			float top = topRect.top + i*deltaY;
	//			// draw a card-back (hence null) into the appropriate rectangle
	//			drawCard(g,
	//					new RectF(left, top, left + topRect.width(), top + topRect.height()),
	//					null);
	//		}
	//	}

	/**
	 * callback method: we have received a touch on the animation surface
	 * 
	 * @param event
	 * 		the motion-event
	 * @return 
	 */
	public void onTouch(MotionEvent event) {

		//		// ignore everything except down-touch events
		//		if (event.getAction() != MotionEvent.ACTION_DOWN) return;
		//
		//		// get the location of the touch on the surface
		//		int touchX = (int) event.getX();
		//		int touchY = (int) event.getY();
		//
		//		if (adjustDimens(stockPos).contains(touchX, touchY)) {
		//			//draw from the stock pile
		//			game.sendAction(new GRDrawAction(this, true));
		//		}
		//		//		else if (middleTopCardLoc.contains(x, y)) {
		//		//			// it's on the middlel pile: we're slapping a card: send
		//		//			// action to the game
		//		//			game.sendAction(new GRSlapAction(this));
		//		//		}
		//		else {
		//			// illegal touch-location: flash for 1/20 second
		//			surface.flash(Color.RED, 50);
		//		}
	}

	//	/** TODO: delete this when done
	//	 * draws a card on the canvas; if the card is null, draw a card-back
	//	 * 
	//	 * @param g
	//	 * 		the canvas object
	//	 * @param rect
	//	 * 		a rectangle defining the location to draw the card
	//	 * @param c
	//	 * 		the card to draw; if null, a card-back is drawn
	//	 */
	//	private static void drawCard(Canvas g, RectF rect, Card c) {
	//		if (c == null || c instanceof backCard) {
	//			// null: draw a card-back, consisting of a blue card
	//			// with a white line near the border. We implement this
	//			// by drawing 3 concentric rectangles:
	//			// - blue, full-size
	//			// - white, slightly smaller
	//			// - blue, even slightly smaller
	//			
	//			/*Paint white = new Paint();
	//			white.setColor(Color.WHITE);
	//			Paint blue = new Paint();
	//			blue.setColor(Color.BLUE);
	//			RectF inner1 = scaledBy(rect, 0.96f); // scaled by 96%
	//			RectF inner2 = scaledBy(rect, 0.98f); // scaled by 98%
	//			g.drawRect(rect, blue); // outer rectangle: blue
	//			g.drawRect(inner2, white); // middle rectangle: white
	//			g.drawRect(inner1, blue); // inner rectangle: blue
	//*/			
	//			//ERIC
	//			backCard selectBackCard = new backCard();
	//			selectBackCard.drawOn(g, rect);
	//			
	//		}
	//		else {
	//			// just draw the card
	//			c.drawOn(g, rect);
	//		}
	//	}
	//	
	//	/** TODO: DELETE WHEN DONE
	//	 * scales a rectangle, moving all edges with respect to its center
	//	 * 
	//	 * @param rect
	//	 * 		the original rectangle
	//	 * @param factor
	//	 * 		the scaling factor
	//	 * @return
	//	 * 		the scaled rectangle
	//	 */
	//	private static RectF scaledBy(RectF rect, float factor) {
	//		// compute the edge locations of the original rectangle, but with
	//		// the middle of the rectangle moved to the origin
	//		float midX = (rect.left+rect.right)/2;
	//		float midY = (rect.top+rect.bottom)/2;
	//		float left = rect.left-midX;
	//		float right = rect.right-midX;
	//		float top = rect.top-midY;
	//		float bottom = rect.bottom-midY;
	//		
	//		// scale each side; move back so that center is in original location
	//		left = left*factor + midX;
	//		right = right*factor + midX;
	//		top = top*factor + midY;
	//		bottom = bottom*factor + midY;
	//		
	//		// create/return the new rectangle
	//		return new RectF(left, top, right, bottom);
	//	}


	private RectF adjustDimens(PointF location) {

		//get the relative position of the card
		float x = location.x * width;
		float y = location.y * height;

		//get the size of the card
		PointF dimens = getCardDimensions();

		//set the card boundary and return
		RectF adjustedRect = new RectF(x, y, x + dimens.x, y + dimens.y);
		return adjustedRect;
	}

	/**
	 * gets the size (in pixels) of our cards
	 * @return
	 */
	private PointF getCardDimensions() {
		return cardDimensions;
	}
}