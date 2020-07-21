package me.noodian.corona.player;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlayerStateChangeEvent extends Event implements Cancellable {
	
	private static final HandlerList handlers = new HandlerList();
	private final PlayerHandler playerHandler;
	private final PlayerState oldState;
	private boolean cancelled;
	
	public static HandlerList getHandlerList() {
		return handlers;
	}
	
	public PlayerStateChangeEvent(PlayerHandler playerHandler, PlayerState oldState) {
		this.playerHandler = playerHandler;
		this.oldState = oldState;
		this.cancelled = false;
	}
	
	@Override
	public boolean isCancelled() {
		return cancelled;
	}
	
	@Override
	public void setCancelled(boolean b) {
		this.cancelled = b;
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
