package me.noodian.corona.ui;

import me.noodian.corona.player.PlayerHandler;
import org.bukkit.Bukkit;
import org.bukkit.scoreboard.ScoreboardManager;

public class ScoreDisplay extends Display {

	public ScoreDisplay(PlayerHandler owner) {
		this.owner = owner;
	}

	// Subscribe to a scoreboard
	public void subscribeTo(Scoreboard publisher) {
		if (this.publisher == publisher) return;
		if (this.publisher != null) this.publisher.onSubscriberRemoved(this);
		this.publisher = publisher;

		if (publisher == null) {
			ScoreboardManager manager = Bukkit.getScoreboardManager();
			if (manager != null)
				owner.getPlayer().setScoreboard(manager.getMainScoreboard());
		} else {
			publisher.onSubscriberAdded(this);
			owner.getPlayer().setScoreboard(publisher.board);
		}
	}

	@Override
	// The scoreboard updates itself
	public void update(Object... args) {}
}
