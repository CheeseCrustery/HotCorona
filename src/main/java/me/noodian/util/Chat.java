package me.noodian.util;

import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.Server;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class Chat {
    private Server m_Server;
    private FileConfiguration m_Config;

    public Chat (Server server, FileConfiguration config) {
        m_Server = server;
        m_Config = config;
    }

    // Get a message from the config and send it server wide
    public boolean sendMessage(String key) {
        String out = m_Config.getString("chat." + key);

        if (out == null) {
            return false;
        }

        String[] lines = out.split("\n");
        for (String line : lines) {
            m_Server.broadcastMessage(line);
        }

        return true;
    }

    // Send a title to the specified player
    public boolean sendTitle(String key, Player player) {
        String title = m_Config.getString("title." + key);
        if (title == null) return false;

        player.spigot().sendMessage(new ComponentBuilder(title).create());
        return true;
    }

    // Insert the placeholders and send the title to the specified player
    public boolean sendTitle(String key, Player player, String[][] placeholders) {
        String title = m_Config.getString("title." + key);
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
