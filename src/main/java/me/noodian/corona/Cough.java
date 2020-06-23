package me.noodian.corona;

import me.noodian.corona.player.PlayerHandler;
import me.noodian.corona.player.PlayerState;
import me.noodian.corona.time.Ticking;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.Collection;

public class Cough extends Ticking {

	private static final double SPEED = 0.5;
	private static final int PARTICLE_DENSITY = 20;

	private final Vector baseVelocity;
	private final PlayerHandler shooter;
	private final BoundingBox hitBox;
	private long lifetime;
	private Vector velocity;

	public Cough(PlayerHandler shooter, double size, long lifetime) {

		// Set player
		this.shooter = shooter;
		Player player = shooter.getPlayer();

		// Set lifetime
		this.lifetime = lifetime;

		// Make bounding box
		double half = size/2;
		Vector center = player.getEyeLocation().toVector();
		center.add(player.getLocation().getDirection().multiply(half));
		hitBox = new BoundingBox(
				center.getX()-half, center.getY()-half, center.getZ()-half,
				center.getX()+half, center.getY()+half, center.getZ()+half
		);

		// Take base velocity of player
		baseVelocity = Corona.get().playerVelocity.getVelocity(player).clone();

		// The cloud moves along the players viewing direction
		velocity = player.getLocation().getDirection().clone().normalize();

		// Start ticking
		start();
	}

	@Override
	public void tick() {

		// Check if still alive
		if (--lifetime <= 0) {
			stop();
			return;
		}

		// Check for collision
		Collection<Entity> entities = Corona.get().world.getNearbyEntities(hitBox, entity -> entity instanceof Player);
		if (entities.size() > 0) onCollision(entities);

		// Move
		hitBox.shift(baseVelocity.add(velocity.multiply(SPEED)));

		// Spawn particles
		for (int i = 0; i < PARTICLE_DENSITY * hitBox.getVolume(); i++) {
			Vector vec = randomPointInBox();
			Corona.get().world.spawnParticle(Particle.SLIME, vec.getX(), vec.getY(), vec.getZ(), 1);
		}
	}

	// If the cough collided with a player, infect him
	private void onCollision(Collection<Entity> entities) {

		for (Entity entity : entities) {

			// Get other player
			if (!(entity instanceof Player)) continue;
			Player otherPlayer = (Player)entity;
			PlayerHandler other = Corona.get().handlers.get(otherPlayer);
			if (other == shooter) continue;

			// Infect other player
			if (other.getState() != PlayerState.DEAD) {
				other.getInfectedBy(shooter);
			}

			Bukkit.getServer().broadcastMessage("COLLISION WITH " + otherPlayer.getName());
		}
	}

	// Returns a random vector within the bounding box
	private Vector randomPointInBox() {
		Vector size = hitBox.getMax().subtract(hitBox.getMin());
		Vector relativePoint = Vector.getRandom().multiply(size);
		return hitBox.getMin().add(relativePoint);
	}
}
