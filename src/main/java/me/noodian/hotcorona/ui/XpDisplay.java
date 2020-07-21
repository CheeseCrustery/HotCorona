package me.noodian.corona.ui;

import me.noodian.corona.player.PlayerHandler;
import org.bukkit.Sound;

public class XpDisplay extends Display {

	private final Sound tick, finish;
	private int previousLevel;

	public XpDisplay(PlayerHandler owner) {
		this.owner = owner;
		this.tick = null;
		this.finish = null;
		this.previousLevel = 0;
	}

	public XpDisplay(PlayerHandler owner, Sound tick, Sound finish) {
		this.owner = owner;
		this.tick = tick;
		this.finish = finish;
		this.previousLevel = 0;
	}

	@Override
	// Display the xp values to the player
	public void update(Object... args) {

		// Get input
		int level;
		float exp;
		if (args.length == 2 && (int)args[0] >= 0 && (int)args[1] > 0) {
			int ticks = (int)args[0];
			int initialTicks = (int)args[1];
			level = (int) Math.ceil((double)ticks / 20);
			exp = (float)ticks / initialTicks;
		} else {
			level = 0;
			exp = 0.0f;
		}

		// Set xp bar
		owner.getPlayer().setLevel(level);
		owner.getPlayer().setExp(exp);

		// Play sound
		if (level != previousLevel) {
			if (finish != null && level == 0)
				owner.getPlayer().playSound(owner.getPlayer().getEyeLocation(), finish, 1, 1);
			else if (tick != null)
				owner.getPlayer().playSound(owner.getPlayer().getEyeLocation(), tick, 1, 1);
			previousLevel = level;
		}
	}
}
