package me.noodian.corona;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import me.noodian.corona.player.*;
import me.noodian.corona.time.*;
import me.noodian.corona.time.Timer;
import me.noodian.corona.ui.*;
import me.noodian.util.*;
import org.bukkit.*;
import org.bukkit.FireworkEffect.Builder;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.logging.Level;

public class Game extends JavaPlugin implements Listener {
	
	public static boolean PAPI_ENABLED = false;
	private static Game INSTANCE = null;
	
	private PlayerVelocityMonitor playerVelocityMonitor;
	private TextManager textManager;
	private PlayerList handlers;
	private Updater updater;
	private World world;
	private Scoreboard mainScoreboard;
	private Timer globalTimer;
	private GameState state;
	private long startTime;
	
	// Singleton accessor
	public static Game get() {
		return INSTANCE;
	}
	
	@Override
	// Initialize
	public void onEnable() {
		
		// Define singleton
		INSTANCE = this;
		
		// Register everything
		if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
			new Placeholders().register();
			PAPI_ENABLED = true;
		} else {
			log(Level.WARNING, "PlaceholderAPI not found");
		}
		getServer().getPluginManager().registerEvents(this, this);
		getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
		saveDefaultConfig();
		
		// Reset session world and switch to it
		try {
			WorldLoader.resetWorld("_session", "_rollback");
		} catch (Exception ex) {
			log(Level.WARNING, "Error while resetting world: " + ex.getMessage());
		} finally {
			log(Level.INFO, "Resetting finished");
		}
		world = new WorldCreator("_session").createWorld();
		if (world == null) {
			log(Level.SEVERE, "Could not create world!");
			getServer().getPluginManager().disablePlugin(this);
		}
		
		// Initialize members
		textManager = new TextManager();
		state = GameState.PAUSED;
		updater = new Updater();
		playerVelocityMonitor = new PlayerVelocityMonitor();
		handlers = new PlayerList();
		mainScoreboard = new Scoreboard();
	}
	
	@EventHandler
	// Create a handler for the player
	public void onPlayerJoin(PlayerJoinEvent e) {
		PlayerHandler handler;
		
		// Unpause if needed
		if (state == GameState.PAUSED) setState(GameState.STARTING);
		
		// Create player handler
		if (state == GameState.STARTING)
			handler = new PlayerHandler(e.getPlayer(), PlayerState.HEALTHY);
		else
			handler = new PlayerHandler(e.getPlayer(), PlayerState.DEAD);
		
		// Update scoreboard
		updateScoreboard();
		handler.setScoreboard(mainScoreboard);
		
		// Update timer
		if (globalTimer != null)
			handler.setGlobalTimer(globalTimer);
	}
	
	@EventHandler
	// Remove player
	public void onPlayerQuit(PlayerQuitEvent e) {
		handlers.get(e.getPlayer()).remove();
		updateScoreboard();
		testForWinner();
		if (getServer().getOnlinePlayers().size() <= 1)
			setState(GameState.PAUSED);
	}
	
	@EventHandler
	// When a player's state changes, check if round has ended
	public void onPlayerStateChange(PlayerStateChangeEvent e) {
		
		// Only update when not already finished
		if (e.isCancelled()) return;
		testForWinner();
		updateScoreboard();
	}
	
	// Get the state
	public GameState getState() {
		return state;
	}
	
	// Get a reference to the text manager object
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
	
	// Get a reference to the velocity monitor
	public PlayerVelocityMonitor getVelocity() {
		return playerVelocityMonitor;
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
	
	// If there's a winner, end the game
	private void testForWinner() {
		
		// If only one player is alive, he wins
		if (handlers.getPlayers(PlayerState.HEALTHY, PlayerState.INCUBATING, PlayerState.INFECTED).size() == 1) {
			setState(GameState.ENDED);
		}
		
		// If no one is infected, all survivors win
		else if (handlers.getPlayers(PlayerState.INCUBATING, PlayerState.INFECTED).size() == 0) {
			setState(GameState.ENDED);
		}
	}
	
	// Set the game state and take action accordingly
	private void setState(GameState state) {

		if (this.state == state) return;
		this.state = state;
		
		switch (state) {
			case STARTING:

				// Run clocks
				updater.runTaskTimer(this, 0, 1);

				// Send chat message
				textManager.sendChatMessage("starting");
				
				// Start countdown
				TimerCallback startCallback = args -> setState(GameState.INGAME);
				globalTimer = new Timer(10*20, startCallback);
				for (Player player : handlers.getPlayers()) {
					handlers.get(player).setGlobalTimer(globalTimer);
				}

				break;
			case INGAME:

				// Set start date
				startTime = new Date().getTime();

				// Infect random player
				Collection<Player> players = handlers.getPlayers();
				int infected = new Random().nextInt(players.size());
				int i = 0;
				for (Player player : players) {
					if (i == infected)
						this.handlers.get(player).getInfectedBy(null);
					else
						textManager.sendTitle("start", player);
					i++;
				}

				break;
			case ENDED:

				// After game has ended, either reload or leave the server
				TimerCallback endCallback = args -> setState(GameState.PAUSED);
				globalTimer = new Timer(10*20, endCallback);
				
				// Send winner message
				textManager.sendChatMessage("survived");
				
				for (Player player : handlers.getPlayers()) {

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
					textManager.sendTitle("end", player);
					
					// Set end timer
					handlers.get(player).setGlobalTimer(globalTimer);
				}
				
				break;
			case PAUSED:
				
				// Cancel updates
				updater.cancel();
				
				// Move players
				log(Level.INFO, "Not enough players to restart. Trying to move to other server...");
				String otherServer = getConfig().getString("config.other-server");
				if (otherServer != null) {
					ByteArrayDataOutput out = ByteStreams.newDataOutput();
					out.writeUTF("Connect");
					out.writeUTF(getConfig().getString("config.other-server"));
					for (Player player : getServer().getOnlinePlayers())
						player.sendPluginMessage(this, "BungeeCord", out.toByteArray());
				} else {
					log(Level.WARNING, "Name of other server not found! Please specify config.other-server in the configuration file.");
				}
				
				// If moving failed, restart
				new BukkitRunnable() {
					@Override
					public void run() {
						if (getServer().getOnlinePlayers().size() > 0) {
							log(Level.WARNING, "Moving players failed!");
						}
						log(Level.INFO, "Restarting server...");
						getServer().dispatchCommand(Bukkit.getConsoleSender(), "restart");
					}
				}.runTaskLater(this, getConfig().getInt("config.timeout", 100));
		}
	}

	// Update the scoreboards data
	private void updateScoreboard() {
		ArrayList<Player> alive = handlers.getPlayers(PlayerState.INFECTED, PlayerState.INCUBATING, PlayerState.HEALTHY);
		ArrayList<Player> dead = handlers.getPlayers(PlayerState.DEAD);
		mainScoreboard.update(alive, dead);
	}
}
