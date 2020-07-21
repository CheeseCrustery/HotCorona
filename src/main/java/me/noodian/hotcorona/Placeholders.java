package me.noodian.corona;

import me.noodian.corona.player.PlayerState;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

import java.util.ArrayList;

public class Placeholders extends PlaceholderExpansion {
	
	@Override
	// You must override this method to let PlaceholderAPI know to not unregister your expansion class
	public boolean persist(){
		return true;
	}
	
	@Override
	// Return whether all dependencies are loaded
	public boolean canRegister(){
		return true;
	}
	
	@Override
	// The name of the person who created this expansion
	public String getAuthor(){
		return Game.get().getDescription().getAuthors().get(0);
	}
	
	@Override
	// The plugins identifier
	public String getIdentifier(){
		return "hotcorona";
	}
	
	@Override
	// The plugins version
	public String getVersion(){
		return Game.get().getDescription().getVersion();
	}
	
	@Override
	// This is the method called when a placeholder with our identifier is found and needs a value
	public String onPlaceholderRequest(Player player, String identifier){
		
		// %hotcorona_living_players%
		if (identifier.equals("living_players")) {
			ArrayList<Player> livingPlayers = Game.get().getHandlers().getPlayers(PlayerState.HEALTHY, PlayerState.INCUBATING, PlayerState.INFECTED);
			StringBuilder list = new StringBuilder();
			for (int j = 0; j < livingPlayers.size(); j++) {
				list.append(livingPlayers.get(j).getDisplayName());
				if (j < livingPlayers.size() - 1) list.append(", ");
			}
			return list.toString();
		}
		
		// We return null if an invalid placeholder was provided
		return null;
	}
}