package me.noodian.corona.player;

import me.noodian.corona.Corona;
import me.noodian.corona.ui.Displayable;
import me.noodian.corona.ui.ScoreboardDisplay;
import me.noodian.corona.ui.UiDisplay;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class PlayerList implements Displayable {

	private final HashMap<PlayerState, ArrayList<Player>> states;
	private final HashMap<Player, PlayerHandler> players;
	private final ArrayList<PlayerHandler> handlers;
	private final Set<UiDisplay> subscribers;

	public PlayerList() {
		states = new HashMap<>();
		for (PlayerState state : PlayerState.values()) {
			states.put(state, new ArrayList<>());
		}
		players = new HashMap<>();
		handlers = new ArrayList<>();
		subscribers = new HashSet<>();
		new ScoreboardDisplay().subscribeTo(this);
	}

	@Override
	// Add a subscriber
	public void addSubscriber(UiDisplay subscriber) {
		subscribers.add(subscriber);
	}

	@Override
	// Remove a subscriber
	public void removeSubscriber(UiDisplay subscriber) {
		subscribers.remove(subscriber);
	}

	// Create player handler and store it accordingly
	public void add(Player player, PlayerState state) {
		if (players.get(player) != null) return;

		PlayerHandler handler = new PlayerHandler(player, state, this);
		states.get(PlayerState.HEALTHY).add(player);
		players.put(player, handler);
		handlers.add(handler);
	}

	// Remove player handler
	public void remove(Player player) {

		PlayerHandler handler = players.get(player);
		if (handler == null) return;

		handler.packetHandler.Remove();
		Corona.getInstance().updateManager.remove(handler);
		states.get(handler.getState()).remove(player);
		players.remove(player);
		handlers.remove(handler);
	}

	// Get all players in the specified states
	public ArrayList<Player> getPlayers(PlayerState... states) {
		ArrayList<Player> out = new ArrayList<>();

		if (states == null)
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

	// When player state changes, update list and UI
	void stateChange(PlayerHandler handler, PlayerState oldState, PlayerState newState) {
		states.get(oldState).remove(handler.getPlayer());
		states.get(newState).add(handler.getPlayer());

		Object alive = getPlayers(PlayerState.INFECTED, PlayerState.INCUBATING, PlayerState.HEALTHY);
		Object dead = getPlayers(PlayerState.DEAD);
		for (UiDisplay subscriber : subscribers) subscriber.update(alive, dead);

		Corona.getInstance().playerStateChange();
	}
}
