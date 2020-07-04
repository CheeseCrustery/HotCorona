package me.noodian.corona;

import org.bukkit.Server;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.logging.Level;

public class TextManager {
    
    private final Server server;

    public TextManager(Server server) {
        this.server = server;
    }

    // Get a string from the Game.get().getConfig()
    public String get(String key) {
        String out = Game.get().getConfig().getString(key);
        if (out == null) {
            Game.get().log(Level.WARNING, "Config key \"" + key + "\" not found!");
            return key;
        }
        return out;
    }

    // Get a message from the Game.get().getConfig() and send it server wide
    public void sendMessage(String key) {
        String out = get("chat." + key);

        String[] lines = out.split("\n");
        for (String line : lines) {
            server.broadcastMessage(line);
        }
    }

    // Send a title to the specified player
    public void sendTitle(String key, Player player) {
        String title = get("title." + key);
        Game.get().getHandlers().get(player).showTitle(title);
    }

    // Insert the placeholders and send the title to the specified player
    public void sendTitle(String key, Player player, String[][] placeholders) {
        String title = get("title." + key);
        title = insertPlaceholders(title, placeholders);
        Game.get().getHandlers().get(player).showTitle(title);
    }

    // Insert the Array of [i][Placeholder, Value] into the string
    private String insertPlaceholders(String text, String[][] placeholders) {
        for (String[] placeholder : placeholders) {
            text = text.replaceAll("%" + placeholder[0] + "%", placeholder[1]);
        }
        return text;
    }
}
