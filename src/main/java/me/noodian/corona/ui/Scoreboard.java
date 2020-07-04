package me.noodian.corona.ui;

import me.noodian.corona.Game;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.Collection;
import java.util.logging.Level;

public class Scoreboard implements Displayable {

	public org.bukkit.scoreboard.Scoreboard board;
	private Objective objective;
	private Team alive, dead;

	public Scoreboard() {
		ScoreboardManager manager = Bukkit.getScoreboardManager();
		if (manager == null) {
			Game.get().log(Level.WARNING, "Failed to initialize scoreboard. Couldn't get scoreboard manager.");
			return;
		}
		board = manager.getNewScoreboard();

		objective = board.registerNewObjective("living", "dummy", Game.get().getTextManager().get("scoreboard.title"));

		alive = board.registerNewTeam("alive");
		ChatColor aliveColor = ChatColor.getByChar(Game.get().getTextManager().get("scoreboard.color.alive"));
		if (aliveColor == null) aliveColor = ChatColor.GREEN;
		alive.setColor(aliveColor);
		alive.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);

		dead = board.registerNewTeam("dead");
		ChatColor deadColor = ChatColor.getByChar(Game.get().getTextManager().get("scoreboard.color.dead"));
		if (deadColor == null) deadColor = ChatColor.GRAY;
		dead.setColor(deadColor);
		dead.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
		dead.setCanSeeFriendlyInvisibles(true);

		for (Player player: Bukkit.getOnlinePlayers())
			alive.addEntry(player.getDisplayName());
	}

	@Override
	// Because the scoreboard updates itself, we don't need to keep track of subscribers
	public void onSubscriberAdded(Display subscriber) {}

	@Override
	// Because the scoreboard updates itself, we don't need to keep track of subscribers
	public void onSubscriberRemoved(Display subscriber) {}

	// Update the scoreboard with list of alive and list of dead players
	public void update(Collection<Player> newAlive, Collection<Player> newDead) {

		// Check for bad initialization
		if (objective == null || alive == null || dead == null) return;

		// Update teams
		updateTeam(alive, newAlive);
		updateTeam(dead, newDead);

		// Update scores
		int counter = 0;
		for (Player player : newDead)
			counter = countUp(counter, player.getDisplayName());
		if (newDead.size() == 0) counter = countUp(counter, "None");

		counter = countUp(counter, Game.get().getTextManager().get("scoreboard.dead"));
		counter = countUp(counter, "");

		for (Player player : newAlive)
			counter = countUp(counter, player.getDisplayName());
		if (newAlive.size() == 0) counter = countUp(counter, "None");

		countUp(counter, Game.get().getTextManager().get("scoreboard.alive"));
	}

	// Set the entries score to the counter and increase the counter
	private int countUp(int counter, String entry) {
		Score score = objective.getScore(entry);
		score.setScore(counter);
		return ++counter;
	}

	// Set the team's entries to the list
	private void updateTeam(Team team, Collection<Player> entries) {
		for (String oldEntry : team.getEntries())
			team.removeEntry(oldEntry);
		for (Player newEntry : entries)
			team.addEntry(newEntry.getDisplayName());
	}
}