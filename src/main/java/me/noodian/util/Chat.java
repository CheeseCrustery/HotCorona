package me.noodian.util;

import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.Server;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

@SuppressWarnings("UnusedReturnValue")
public class Chat {
    private final Server server;
    private final FileConfiguration config;

    public Chat (Server server, FileConfiguration config) {
        this.server = server;
        this.config = config;
    }

    // Get a message from the config and send it server wide
    public boolean sendMessage(String key) {
        String out = config.getString("chat." + key);

        if (out == null) {
            return false;
        }

        String[] lines = out.split("\n");
        for (String line : lines) {
            server.broadcastMessage(line);
        }

        return true;
    }

    // Send a title to the specified player
    public boolean sendTitle(String key, Player player) {
        String title = config.getString("title." + key);
        if (title == null) return false;

        player.spigot().sendMessage(new ComponentBuilder(title).create());
        return true;
    }

    // Insert the placeholders and send the title to the specified player
    public boolean sendTitle(String key, Player player, String[][] placeholders) {
        String title = config.getString("title." + key);
        title = insertPlaceholders(title, placeholders);
        if (title == null) return false;

        player.spigot().sendMessage(new ComponentBuilder(title).create());
        return true;
    }

    // Insert the Array of [i][Placeholder, Value] into the string
    private String insertPlaceholders(String text, String[][] placeholders) {
        for (int i = 0; i < placeholders.length / 2; i++) {
            text = text.replaceAll("%"+placeholders[i][0]+"%", placeholders[i][1]);
        }
        return text;
    }
}
