package me.noodian.corona;

import me.noodian.corona.player.PlayerManager;
import me.noodian.corona.player.PlayerState;
import io.github.cheesecrustery.util.*;
import io.github.cheesecrustery.util.countdown.*;

import me.noodian.util.Chat;
import me.noodian.util.NameEncoder;
import me.noodian.util.PlayerVelocity;
import me.noodian.util.ScoreboardUi;
import me.noodian.util.countdown.CountdownCallback;
import me.noodian.util.countdown.Countdown;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Builder;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public final class Corona extends JavaPlugin implements Listener, CountdownCallback {

	private static Corona ms_Instance;
	private GameState m_State;
	private ArrayList<Player> m_Winners;
	private long m_StartTime;

	public PlayerManager Players;
	public Chat ServerChat;
	public FileConfiguration Config;
	public ScoreboardUi Scores;

	// Get the singleton's instance
	public static Corona GetInstance() {
		return ms_Instance;
	}

	// Constructor
	@Override
	public void onEnable() {

		// Init BungeeCord plugin messages
		this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

		// Register all events in the plugin manager
		getServer().getPluginManager().registerEvents(this, this);

		// Init config
		saveDefaultConfig();
		Config = this.getConfig();

		// Init members
		ms_Instance = this;
		Players = new PlayerManager();
		ServerChat = new Chat(this.getServer(), Config);
		m_State = GameState.PAUSE;
		m_Winners = new ArrayList<>(3);
	}

	// When start countdown has finished, start game
	@Override
	public void Finished() {
		SetState(GameState.INGAME);
	}

	// Add player to manager, unpause plugin if needed
	@EventHandler
	public void OnPlayerJoin(PlayerJoinEvent e) {

		switch (m_State) {
			case PAUSE:
				SetState(GameState.STARTING);
			case STARTING:
				Players.add(e.getPlayer(), PlayerState.HEALTHY);
				break;
			case INGAME:
			case END:
				Players.add(e.getPlayer(), PlayerState.DEAD);
		}
	}

	// Remove player from manager, pause plugin if needed
	@EventHandler
	public void OnPlayerQuit(PlayerQuitEvent e) {
		Players.remove(e.getPlayer());

		if (getServer().getOnlinePlayers().size() <= 0) {
			SetState(GameState.PAUSE);
		}
	}

	// Shoot projectile based on held item
	@EventHandler
	public void OnPlayerInteract(PlayerInteractEvent e) {
		if (e.getAction() != Action.RIGHT_CLICK_AIR) return;

		int id = NameEncoder.Decode(e.getItem().getItemMeta().getDisplayName());
		if (id == 42) {

			// Cough
			UpdateManager.GetInstance().Objects.add(new Cough(e.getPlayer(), 2.0d, 200L));
			getServer().getWorld("world").playSound(e.getPlayer().getEyeLocation(), Sound.ENTITY_WITHER_BREAK_BLOCK, 1.0f, 1.0f);
		} else if (id == 43) {

			// Sneeze
			Projectile projectile = e.getPlayer().launchProjectile(Snowball.class);
			getServer().getWorld("world").playSound(e.getPlayer().getEyeLocation(), Sound.ENTITY_ENDER_DRAGON_SHOOT, 1.0f, 1.0f);
		}
	}

	// Infect player on sneeze hit
	@EventHandler
	public void OnSneezeHit(EntityDamageByEntityEvent e) {

		if (e.getDamager() instanceof Snowball
				&& e.getEntity() instanceof Player
				&& e.getDamager() != e.getEntity()
		) {
			Snowball snowball = (Snowball)e.getDamager();
			if (snowball.getShooter() instanceof Player) {
				Players.infect((Player)e.getEntity(), (Player)snowball.getShooter());
			}
		}
	}

	// When a player's state changes, check if round has ended
	public void PlayerStateChange() {

		// If only one player is alive, he wins
		PlayerState[] livingStates = {PlayerState.HEALTHY, PlayerState.INCUBATING, PlayerState.INFECTED};
		PlayerState[] infectedStates = {PlayerState.INCUBATING, PlayerState.INFECTED};
		if (Players.getPlayers(livingStates).size() == 1 || Players.getPlayers(infectedStates).size() == 0) {
			m_Winners = Players.getPlayers(livingStates);
			SetState(GameState.END);
		}
	}

	// Get the time in seconds since the game started
	public int GetGameDuration() {
		if (m_StartTime > 0) {
			long now = new Date().getTime();
			return (int)((now - m_StartTime) / 1000L);
		} else {
			return 0;
		}
	}

	// Set the game state and take action accordingly
	public boolean SetState(GameState state) {

		if (m_State == state) return false;

		switch (state) {
			case STARTING:

				// Run clocks
				PlayerVelocity.GetInstance().runTaskTimer(this, 0, 1);
				UpdateManager.GetInstance().runTaskTimer(this, 0, 1);

				// Send chat message and start countdown
				ServerChat.sendMessage("game-start");
				Players.globalCountdown(new Countdown(10, this));
				break;
			case INGAME:

				// Set start date
				m_StartTime = new Date().getTime();

				// Infect random player
				Collection<? extends Player> players = this.getServer().getOnlinePlayers();
				int infected = new Random().nextInt(players.size());
				int i = 0;
				for (Player player : players) {
					if (i == infected) Players.changeState(player, PlayerState.INCUBATING);
					else ServerChat.sendTitle("start", player);
				}
				break;
			case END:

				for (Player player : getServer().getOnlinePlayers()) {

					// Kill everyone
					Players.changeState(player, PlayerState.DEAD);

					// Fireworks
					Firework firework = player.getWorld().spawn(player.getLocation(), Firework.class);
					FireworkMeta fireworkMeta = firework.getFireworkMeta();
					Builder builder = FireworkEffect.builder();

					fireworkMeta.addEffect(builder.trail(true).build());
					fireworkMeta.addEffect(builder.withFade(List.of(Color.GREEN, Color.WHITE)).build());
					fireworkMeta.addEffect(builder.with(FireworkEffect.Type.STAR).build());
					fireworkMeta.setPower(2);
					firework.setFireworkMeta(fireworkMeta);

					// Send winner title
					if (m_Winners.size() == 1)
						ServerChat.sendTitle("onewinner", player, new String[][]{{"name", m_Winners.get(0).getDisplayName()}});
					else
						ServerChat.sendTitle("multiplewinners", player);
				}
				break;
			case PAUSE:

				// End clocks
				PlayerVelocity.GetInstance().cancel();
				UpdateManager.GetInstance().cancel();
		}

		m_State = state;
		return true;
	}
}
