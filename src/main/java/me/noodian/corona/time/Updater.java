package me.noodian.corona.time;

import me.noodian.corona.Corona;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;
import java.util.logging.Level;

public class Updater extends BukkitRunnable {

	private final Set<Ticking> objects;
	private Set<Ticking> toBeRemoved;
	private Set<Ticking> toBeAdded;

	public Updater() {
		objects = new HashSet<>();
		toBeAdded = new HashSet<>();
		toBeRemoved = new HashSet<>();
	}

	@Override
	// Every callback, tick every object
	public void run() {

		// Run updates
		for (Ticking object : objects)
			object.tick();

		// Update list
		objects.removeAll(toBeRemoved);
		objects.addAll(toBeAdded);
		toBeRemoved = new HashSet<>();
		toBeAdded = new HashSet<>();
	}

	// Schedule adding a new object, should only be called by the object itself
	void add(Ticking object) {
		if (!objects.contains(object)) toBeAdded.add(object);
		else Corona.get().getLogger().log(Level.INFO, "Tried to add existing object to UpdateManager: " + object);
	}

	// Schedule removing an object, should only be called by the object itself
	void remove(Ticking object) {
		if (objects.contains(object)) toBeRemoved.add(object);
		else Corona.get().getLogger().log(Level.INFO, "Tried to remove non-existent object from UpdateManager: " + object);
	}
}
