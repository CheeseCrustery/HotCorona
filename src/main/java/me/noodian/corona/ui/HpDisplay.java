package me.noodian.corona.ui;

import me.noodian.corona.Game;
import me.noodian.corona.player.PlayerHandler;
import org.bukkit.Sound;
import org.bukkit.attribute.*;

public class HpDisplay extends Display {

	public HpDisplay(PlayerHandler owner) {
		this.owner = owner;
	}

	@Override
	// Display the hp values to the player
	public void update(Object... args) {

		// Get input
		double targetHp;
		if (args.length == 2) {
			double ticks = (double) args[0];
			double initialTicks = (double) args[1];
			targetHp = Math.ceil(20.0d * (double)ticks / initialTicks);
		} else {
			targetHp = 20.0d;
		}

		// Only update if needed
		if (owner.getPlayer().getHealth() != targetHp) {

			// Do not kill player
			if (targetHp == 0.0d) {
				AttributeInstance maxHealth = owner.getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH);
				if (maxHealth != null) owner.getPlayer().setHealth(maxHealth.getValue());
			}

			// Damage player
			else if (owner.getPlayer().getHealth() > targetHp) {
				owner.getPlayer().damage(owner.getPlayer().getHealth() - targetHp);
				Game.get().getCurrentWorld().playSound(owner.getPlayer().getEyeLocation(), Sound.ENTITY_ZOMBIE_HURT, 1.0f, 1.0f);
			}

			// Heal player
			else if (owner.getPlayer().getHealth() < targetHp) {
				owner.getPlayer().setHealth(targetHp);
			}
		}
	}
}
