package me.noodian.corona.player;

import me.noodian.corona.Corona;
import me.noodian.corona.Ticking;
import me.noodian.corona.UpdateManager;
import me.noodian.util.countdown.Countdown;
import me.noodian.util.countdown.CountdownCallback;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;

public class PlayerManager implements Ticking, CountdownCallback {

	private final HashMap<PlayerState, ArrayList<Player>> states;
	private final HashMap<Player, PlayerHandler> handlers;
	private Countdown xpCountdown;
	
	public PlayerManager() {
		states = new HashMap<>();
		states.put(PlayerState.HEALTHY, new ArrayList<>());
		states.put(PlayerState.INCUBATING, new ArrayList<>());
		states.put(PlayerState.INFECTED, new ArrayList<>());
		states.put(PlayerState.DEAD, new ArrayList<>());

		handlers = new HashMap<>();
	}

	// Create player handler and store it accordingly
	public void add(Player player, PlayerState state) {
		if (handlers.get(player) != null) return;
		handlers.put(player, new PlayerHandler(player, state, this));
		states.get(PlayerState.HEALTHY).add(player);
	}

	// Remove player handler
	public void remove(Player player) {
		UpdateManager.GetInstance().Objects.remove(handlers.get(player));
		PlayerHandler handler = handlers.get(player);
		if (handler == null) return;
		handler.packetHandler.Remove();
		handlers.remove(player);
		states.get(handler.getState()).remove(player);
	}
	
	// Get all players in the specified state
	public ArrayList<Player> getPlayers(PlayerState state) {
		return states.get(state);
	}

	// Get all players in the specified states
	public ArrayList<Player> getPlayers(PlayerState[] states) {
		ArrayList<Player> out = new ArrayList<Player>();
		for (PlayerState state : states) {
			out.addAll(this.states.get(state));
		}
		return out;
	}

	// Change the state of the player
	public boolean changeState(Player infected, PlayerState state) {
		return handlers.get(infected).setState(PlayerState.INCUBATING);
	}

	// Infect the player and heal the infector after 5 seconds
	public boolean infect(Player infected, Player infector) {

		// Change state
		if (!handlers.get(infected).setState(PlayerState.INCUBATING)) return false;

		// Heal infector
		new BukkitRunnable() {
			@Override
			public void run() {
				handlers.get(infector).setState(PlayerState.HEALTHY);
			}
		}.runTaskLater(Corona.GetInstance(), 5 * 20);

		return true;
	}

	// Add all players to the countdown
	public void globalCountdown(Countdown countdown) {
		for (Player player: Bukkit.getOnlinePlayers()) {
			handlers.get(player).setXpCountdown(countdown.);
		}
	}

	//
	public void playerUsedItem(Player player, int item) {

	}

	@Override
	public void Tick() {
		// Incubating -> Infected
		else if (xpCountdown == countdown)
			setState(PlayerState.INFECTED);
	}

	@Override
	public void Finished(Countdown sender) {
		if (xpCountdown == sender) {
			for (PlayerHandler handler : getHandlers()) {
				handler.setState(IN)
			}
		}
	}

	// Returns list of all playerhandlers
	private ArrayList<PlayerHandler> getHandlers() {
		ArrayList<PlayerHandler> out = new ArrayList<>();
		for (PlayerState state : PlayerState.values()) {
			for (Player player : states.get(state)) {
				out.add(handlers.get(player));
			}
		}
		return out;
	}
}
