package me.noodian.corona.ui;

import me.noodian.corona.Corona;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.*;

import java.util.ArrayList;

public class ScoreboardDisplay extends UiDisplay {

	private Objective objective;
	private Team alive, dead;

	public ScoreboardDisplay() {
		ScoreboardManager manager = Bukkit.getScoreboardManager();
		if (manager == null) return;
		Scoreboard board = manager.getMainScoreboard();

		objective = board.registerNewObjective("living", "dummy", Corona.get().text.get("scoreboard.title"));
		objective.setDisplaySlot(DisplaySlot.SIDEBAR);

		alive = board.registerNewTeam("alive");
		ChatColor aliveColor = ChatColor.getByChar(Corona.get().text.get("scoreboard.color.alive"));
		if (aliveColor == null) aliveColor = ChatColor.GREEN;
		alive.setColor(aliveColor);
		alive.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);

		dead = board.registerNewTeam("dead");
		ChatColor deadColor = ChatColor.getByChar(Corona.get().text.get("scoreboard.color.dead"));
		if (deadColor == null) deadColor = ChatColor.GRAY;
		dead.setColor(deadColor);
		dead.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
		dead.setCanSeeFriendlyInvisibles(true);

		for (Player player: Bukkit.getOnlinePlayers()) {
			alive.addEntry(player.getDisplayName());
		}
	}

	@Override
	// Update the scoreboard with list of alive and list of dead players
	public void update(Object... args) {

		// Check for bad initialization
		if (objective == null || alive == null || dead == null) return;

		// Check input
		ArrayList<Player> newAlive, newDead;
		if (args.length == 2 && args[0] instanceof ArrayList && args[1] instanceof ArrayList) {
			newAlive = (ArrayList<Player>)(args[0]); // Oldest Infected -> Newest alive
			newDead = (ArrayList<Player>)(args[1]); // Oldest Dead -> Newest dead
		} else {
			newAlive = new ArrayList<>();
			newDead = new ArrayList<>();
		}

		// Update teams
		updateTeam(alive, newAlive);
		updateTeam(dead, newDead);

		// Update scores
		int counter = 0;
		for (Player player : newDead) {
			counter = countUp(counter, player.getDisplayName());
		}
		if (newDead.size() == 0) counter = countUp(counter, "None");

		counter = countUp(counter, Corona.get().text.get("scoreboard.dead"));
		counter = countUp(counter, "");

		for (Player player : newAlive) {
			counter = countUp(counter, player.getDisplayName());
		}
		if (newAlive.size() == 0) counter = countUp(counter, "None");

		countUp(counter, Corona.get().text.get("scoreboard.alive"));
	}

	// Set the entries score to the counter and increase the counter
	private int countUp(int counter, String entry) {
		Score score = objective.getScore(entry);
		score.setScore(counter);
		return ++counter;
	}

	// Set the team's entries to the list
	private void updateTeam(Team team, ArrayList<Player> entries) {
		for (String oldEntry : team.getEntries())
			team.removeEntry(oldEntry);
		for (Player newEntry : entries)
			team.addEntry(newEntry.getDisplayName());
	}
}