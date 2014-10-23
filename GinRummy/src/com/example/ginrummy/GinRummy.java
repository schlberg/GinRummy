package com.example.ginrummy;

import edu.up.cs301.game.config.GameConfig;
import edu.up.cs301.game.*;
import gameObjects.Card;
import android.widget.LinearLayout;

public class GinRummy extends GameMainActivity {

	private static final int PORT_NUMBER = 4752;

	@Override
	public GameConfig createDefaultConfig() {
		// TODO Auto-generated method stub
		//link the animation surface
		Animation.AnimationSurface gameBoard = (Animation.AnimationSurface)this.findViewById(R.id.animationSurface);
		CardAnimator animator = new CardAnimator();
		gameBoard.setAnimator(animator);

		//link the side bar containing the buttons and such
		LinearLayout sideBar = (LinearLayout)findViewById(R.id.sideBar);
		sideBar.setBackgroundColor(animator.backgroundColor());

		//initiate the card images
		Card.initImages(this);

		// Create a game configuration class for SlapJack
		GameConfig defaultConfig = new GameConfig(null, 2, 2, "Gin Rummy", PORT_NUMBER);

		return defaultConfig;
	}

	@Override
	public LocalGame createLocalGame() {
		// TODO Auto-generated method stub

		return null;
	}
}
