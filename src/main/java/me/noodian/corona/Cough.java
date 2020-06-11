package me.noodian.corona;

import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import me.noodian.util.PlayerVelocity;

import java.util.Collection;
import java.util.Random;
import java.util.function.Predicate;

public class Cough implements Ticking {

	private static final double ms_Speed = 0.5;
	private static final int ms_ParticleDensity = 20;

	private final Vector m_BaseVelocity;
	private final Player m_Player;
	private final BoundingBox m_BoundingBox;
	private long m_Lifetime;
	private Vector m_Velocity;

	public Cough(Player player, double size, long lifetime) {

		// Set player
		m_Player = player;

		// Set lifetime
		m_Lifetime = lifetime;

		// Make bounding box
		double half = size/2;
		Vector center = player.getEyeLocation().toVector();
		center.add(player.getLocation().getDirection().multiply(half));
		m_BoundingBox = new BoundingBox(
				center.getX()-half, center.getY()-half, center.getZ()-half,
				center.getX()+half, center.getY()+half, center.getZ()+half
		);

		// Take base velocity of player
		m_BaseVelocity = PlayerVelocity.GetInstance().GetVelocity(player).clone();

		// The cloud moves along the players viewing direction
		m_Velocity = player.getLocation().getDirection().clone().normalize();
	}

	public void OnCollision(Object[] objects) {

		// Get closest player
		Player closest = null;
		double minDist = Double.MAX_VALUE;
		for (Object object : objects) {

			// Cast to player
			if (!(object instanceof Player)) continue;
			Player player = (Player)object;

			// Disregard self
			if (player == m_Player) continue;

			// Check distance
			double dist = m_BoundingBox.getCenter().distanceSquared(player.getLocation().toVector());
			if (dist < minDist) {
				closest = player;
				minDist = dist;
			}
		}

		// Infect closest player
		if (closest != null) {
			Corona.GetInstance().Players.infect(closest, m_Player);

			System.out.println("COLLISION WITH " + closest.getName());
			Bukkit.getServer().broadcastMessage("COLLISION WITH " + closest.getName());
		}
	}

	@Override
	public void Tick() {

		// Check if still alive
		if (--m_Lifetime <= 0) {
			UpdateManager.GetInstance().Objects.remove(this);
			return;
		}

		// Check for collision
		Collection<Entity> entities = Bukkit.getServer().getWorld("world").getNearbyEntities(m_BoundingBox, new Predicate<Entity>() {
			@Override
			public boolean test(Entity entity) {
				return entity instanceof Player;
			}
		});
		if (entities.size() > 0) OnCollision(entities.toArray());

		// Move
		m_BoundingBox.shift(m_BaseVelocity.add(m_Velocity.multiply(ms_Speed)));

		// Spawn particles
		for (int i = 0; i < ms_ParticleDensity * m_BoundingBox.getVolume(); i++) {
			Vector vec = RandomVector();
			Bukkit.getServer().getWorld("world").spawnParticle(Particle.SLIME, vec.getX(), vec.getY(), vec.getZ(), 1);
		}
	}

	private Vector RandomVector() {
		Random random = new Random();
		Vector out = m_BoundingBox.getMin().clone();
		out.setX(out.getX() + (m_BoundingBox.getMaxX() - m_BoundingBox.getMinX()) * random.nextDouble());
		out.setY(out.getY() + (m_BoundingBox.getMaxY() - m_BoundingBox.getMinY()) * random.nextDouble());
		out.setZ(out.getZ() + (m_BoundingBox.getMaxZ() - m_BoundingBox.getMinZ()) * random.nextDouble());
		return out;
	}
}
