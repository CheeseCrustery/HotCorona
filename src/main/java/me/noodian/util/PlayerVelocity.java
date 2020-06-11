package me.noodian.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;

public class PlayerVelocity extends BukkitRunnable {

	private static PlayerVelocity m_Instance;

	private HashMap<Player, Pair<Vector>> m_PlayerPositions;

	private PlayerVelocity() {
		m_PlayerPositions = new HashMap<Player, Pair<Vector>>();
	}

	public static PlayerVelocity GetInstance() {
		if (m_Instance == null) m_Instance = new PlayerVelocity();
		return m_Instance;
	}

	public Vector GetVelocity(Player player) {
		return m_PlayerPositions.get(player).X.clone().subtract(m_PlayerPositions.get(player).Y);
	}

	@Override
	public void run() {
		for (Player player : Bukkit.getOnlinePlayers()) {
			if (m_PlayerPositions.get(player) == null) m_PlayerPositions.put(player, new Pair<Vector>());
			m_PlayerPositions.get(player).Y = m_PlayerPositions.get(player).X;
			m_PlayerPositions.get(player).X = player.getLocation().toVector();
		}
	}
}
