package edu.up.cs301.card;

import edu.up.cs301.card.*;
import android.graphics.*;

public class CardPath {
	
	private static float animationDuration = 50; //how long the animation will take, default 50 ticks
	
	//instance variables
	private Card card;
	private PointF origin;
	private PointF destination;
	private RectF location;
	private int progress; //the number of ticks that have elapsed
	
	public CardPath(Card card, PointF origin, PointF destination) {
		this.card = card;
		this.origin = origin;
		this.destination = destination;
		progress = 0;
	}

	public Card getCard() {
		return card;
	}

	public void setCard(Card card) {
		this.card = card;
	}

	public PointF getOrigin() {
		return origin;
	}
	
	public void setAnimationSpeed(float speed) {
		this.animationDuration = speed;
	}
	
	public float getAnimationSpeed() {
		return animationDuration;
	}

	public PointF getDestination() {
		return destination;
	}

	public void setDestination(PointF desination) {
		this.destination = desination;
	}

	/**
	 * advances the animation by one tick
	 * @return the updated location of the card
	 */
	public RectF advance() {
		//advance the card linearly along the path
		//find the change amount, in pixels
		float dx = origin.x + (destination.x - origin.x)/animationDuration*progress;
		float dy = origin.y + (destination.y - origin.y)/animationDuration*progress;
		
		progress++;
		
		//set the location of the card
		location = new RectF(dx,dy,dx + CardAnimator.getCardDimensions().x,
				dy + CardAnimator.getCardDimensions().y);
		
		return location;
	}
	
	
	/**
	 * indicates whether the animation is complete
	 * @return TRUE if the path is complete
	 */
	public boolean isComplete() {
		return progress == animationDuration;
	}

	/**
	 * draws the card at the appropriate location
	 * @param canvas
	 */
	public void drawOn(Canvas canvas) {
		card.drawOn(canvas, location);
	}
}
