package me.noodian.corona.player;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlayerStateChangeEvent extends Event {
	
	private static final HandlerList handlers = new HandlerList();
	private final PlayerHandler playerHandler;
	private final PlayerState oldState;
	
	public static HandlerList getHandlerList() {
		return handlers;
	}
	
	public PlayerStateChangeEvent(PlayerHandler playerHandler, PlayerState oldState) {
		this.playerHandler = playerHandler;
		this.oldState = oldState;
	}
	
	@Override
	// Returns the list of listeners
	public HandlerList getHandlers() {
		return handlers;
	}
	
	public PlayerState getOldState() {
		return oldState;
	}
	
	// Returns the player handler
	public PlayerHandler getPlayerHandler() {
		return playerHandler;
	}
}
