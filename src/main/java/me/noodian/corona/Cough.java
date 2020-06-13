package me.noodian.corona;

import me.noodian.corona.player.PlayerHandler;
import me.noodian.corona.time.Ticking;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.Random;

public class Cough implements Ticking {

	private static final double SPEED = 0.5;
	private static final int PARTICLE_DENSITY = 20;

	private final Vector baseVelocity;
	private final Player player;
	private final BoundingBox boundingBox;
	private long lifetime;
	@SuppressWarnings({"FieldMayBeFinal", "CanBeFinal"})
	private Vector velocity;

	public Cough(Player player, double size, long lifetime) {

		// Set player
		this.player = player;

		// Set lifetime
		this.lifetime = lifetime;

		// Make bounding box
		double half = size/2;
		Vector center = player.getEyeLocation().toVector();
		center.add(player.getLocation().getDirection().multiply(half));
		boundingBox = new BoundingBox(
				center.getX()-half, center.getY()-half, center.getZ()-half,
				center.getX()+half, center.getY()+half, center.getZ()+half
		);

		// Take base velocity of player
		baseVelocity = Corona.getInstance().playerVelocity.getVelocity(player).clone();

		// The cloud moves along the players viewing direction
		velocity = player.getLocation().getDirection().clone().normalize();
	}

	public void onCollision(Object[] objects) {

		// Get closest player
		Player closest = null;
		double minDist = Double.MAX_VALUE;
		for (Object object : objects) {

			// Cast to player
			if (!(object instanceof Player)) continue;
			Player player = (Player)object;

			// Disregard self
			if (player == this.player) continue;

			// Check distance
			double dist = boundingBox.getCenter().distanceSquared(player.getLocation().toVector());
			if (dist < minDist) {
				closest = player;
				minDist = dist;
			}
		}

		// Infect closest player
		if (closest != null) {
			PlayerHandler infected = Corona.getInstance().handlers.get(closest);
			PlayerHandler infector = Corona.getInstance().handlers.get(player);
			infected.getInfectedBy(infector);

			System.out.println("COLLISION WITH " + closest.getName());
			Bukkit.getServer().broadcastMessage("COLLISION WITH " + closest.getName());
		}
	}

	@Override
	public void tick() {

		// Check if still alive
		if (--lifetime <= 0) {
			Corona.getInstance().updateManager.remove(this);
			return;
		}

		// Check for collision
		Collection<Entity> entities = Corona.getInstance().world.getNearbyEntities(boundingBox, entity -> entity instanceof Player);
		if (entities.size() > 0) onCollision(entities.toArray());

		// Move
		boundingBox.shift(baseVelocity.add(velocity.multiply(SPEED)));

		// Spawn particles
		for (int i = 0; i < PARTICLE_DENSITY * boundingBox.getVolume(); i++) {
			Vector vec = randomVector();
			Corona.getInstance().world.spawnParticle(Particle.SLIME, vec.getX(), vec.getY(), vec.getZ(), 1);
		}
	}

	private Vector randomVector() {
		Random random = new Random();
		Vector out = boundingBox.getMin().clone();
		out.setX(out.getX() + (boundingBox.getMaxX() - boundingBox.getMinX()) * random.nextDouble());
		out.setY(out.getY() + (boundingBox.getMaxY() - boundingBox.getMinY()) * random.nextDouble());
		out.setZ(out.getZ() + (boundingBox.getMaxZ() - boundingBox.getMinZ()) * random.nextDouble());
		return out;
	}
}
