package me.noodian.util;

import me.noodian.corona.time.Ticking;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashMap;

public class PlayerVelocity implements Ticking {

	private final HashMap<Player, Pair<Vector>> playerPositions;

	public PlayerVelocity() {
		playerPositions = new HashMap<>();
	}

	// Get the current velocity of a player
	public Vector getVelocity(Player player) {
		return playerPositions.get(player).x.clone().subtract(playerPositions.get(player).y);
	}

	@SuppressWarnings("SuspiciousNameCombination")
	@Override
	// Update all player positions
	public void tick() {
		for (Player player : Bukkit.getOnlinePlayers()) {
			if (playerPositions.get(player) == null) playerPositions.put(player, new Pair<>());
			playerPositions.get(player).y = playerPositions.get(player).x;
			playerPositions.get(player).x = player.getLocation().toVector();
		}
	}
}
