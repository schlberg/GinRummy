package com.example.ginrummy;

import gameObjects.Card;
import gameObjects.Rank;
import gameObjects.Suit;
import android.graphics.*;
import android.view.MotionEvent;

public class CardAnimator implements Animation.Animator {

	private static final int FRAME_INTERVAL = 10;
	private static final int BACKGROUND_COLOR = 0xff278734;
	
	protected Card card;
	protected RectF cardPos;
	private PointF cardDimensions;
	
	public CardAnimator() {
		super();
		cardDimensions = new PointF(261, 379);
	}
	
	@Override
	public int interval() {
		return FRAME_INTERVAL;
	}

	@Override
	public int backgroundColor() {
		return BACKGROUND_COLOR;
	}

	@Override
	public boolean doPause() {
		return false;
	}

	@Override
	public boolean doQuit() {
		return false;
	}

	@Override
	public void tick(Canvas canvas) {
		// TODO Auto-generated method stub
		
		if (card != null && cardPos != null) card.drawOn(canvas, cardPos);
	}

	@Override
	public void onTouch(MotionEvent event) {
		// TODO Auto-generated method stub
		
		int x = (int)(event.getX());
		int y = (int)(event.getY());
		
		card = new Card(Rank.ACE, Suit.Spade);
		cardPos = new RectF(x,y,x + cardDimensions.x, y + cardDimensions.y);
	}
	
	/**
	 * 
	 * @param c
	 * @param orgX
	 * @param orgY
	 * @param destX
	 * @param destY
	 */
	public void slideCard(Card c, int orgX, int orgY, int destX, int destY) {
		//calculate the distance along the path in x and y
	}

}
