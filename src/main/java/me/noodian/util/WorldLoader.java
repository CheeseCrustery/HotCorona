package me.noodian.util;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.libs.org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;

public class WorldLoader {

	// Unload the world and reset it to the rollback state
	public static void resetWorld(String world, String rollback) throws IOException, IllegalAccessException {

		// Unload session
		if (Bukkit.getServer().getWorld(world) == null || Bukkit.getServer().unloadWorld(world, false)) {

			// Empty world folder
			Path worldFolder = FileSystems.getDefault().getPath(world);
			FileUtils.cleanDirectory(worldFolder.toFile());

			// Copy regions
			Path rollbackFolder = FileSystems.getDefault().getPath(rollback);
			FileUtils.copyDirectoryToDirectory(rollbackFolder.resolve("region").toFile(), worldFolder.toFile());

			// Copy level.dat
			FileUtils.copyFileToDirectory(rollbackFolder.resolve("level.dat").toFile(), worldFolder.toFile());
		} else {
			throw new IllegalAccessException("Could not unload world " + world);
		}
	}
}
