package me.noodian.corona.ui;

import me.noodian.corona.player.PlayerHandler;

public class XpDisplay extends UiDisplay {

	public XpDisplay(PlayerHandler owner) {
		this.owner = owner;
	}

	@Override
	// Display the xp values to the player
	public void update(Object... args) {

		// Get input
		int ticks, initialTicks;
		if (args.length == 2) {
			ticks = (int) args[0];
			initialTicks = (int) args[1];
		} else {
			ticks = 0;
			initialTicks = 0;
		}

		// Set xp bar
		int current = (int) Math.ceil((double)ticks / 20);
		owner.getPlayer().setLevel(current);
		owner.getPlayer().setExp((float)ticks / initialTicks);
	}
}
