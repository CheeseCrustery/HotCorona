package me.noodian.corona;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import me.noodian.corona.player.*;
import me.noodian.corona.time.*;
import me.noodian.corona.time.Timer;
import me.noodian.corona.ui.*;
import me.noodian.util.*;
import org.apache.logging.log4j.core.net.Priority;
import org.bukkit.*;
import org.bukkit.FireworkEffect.Builder;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.logging.Level;

public class Game extends JavaPlugin implements Listener {

	static private Game INSTANCE;
	
	private PlayerVelocityMonitor playerVelocityMonitor;
	private TextManager textManager;
	private PlayerList handlers;
	private Updater updater;
	private World world;
	private Scoreboard mainScoreboard;
	private Timer globalTimer;
	private GameState state;
	private Player winner;
	private long startTime;
	
	// Singleton accessor
	public static Game get() {
		return INSTANCE;
	}
	
	@Override
	// Initialize
	public void onEnable() {
		INSTANCE = this;
		
		getServer().getPluginManager().registerEvents(this, Game.get());
		getServer().getMessenger().registerOutgoingPluginChannel(Game.get(), "BungeeCord");
		
		saveDefaultConfig();
		textManager = new TextManager(getServer());
		state = GameState.PAUSED;
	}
	
	@Override
	// Destructor
	public void onDisable() {
		remove();
	}
	
	@EventHandler
	// Create a handler for the player
	public void onPlayerJoin(PlayerJoinEvent e) {
		PlayerHandler handler;
		
		if (state == GameState.PAUSED) {
			setState(GameState.STARTING);
		} else {
			if (state == GameState.STARTING)
				handler = new PlayerHandler(e.getPlayer(), PlayerState.HEALTHY);
			else
				handler = new PlayerHandler(e.getPlayer(), PlayerState.DEAD);
			handler.setScoreboard(mainScoreboard);
			if (globalTimer != null)
				handler.setGlobalTimer(globalTimer);
		}
	}
	
	@EventHandler
	// Remove handler
	public void onPlayerQuit(PlayerQuitEvent e) {
		handlers.get(e.getPlayer()).remove();
		if (getServer().getOnlinePlayers().size() <= 1)
			setState(GameState.PAUSED);
	}
	
	@EventHandler(priority=EventPriority.LOW)
	// When a player's state changes, check if round has ended
	public void onPlayerStateChange(PlayerStateChangeEvent e) {
		
		if (state == GameState.ENDED) return;
		updateScoreboard();
		
		// If only one player is alive, he wins
		if (handlers.getPlayers(PlayerState.HEALTHY, PlayerState.INCUBATING, PlayerState.INFECTED).size() == 1) {
			winner = handlers.getPlayers(PlayerState.HEALTHY, PlayerState.INCUBATING, PlayerState.INFECTED).get(0);
			setState(GameState.ENDED);
		}
		
		// If no one is infected, all survivers win
		else if (handlers.getPlayers(PlayerState.INCUBATING, PlayerState.INFECTED).size() == 0) {
			setState(GameState.ENDED);
		}
	}
	
	// Returns the text manager object by reference
	public TextManager getTextManager() {
		return textManager;
	}
	
	// Get a reference to the handlers object
	public PlayerList getHandlers() {
		return handlers;
	}
	
	// Get a reference to the world object
	public World getCurrentWorld() {
		return world;
	}

	// Get a reference to the updater
	public Updater getUpdater() {
		return updater;
	}
	
	// Get a reference to the main scoreboard
	public Scoreboard getMainScoreboard() {
		return mainScoreboard;
	}
	
	// Get a reference to the velocity monitor
	public PlayerVelocityMonitor getVelocity() {
		return playerVelocityMonitor;
	}
	
	// Get the current game state
	public GameState getState() {
		return state;
	}
	
	// Get the time in ticks since the game started
	public int getGameDuration() {
		if (startTime > 0) {
			long now = new Date().getTime();
			return (int)((now - startTime) / 50L);
		} else {
			return 0;
		}
	}
	
	// Short logging function
	public void log(java.util.logging.Level level, String message) {
		INSTANCE.getLogger().log(level, message);
	}
	
