package me.noodian.corona;

import me.noodian.corona.time.Ticking;
import org.bukkit.Particle;
import org.bukkit.entity.Snowball;

import java.util.HashMap;

public class Sneeze extends Ticking {

	private static final int MIN_PARTICLES = 2, MAX_PARTICLES = 5;
	private static final HashMap<Snowball, Sneeze> instances = new HashMap<>();

	private final Snowball snowball;
	private int particleAmount;
	private int particleDelta;

	public Sneeze(Snowball snowball) {
		this.snowball = snowball;
		this.particleAmount = MIN_PARTICLES;
		this.particleDelta = 1;
		instances.put(snowball, this);
		start();
	}

	@Override
	// Spawn particles
	public void tick() {

		// Oscillating effect
		if (particleAmount >= MAX_PARTICLES) particleDelta = -1;
		else if (particleAmount <= MIN_PARTICLES) particleDelta = 1;
		particleAmount += particleDelta;

		Corona.get().world.spawnParticle(
				Particle.SLIME,
				snowball.getLocation(),
				particleAmount,
				0.1,
				0.1,
				0.1
		);
	}

	@Override
	// Remove self
	public void remove() {
		instances.remove(this.snowball);
		stop();
	}

	// Get the sneeze object of a snowball
	public static Sneeze get(Snowball snowball) {
		return instances.get(snowball);
	}
}
