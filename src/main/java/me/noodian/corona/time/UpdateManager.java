package me.noodian.corona.time;

import org.bukkit.scheduler.BukkitRunnable;
import java.util.ArrayList;

public class UpdateManager extends BukkitRunnable {

	private final ArrayList<Ticking> objects;

	public UpdateManager() {
		objects = new ArrayList<>();
	}

	// Add a ticking object if it's not already registered
	public void add(Ticking object) {
		if (!objects.contains(object)) objects.add(object);
	}

	// Remove a ticking object
	public void remove(Ticking object) {
		objects.remove(object);
	}

	@Override
	// Every callback, tick every object
	public void run() {
		for (Ticking object : objects) object.tick();
	}
}
