package me.noodian.corona.ui;

import me.noodian.corona.player.PlayerHandler;

public abstract class UiDisplay {

	protected PlayerHandler owner;
	private Displayable publisher;

	// Update the GUI
	public abstract void update(Object... args);

	// Subscribe to a new publisher, unsubscribe from the old one
	public void subscribeTo(Displayable publisher) {
		if (this.publisher == publisher) return;
		if (this.publisher != null) this.publisher.removeSubscriber(this);
		this.publisher = publisher;

		if (publisher == null)
			update();
		else
			publisher.addSubscriber(this);
	}
}
