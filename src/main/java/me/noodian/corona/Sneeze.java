package me.noodian.corona;

import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Snowball;

public class Sneeze implements Ticking {

	private Snowball m_Super;

	@Override
	public void Tick() {
		World world = Bukkit.getServer().getWorld("world");
		world.spawnParticle(Particle.SLIME, m_Super.getLocation().getX(), m_Super.getLocation().getY(), m_Super.getLocation().getZ(), 1);
	}
}
