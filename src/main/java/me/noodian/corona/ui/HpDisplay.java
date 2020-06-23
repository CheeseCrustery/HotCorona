package me.noodian.corona.ui;

import me.noodian.corona.Corona;
import me.noodian.corona.player.PlayerHandler;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;

public class HpDisplay extends UiDisplay {

	public HpDisplay(PlayerHandler owner) {
		this.owner = owner;
	}

	@Override
	// Display the hp values to the player
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

		// Only update if needed
		double targetHp = Math.ceil(20.0d * (double)ticks / initialTicks);
		if (owner.getPlayer().getHealth() != targetHp) {

			// Do not kill player
			if (targetHp == 0.0d) {
				AttributeInstance maxHealth = owner.getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH);
				if (maxHealth != null) owner.getPlayer().setHealth(maxHealth.getValue());
			}

			// Damage player
			else if (owner.getPlayer().getHealth() > targetHp) {
				owner.getPlayer().damage(owner.getPlayer().getHealth() - targetHp);
				Corona.get().world.playSound(owner.getPlayer().getEyeLocation(), Sound.ENTITY_ZOMBIE_HURT, 1.0f, 1.0f);
			}

			// Heal player
			else if (owner.getPlayer().getHealth() < targetHp) {
				owner.getPlayer().setHealth(targetHp);
			}
		}
	}
}
