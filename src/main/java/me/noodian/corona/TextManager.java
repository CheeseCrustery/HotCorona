package me.noodian.corona;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

import java.util.logging.Level;

public class TextManager {

    // Get a string from the Game.get().getConfig()
    public String get(String key) {
        String out = Game.get().getConfig().getString(key);
        if (out == null) {
            Game.get().log(Level.WARNING, "Config key \"" + key + "\" not found!");
            return key;
        }
        return out;
    }
    
    // Get message from config, insert placeholders, send it to all players
    public void sendChatMessage(String key) {
        for (Player player : Game.get().getHandlers().getPlayers())
            sendChatMessage(player, key);
    }
    
    // Get message from config, insert placeholders, send it to player
    public void sendChatMessage(Player player, String key) {
        String out = get("chat." + key);
        out = setPlaceholdersSafely(player, out);
        
        String[] lines = out.split("\n");
        for (String line : lines)
            player.sendMessage(line);
    }

    // Send a title to the specified player
    public void sendTitle(String key, Player player) {
        String out = get("title." + key);
        out = setPlaceholdersSafely(player, out);
        Game.get().getHandlers().get(player).showTitle(out);
    }
    
    // Set placeholders only if the PAPI is enabled
    private String setPlaceholdersSafely(Player player, String text) {
        if (Game.PAPI_ENABLED)
            return PlaceholderAPI.setPlaceholders(player, text);
        else
            return text;
    }
}
