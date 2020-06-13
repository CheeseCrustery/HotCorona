package me.noodian.corona;

import me.noodian.corona.time.Ticking;
import org.bukkit.Particle;
import org.bukkit.entity.Snowball;

public class Sneeze implements Ticking {

	private final Snowball snowball;

	public Sneeze(Snowball snowball) {
		this.snowball = snowball;
	}

	@Override
	// Spawn particles
	public void tick() {
		Corona.getInstance().world.spawnParticle(Particle.SLIME, snowball.getLocation().getX(), snowball.getLocation().getY(), snowball.getLocation().getZ(), 1);
	}
}
