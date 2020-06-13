package me.noodian.corona.ui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.ArrayList;

public class ScoreboardDisplay extends UiDisplay {

	private Objective objective;
	private Team alive, dead;

	public ScoreboardDisplay() {
		ScoreboardManager manager = Bukkit.getScoreboardManager();
		if (manager == null) return;
		Scoreboard board = manager.getNewScoreboard();

		objective = board.registerNewObjective("living", "dummy", "Corona");
		objective.setDisplaySlot(DisplaySlot.SIDEBAR);

		alive = board.registerNewTeam("alive");
		alive.setColor(ChatColor.GREEN);
		alive.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);

		dead = board.registerNewTeam("dead");
		dead.setColor(ChatColor.GRAY);
		dead.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
		dead.setCanSeeFriendlyInvisibles(true);

		for (Player player: Bukkit.getOnlinePlayers()) {
			alive.addEntry(player.getDisplayName());
		}
	}

	@SuppressWarnings("unchecked")
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

		counter = countUp(counter, "Dead:");
		counter = countUp(counter, "");

		for (Player player : newAlive) {
			counter = countUp(counter, player.getDisplayName());
		}

		countUp(counter, "Alive:");
	}

	private int countUp(int counter, String entry) {
		Score score = objective.getScore(entry);
		score.setScore(counter);
		return ++counter;
	}

	private void updateTeam(Team team, ArrayList<Player> newEntries) {
		for (String oldEntry : team.getEntries())
			team.removeEntry(oldEntry);
		for (Player newEntry : newEntries)
			alive.addEntry(newEntry.getDisplayName());
	}
}