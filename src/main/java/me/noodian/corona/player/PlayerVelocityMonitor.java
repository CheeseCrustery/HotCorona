package me.noodian.corona.player;

import me.noodian.corona.Game;
import me.noodian.corona.time.Ticking;
import me.noodian.util.Pair;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;

public class PlayerVelocityMonitor extends Ticking {

	private final HashMap<Player, Pair<Vector>> playerPositions;
	private final HashSet<Player> players;
	
	public PlayerVelocityMonitor() {
		playerPositions = new HashMap<>();
		players = new HashSet<>();
		start();
	}

	@Override
	// Update all player positions
	public void tick() {
		for (Player player : players) {
			if (playerPositions.get(player) == null) playerPositions.put(player, new Pair<>());
			playerPositions.get(player).y = playerPositions.get(player).x;
			playerPositions.get(player).x = player.getLocation().toVector();
		}
	}
	
	// Add player
	public void add(Player player) {
		if (players.contains(player))
			Game.get().log(Level.WARNING, "Tried to add existing player to velocity monitor: " + player);
		players.add(player);
	}
	
	// Remove player
	public void remove(Player player) {
		if (!players.contains(player))
			Game.get().log(Level.WARNING, "Tried to remove non-existent player from velocity monitor: " + player);
		players.remove(player);
		stop();
	}
	
	// Get the current velocity of a player
	public Vector get(Player player) {
		return playerPositions.get(player).x.clone().subtract(playerPositions.get(player).y);
	}
}
