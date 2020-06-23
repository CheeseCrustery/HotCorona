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

		updateSubscribers();
	}

	// Remove player handler from lists
	void remove(PlayerHandler handler) {
		states.get(handler.getState()).remove(handler.getPlayer());
		players.remove(handler.getPlayer());
		handlers.remove(handler);

		updateSubscribers();
	}

	// When player state changes, update list and UI
	void stateChange(PlayerHandler handler, PlayerState oldState, PlayerState newState) {
		states.get(oldState).remove(handler.getPlayer());
		states.get(newState).add(handler.getPlayer());

		updateSubscribers();
		Corona.get().playerStateChange();
	}

	// Update the scoreboards data
	private void updateSubscribers() {
		Object alive = getPlayers(PlayerState.INFECTED, PlayerState.INCUBATING, PlayerState.HEALTHY);
		Object dead = getPlayers(PlayerState.DEAD);
		for (UiDisplay subscriber : subscribers) subscriber.update(alive, dead);
	}
}
