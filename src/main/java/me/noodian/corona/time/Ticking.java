package me.noodian.corona.time;

import me.noodian.corona.Corona;

public abstract class Ticking {

	public abstract void tick();

	// Safely delete the object
	public void remove() {
		stop();
	}

	// Stop ticking
	protected final void stop() {
		Corona.get().updater.remove(this);
	}

	// Start ticking
	protected final void start() {
		Corona.get().updater.add(this);
	}
}