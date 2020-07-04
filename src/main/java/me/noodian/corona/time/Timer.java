package me.noodian.corona.time;

import me.noodian.corona.Game;
import me.noodian.corona.ui.*;

import java.util.*;

public class Timer extends Ticking implements Displayable {

	private final TimerCallback callback;
	private final Object[] args;
	private final int initialTicks; // Ticks at the beginning
	private int ticks; // Ticks left on timer
	private Set<Display> subscribers;

	public Timer(int ticks, TimerCallback callback) {
		int realTicks = Math.max(ticks, 0);
		this.initialTicks = realTicks;
		this.ticks = realTicks;

		this.callback = callback;
		this.args = null;
		this.subscribers = new HashSet<>();

		start();
	}

	public Timer(int ticks, TimerCallback callback, Object[] args) {
		int realTicks = Math.max(ticks, 0);
		this.initialTicks = realTicks;
		this.ticks = realTicks;

		this.callback = callback;
		this.args = args;
		this.subscribers = new HashSet<>();

		start();
	}

	@Override
	// Add a subscriber whose graphics should be updated
	public void onSubscriberAdded(Display subscriber) {
		subscribers.add(subscriber);
		updateSubscribers();
	}

	@Override
	// Remove a subscriber, no longer update his graphics
	public void onSubscriberRemoved(Display subscriber) {
		subscribers.remove(subscriber);
	}

	@Override
	// Count down timer
	public void tick() {

		// Check if timer has finished
		if (ticks <= 0) {
			remove();
			if (this.callback != null) callback.finished(args);
		} else {
			ticks--;
		}

		updateSubscribers();
	}

	@Override
	// Delete timer
	public void remove() {
		this.subscribers = new HashSet<>();
		this.ticks = 0;
		stop();
	}

	// Update all subscribers
	private void updateSubscribers() {
		for (Display subscriber : subscribers)
			if (subscriber != null) subscriber.update(ticks, initialTicks);
	}
}
