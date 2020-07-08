package me.noodian.corona.player;

import me.noodian.corona.Game;
import me.noodian.corona.GameState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.util.*;

public class PlayerList implements Listener {

	private final HashMap<PlayerState, ArrayList<Player>> states;
	private final HashMap<Player, PlayerHandler> players;
	private final ArrayList<PlayerHandler> handlers;

	public PlayerList() {
		states = new HashMap<>();
		for (PlayerState state : PlayerState.values()) {
			states.put(state, new ArrayList<>());
		}
		players = new HashMap<>();
		handlers = new ArrayList<>();
		Game.get().getServer().getPluginManager().registerEvents(this, Game.get());
	}
	
	@EventHandler(priority=EventPriority.LOW) // LOW means it gets called first
	// When player state changes, update lists
	public void onPlayerStateChange(PlayerStateChangeEvent e) {
		if (Game.get().getState() == GameState.INGAME) {
			if (states.get(e.getOldState()) != null) {
				states.get(e.getOldState()).remove(e.getPlayerHandler().getPlayer());
				states.get(e.getPlayerHandler().getState()).add(e.getPlayerHandler().getPlayer());
			}
		} else {
			e.setCancelled(true);
		}
	}
	
	// Safely remove
	public void remove() {
		HandlerList.unregisterAll(this);
	}

	// Get all players in the specified states
	public ArrayList<Player> getPlayers(PlayerState... states) {
		ArrayList<Player> out = new ArrayList<>();

		if (states.length == 0)
			for (PlayerHandler handler : handlers)
				out.add(handler.getPlayer());
		else
			for (PlayerState state : states)
				out.addAll(this.states.get(state));
		return out;
	}

	// Return the player handler
	public PlayerHandler get(Player player) {
		return players.get(player);
	}
	
	// Add player handler to lists
	void add(PlayerHandler handler) {
		if (handlers.contains(handler)) return;

		states.get(PlayerState.HEALTHY).add(handler.getPlayer());
		players.put(handler.getPlayer(), handler);
		handlers.add(handler);
	}

	// Remove player handler from lists
	void remove(PlayerHandler handler) {
		states.get(handler.getState()).remove(handler.getPlayer());
		players.remove(handler.getPlayer());
		handlers.remove(handler);
	}
}
