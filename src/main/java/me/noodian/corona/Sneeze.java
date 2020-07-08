package me.noodian.corona;

import me.noodian.corona.player.PlayerHandler;
import me.noodian.corona.player.PlayerState;
import me.noodian.corona.time.Ticking;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;

public class Sneeze extends Ticking implements Listener {

	private static final int MIN_PARTICLES = 2, MAX_PARTICLES = 5;

	private final Snowball snowball;
	private int particleAmount;
	private int particleDelta;

	public Sneeze(Snowball snowball) {
		this.snowball = snowball;
		this.particleAmount = MIN_PARTICLES;
		this.particleDelta = 1;
		
		Game.get().getServer().getPluginManager().registerEvents(this, Game.get());
		start();
	}

	@Override
	// Spawn particles
	public void tick() {

		// Oscillating effect
		if (particleAmount >= MAX_PARTICLES) particleDelta = -1;
		else if (particleAmount <= MIN_PARTICLES) particleDelta = 1;
		particleAmount += particleDelta;

		Game.get().getCurrentWorld().spawnParticle(
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
		HandlerList.unregisterAll(this);
		stop();
	}

	@EventHandler
	// Infect player and destroy sneeze
	public void onSneezeHit(ProjectileHitEvent e) {

		if (snowball == e.getEntity()) {

			// Player hit
			if (snowball.getShooter() instanceof Player && e.getHitEntity() instanceof Player) {
				PlayerHandler infected = Game.get().getHandlers().get((Player) e.getHitEntity());
				PlayerHandler infector = Game.get().getHandlers().get((Player) snowball.getShooter());

				if (infected != null && infected.getState() != PlayerState.DEAD)
					infected.getInfectedBy(infector);
			}

			// Remove sneeze
			remove();
		}
	}
}
