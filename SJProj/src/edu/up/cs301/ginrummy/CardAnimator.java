package edu.up.cs301.ginrummy;

import java.util.ArrayList;
import edu.up.cs301.card.*;
import android.graphics.*;
import android.view.MotionEvent;

public class CardAnimator implements edu.up.cs301.animation.Animator {

	private static final int FRAME_INTERVAL = 10;
	private static final int BACKGROUND_COLOR = 0xff278734;

	private ArrayList<CardPath> paths;
	protected ArrayList<Card> cards;
	protected ArrayList<RectF> cardPos;

	//the width and hieght of the card
	private static PointF cardDimensions;

	public static PointF getCardDimensions() {
		return cardDimensions;
	}

	public static void setCardDimensions(PointF cardDimensions) {
		CardAnimator.cardDimensions = cardDimensions;
	}

	//constructor
	public CardAnimator() {
		super();

		//initialize the arraylists
		cards = new ArrayList<Card>();
		cardPos = new ArrayList<RectF>();
		paths = new ArrayList<CardPath>();

		//set the default size of the card
		cardDimensions = new PointF(261, 379);

		//create a new card
		cards.add(new Card(Rank.ACE, Suit.Spade));
		cardPos.add(new RectF(0,0,cardDimensions.x,cardDimensions.y));
	}

	public int interval() {
		return FRAME_INTERVAL;
	}

	public int backgroundColor() {
		return BACKGROUND_COLOR;
	}

	public boolean doPause() {
		return false;
	}

	public boolean doQuit() {
		return false;
	}

	public void tick(Canvas canvas) {

		//draw the static cards
		for (Card card : cards) {
			int idx = cards.indexOf(card);

			//draw the card
			card.drawOn(canvas, cardPos.get(idx));
		}

		//advance and draw the card paths
		for (CardPath path : paths)  {
			int idx = paths.indexOf(path);

			//advance the card along the path
			RectF newPos = path.advance();

			//draw the card
			path.drawOn(canvas);

			//update the location information
			cardPos.set(idx, newPos);

			if (path.isComplete()) {
				//if the animation is done, get the card back and end the animation
				cards.add(path.getCard());
				paths.remove(idx);
			}
		}
	}

	public void onTouch(MotionEvent event) {
		//get the location of the touch
		float x = event.getX();
		float y = event.getY();

		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			//TODO find which card we are touching and select that one
			int touchedCardIdx = -1;
			for (RectF position : cardPos) {
				if (position.contains(x, y)) {
					touchedCardIdx = cardPos.indexOf(position);
				}
				//by the end of this loop, we will know the TOP card which was touched
			}
			//if no card was touched
			if (touchedCardIdx == -1) return;

			//get the original position of the card
			float origX = cardPos.get(touchedCardIdx).left;
			float origY = cardPos.get(touchedCardIdx).top;
			
			//move the card to the tapped location along a path
			paths.add(new CardPath(cards.get(touchedCardIdx), new PointF(origX, origY), new PointF((float)Math.random()*500, (float)Math.random()*500)));
			cards.remove(cards.get(touchedCardIdx));
		}

	}
}
