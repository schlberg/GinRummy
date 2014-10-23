package com.example.ginrummy;

import edu.up.cs301.game.GameMainActivity;
import edu.up.cs301.game.LocalGame;
import edu.up.cs301.game.config.GameConfig;
import gameObjects.Card;
import android.support.v7.app.ActionBarActivity;
import android.animation.*;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.MenuItem;
import android.widget.LinearLayout;

public class GinRummy extends GameMainActivity {

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
		return null;
	}

	@Override
	public LocalGame createLocalGame() {
		// TODO Auto-generated method stub

		return null;
	}
}
