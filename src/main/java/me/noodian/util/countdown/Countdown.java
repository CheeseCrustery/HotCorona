package me.noodian.util.countdown;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

public class Countdown {

	public Player player;
	public BarType type;

	private int count, current; // Time in ticks
	private final CountdownCallback callback;

	public Countdown(int count, CountdownCallback callback) {
		this.count = count;
		this.current = count;
		this.callback = callback;
	}

	public Countdown(int count, CountdownCallback callback, Player players, BarType type) {
		this.count = count;
		this.current = count;
		this.callback = callback;
		this.player = players;
		this.type = type;
	}

	// Set the current value
	public void setCurrent(int current) {
		if (current > count) {
			this.current = count;
			update();
		} else if (current >= 0) {
			this.current = current;
			update();
		}
	}

	// Set the maximum value
	public void setCount(int count) {
		if (count > 0) {
			if (current > count) {
				current = count;
			}

			this.count = count;
			update();
		}
	}

	// Decrease value and update
	public void tick() {
		if (current > 0) {
			current--;
			update();
		} else if (current == 0) {
			callback.Finished(this);
			current--;
		}
	}

	// Decrease value and update based on the boolean
	public void tick(boolean update) {
		if (current > 0) {
			current--;
			if (update)
				update();
		} else if (current == 0) {
			callback.Finished(this);
			current--;
		}
	}

	// Update the xp / health / boss bars of the player
	private void update() {

		switch(type) {
			case XP:
				player.setLevel(current);
				player.setExp((float)current / count);
				break;
			case HEALTH:
				double targetHp = Math.ceil(20.0d * (double)current / count);
				if (player.getHealth() != targetHp) {

					if (targetHp == 0.0d) {
						AttributeInstance maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
						if (maxHealth != null) player.setHealth(maxHealth.getValue());
					} else {
						// MAY NOT ACTUALLY DAMAGE PLAYER, NEEDS TESTING
						player.setHealth(targetHp);
						World world = Bukkit.getServer().getWorld("world");
						if (world != null)
							world.playSound(player.getEyeLocation(), Sound.ENTITY_ZOMBIE_HURT, 1.0f, 1.0f);
					}
				}
		}
	}
}