	// Set the game state and take action accordingly
	private void setState(GameState state) {

		if (this.state == state) return;
		
		switch (state) {
			case STARTING:
				/*
				log(Level.INFO, "Starting new game...");
				
				// Reset session world and switch to it
				try {
					WorldLoader.resetWorld("_session", "_rollback");
				} catch (Exception ex) {
					Game.get().log(Level.WARNING, "Error while resetting world: " + ex.getMessage());
				} finally {
					Game.get().log(Level.INFO, "Resetting finished");
				}*/
				world = new WorldCreator("_session").createWorld();
				if (world == null) {
					Game.get().log(Level.SEVERE, "Could not create world!");
					Bukkit.getPluginManager().disablePlugin(Game.get());
				}
				
				initialize();
				
				// Initialize players
				for (Player player : Game.get().getServer().getOnlinePlayers()) {
					PlayerHandler handler = new PlayerHandler(player, PlayerState.HEALTHY);
					handler.setScoreboard(mainScoreboard);
					if (globalTimer != null)
						handler.setGlobalTimer(globalTimer);
				}

				// Run clocks
				updater.runTaskTimer(Game.get(), 0, 1);

				// Send chat message and start countdown
				Game.get().getTextManager().sendMessage("starting");
				TimerCallback startCallback = args -> setState(GameState.INGAME);
				globalTimer = new Timer(10*20, startCallback);
				for (Player player : handlers.getPlayers()) {
					handlers.get(player).setGlobalTimer(globalTimer);
				}
				
				updateScoreboard();

				break;
			case INGAME:

				// Set start date
				startTime = new Date().getTime();

				// Infect random player
				Collection<? extends Player> players = Game.get().getServer().getOnlinePlayers();
				int infected = new Random().nextInt(players.size());
				int i = 0;
				for (Player player : players) {
					if (i == infected)
						this.handlers.get(player).getInfectedBy(null);
					else
						Game.get().getTextManager().sendTitle("start", player);
					i++;
				}

				break;
			case ENDED:

				// After game has ended, either reload or leave the server
				TimerCallback endCallback = args -> {
					setState(GameState.PAUSED);
				};
				globalTimer = new Timer(10*20, endCallback);
				for (Player player : Game.get().getServer().getOnlinePlayers()) {

					// Kill everyone
					this.handlers.get(player).setState(PlayerState.DEAD);

					// Fireworks
					Firework firework = player.getWorld().spawn(player.getLocation(), Firework.class);
					FireworkMeta fireworkMeta = firework.getFireworkMeta();
					Builder builder = FireworkEffect.builder();
					fireworkMeta.addEffect(builder
							.withTrail()
							.withColor(Color.GREEN)
							.withFade(Color.WHITE)
							.with(FireworkEffect.Type.STAR).build());
					fireworkMeta.setPower(2);
					firework.setFireworkMeta(fireworkMeta);

					// Send winner title
					if (winner != null)
						Game.get().getTextManager().sendTitle(
								"one-winner",
								player,
								new String[][]{{"name", winner.getDisplayName()}});
					else
						Game.get().getTextManager().sendTitle("multiple-winners", player);

					// Set end timer
					handlers.get(player).setGlobalTimer(globalTimer);
				}
				
				break;
			case PAUSED:
				
				remove();
				log(Level.INFO,"Game paused");
				
				// Either reload or leave server
				int onlinePlayers = Game.get().getServer().getOnlinePlayers().size();
				int minPlayers = Game.get().getConfig().getInt("config.min-players", 0);
				if (onlinePlayers >= minPlayers && onlinePlayers > 0)
					setState(GameState.STARTING);
				else
					stop();
		}
		
		this.state = state;
	}
	
	// Initialize members for a round
	private void initialize() {
		updater = new Updater();
		playerVelocityMonitor = new PlayerVelocityMonitor();
		handlers = new PlayerList();
		winner = null;
		mainScoreboard = new Scoreboard();
	}
	
	// Remove everything safely
	private void remove() {
		handlers.remove();
		if (globalTimer != null) globalTimer.remove();
		playerVelocityMonitor.remove();
		HandlerList.unregisterAll((Plugin) Game.get());
		Bukkit.getPluginManager().registerEvents(Game.get(), Game.get());
	}

	// Update the scoreboards data
	private void updateScoreboard() {
		ArrayList<Player> alive = handlers.getPlayers(PlayerState.INFECTED, PlayerState.INCUBATING, PlayerState.HEALTHY);
		ArrayList<Player> dead = handlers.getPlayers(PlayerState.DEAD);
		mainScoreboard.update(alive, dead);
	}
	
	// Move all players off the server
	private void stop() {
		
		// Move players
		log(Level.INFO, "Not enough players to restart. Trying to move to other server...");
		String otherServer = getConfig().getString("config.other-server");
		if (otherServer != null) {
			ByteArrayDataOutput out = ByteStreams.newDataOutput();
			out.writeUTF("Connect");
			out.writeUTF(getConfig().getString("config.other-server"));
			for (Player player : getServer().getOnlinePlayers())
				player.sendPluginMessage(Game.get(), "BungeeCord", out.toByteArray());
		} else {
			log(Level.WARNING, "Name of other server not found! Please specify config.other-server in the configuration file.");
		}
		
		// If moving failed, restart
		new BukkitRunnable() {
			@Override
			public void run() {
				if (getServer().getOnlinePlayers().size() > 0) {
					log(Level.WARNING, "Moving players failed!");
					if (getServer().getOnlinePlayers().size() > 0) setState(GameState.STARTING);
				}
			}
		}.runTaskLater(Game.get(), getConfig().getInt("config.timeout", 100));
	}
}
