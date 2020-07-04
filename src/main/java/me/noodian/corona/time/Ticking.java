package me.noodian.corona.time;

import me.noodian.corona.Game;

public abstract class Ticking {

	public abstract void tick();

	// Safely delete the object
	public void remove() {
		stop();
	}

	// Stop ticking
	protected final void stop() {
		Game.get().getUpdater().remove(this);
	}

	// Start ticking
	protected final void start() {
		Game game = Game.get();
		Game.get().getUpdater().add(this);
	}
}