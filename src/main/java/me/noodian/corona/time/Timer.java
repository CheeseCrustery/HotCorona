package me.noodian.corona.time;

import me.noodian.corona.Corona;
import me.noodian.corona.ui.Displayable;
import me.noodian.corona.ui.UiDisplay;

import java.util.Set;

public class Timer implements Displayable, Ticking {

	private final TimerCallback callback;
	private final Object[] args;
	private final int initialTicks; // Ticks at the beginning
	private int ticks; // Ticks left on timer
	private Set<UiDisplay> subscribers;

	public Timer(int ticks, TimerCallback callback) {
		this.callback = callback;
		this.initialTicks = ticks;
		this.ticks = this.initialTicks;
		this.args = null;
		Corona.getInstance().updateManager.add(this);
	}

	public Timer(int ticks, TimerCallback callback, Object[] args) {
		this.callback = callback;
		this.initialTicks = ticks;
		this.ticks = this.initialTicks;
		this.args = args;
		Corona.getInstance().updateManager.add(this);
	}

	@Override
	// Add a subscriber whose graphics should be updated
	public void addSubscriber(UiDisplay subscriber) {
		subscribers.add(subscriber);
	}

	@Override
	// Remove a subscriber, no longer update his graphics
	public void removeSubscriber(UiDisplay subscriber) {
		subscribers.remove(subscriber);
	}

	@Override
	// Count down timer
	public void tick() {

		// Check if timer has finished
		if (ticks <= 0) {
			Corona.getInstance().updateManager.remove(this);
			if (this.callback != null) callback.finished(args);
		}

		// Update
		for (UiDisplay subscriber : subscribers) subscriber.update(ticks, initialTicks);
		ticks--;
	}

	// Delete timer
	public void remove() {
		Corona.getInstance().updateManager.remove(this);
		this.subscribers = null;
		this.ticks = 0;
	}
}
