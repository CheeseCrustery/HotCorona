package me.noodian.util;

import me.noodian.corona.Corona;
import me.noodian.corona.player.PlayerState;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

public class ScoreboardUi {

	private Scoreboard m_Board;
	private Objective m_Objective;
	private Team m_Alive, m_Dead;

	public ScoreboardUi() {
		m_Board = Bukkit.getScoreboardManager().getNewScoreboard();

		m_Objective = m_Board.registerNewObjective("living", "dummy", "Corona");
		m_Objective.setDisplaySlot(DisplaySlot.SIDEBAR);

		m_Alive = m_Board.registerNewTeam("alive");
		m_Alive.setColor(ChatColor.GREEN);
		m_Alive.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);

		m_Dead = m_Board.registerNewTeam("dead");
		m_Dead.setColor(ChatColor.GRAY);
		m_Dead.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
		m_Dead.setCanSeeFriendlyInvisibles(true);

		for (Player player: Bukkit.getOnlinePlayers()) {
			m_Alive.addEntry(player.getDisplayName());
		}
	}

	public void Update() {

		// Get new deaths
		for (Player player : Corona.GetInstance().Players.getPlayers(PlayerState.DEAD)) {
			if (m_Alive.hasEntry(player.getDisplayName())) {
				m_Alive.removeEntry(player.getDisplayName());
				m_Dead.addEntry(player.getDisplayName());
			}
		}

		// Update scores
		int counter = 0;
		for (String entry : m_Dead.getEntries()) {
			counter = CountUp(counter, entry);
		}

		counter = CountUp(counter, "Dead:");
		counter = CountUp(counter, "");

		for (String entry : m_Alive.getEntries()) {
			counter = CountUp(counter, entry);
		}

		counter = CountUp(counter, "Alive:");
	}

	private int CountUp(int counter, String entry) {
		Score score = m_Objective.getScore(entry);
		score.setScore(counter);
		return ++counter;
	}
}