package me.noodian.corona;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import me.noodian.corona.player.*;
import me.noodian.corona.time.*;
import me.noodian.corona.time.Timer;
import me.noodian.util.*;
import org.bukkit.*;
import org.bukkit.FireworkEffect.Builder;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

import java.util.*;
import java.util.logging.Level;

public final class Corona extends JavaPlugin implements Listener {

	private static Corona INSTANCE;

	public PlayerVelocity playerVelocity;
	public PlayerList handlers;
	public Updater updater;
	public World world;
	public Text text;
	public FileConfiguration config;

	private GameState state;
	private Player winner;
	private long startTime;

	// When start countdown has finished, start game
	public final TimerCallback startCallback = args -> setState(GameState.INGAME);

	// After game has ended, either reload or leave the server
	public final TimerCallback endCallback = args -> {

		// Reload
		if (getServer().getOnlinePlayers().size() >= config.getInt("config.minplayers", 0)) {
			setState(GameState.PAUSE);
			setState(GameState.STARTING);
		}

		// Move players to other server
		else {
			Corona.get().getLogger().log(Level.INFO, "Not enough players to restart. Trying to move to other server...");
			ByteArrayDataOutput out = ByteStreams.newDataOutput();
			out.writeUTF("Connect");
			out.writeUTF(config.getString("config.other-server"));
			for (Player player : getServer().getOnlinePlayers()) {
				player.sendPluginMessage(this, "BungeeCord", out.toByteArray());
			}

			// Test if moving worked
			new BukkitRunnable() {
				@Override
				public void run() {
					if (getServer().getOnlinePlayers().size() > 0) {
						Corona.get().getLogger().log(Level.SEVERE, "Moving failed. Restarting game...");
						setState(GameState.PAUSE);
						setState(GameState.STARTING);
					}
				}
			}.runTaskLater(this, config.getInt("config.timeout", 100));
		}
	};

	// Get the singleton's instance
	public static Corona get() {
		return INSTANCE;
	}

	// Constructor
	@Override
	public void onEnable() {

		// Define singleton instance
		INSTANCE = this;

		// Init BungeeCord plugin messages
		this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

		// Register all events in the plugin manager
		getServer().getPluginManager().registerEvents(this, this);

		// Init configurations
		saveDefaultConfig();
		config = this.getConfig();
		text = new Text(this.getServer(), config);

		// Switch to session world
		world = new WorldCreator("_session").createWorld();
		if (world == null) {
			getLogger().log(Level.SEVERE, "Could not create world!");
			Bukkit.getPluginManager().disablePlugin(this);
		}

		state = GameState.PAUSE;
		initialize();
	}

	// Destructor
	@Override
	public void onDisable() {

		if (state != GameState.PAUSE) {
			Corona.get().getLogger().log(Level.INFO, "Trying to reset...");
			try {
				WorldLoader.resetWorld("_session", "_rollback");
			} catch (Exception ex) {
				Corona.get().getLogger().log(Level.WARNING, "Error while resetting world: " + ex.getMessage());
			} finally {
				get().getLogger().log(Level.INFO, "Resetting finished");
			}
		}
	}

	// Add player to manager, unpause plugin if needed
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e) {
		e.getPlayer().teleport(world.getSpawnLocation());

		switch (state) {
			case PAUSE:
				new PlayerHandler(e.getPlayer(), PlayerState.HEALTHY);
				setState(GameState.STARTING);
				break;
			case STARTING:
				new PlayerHandler(e.getPlayer(), PlayerState.HEALTHY);
				break;
			case INGAME:
			case END:
				new PlayerHandler(e.getPlayer(), PlayerState.DEAD);
		}
	}

	// Remove player from manager, pause plugin if needed
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent e) {
		handlers.get(e.getPlayer()).remove();

		if (getServer().getOnlinePlayers().size() <= 1) {
			setState(GameState.PAUSE);

			new BukkitRunnable() {
				@Override
				public void run() {
					Corona.get().getLogger().log(Level.INFO, "Trying to reset...");
					try {
						WorldLoader.resetWorld("_session", "_rollback");
					} catch (Exception ex) {
						Corona.get().getLogger().log(Level.WARNING, "Error while resetting world: " + ex.getMessage());
					} finally {
						get().getLogger().log(Level.INFO, "Resetting finished");
					}
				}
			}.runTaskLater(this, config.getInt("config.timeout", 100));
		}
	}

	// Shoot projectile based on held item
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent e) {
		if (e.getAction() != Action.RIGHT_CLICK_AIR
			|| e.getItem() == null
			|| e.getItem().getItemMeta() == null)
			return;

		int id = NameEncoder.decode(e.getItem().getItemMeta().getDisplayName());
		if (id == PlayerHandler.COUGH_ID) {

			// Cough
			new Cough(handlers.get(e.getPlayer()), 2.0d, 200L);
			handlers.get(e.getPlayer()).usedItem(PlayerHandler.COUGH_ID);
			Corona.get().world.playSound(e.getPlayer().getEyeLocation(), Sound.ENTITY_WITHER_BREAK_BLOCK, 1.0f, 1.0f);
		} else if (id == PlayerHandler.SNEEZE_ID) {

			// Sneeze
			Snowball snowball = e.getPlayer().launchProjectile(Snowball.class);
			new Sneeze(snowball);
			handlers.get(e.getPlayer()).usedItem(PlayerHandler.SNEEZE_ID);
			Corona.get().world.playSound(e.getPlayer().getEyeLocation(), Sound.ENTITY_ENDER_DRAGON_SHOOT, 1.0f, 1.0f);
		}
	}

	@EventHandler
	// Safety measure, even though players can't die
	public void onPlayerRespawn(PlayerSpawnLocationEvent e) {
		e.getPlayer().teleport(world.getSpawnLocation());
	}

	@EventHandler
	// Infect player and destroy sneeze
	public void onSneezeHit(ProjectileHitEvent e) {

		if (e.getEntity() instanceof Snowball) {
			Snowball snowball = (Snowball)e.getEntity();
			Sneeze sneeze = Sneeze.get(snowball);

			if (sneeze != null) {

				// Player hit
				if (snowball.getShooter() instanceof Player && e.getHitEntity() instanceof Player) {
					PlayerHandler infected = handlers.get((Player) e.getHitEntity());
					PlayerHandler infector = handlers.get((Player) snowball.getShooter());

					if (infected != null && infected.getState() != PlayerState.DEAD)
						infected.getInfectedBy(infector);
				}

				// Remove sneeze
				sneeze.remove();
			}
		}
	}

	@EventHandler
	// Disable moving items in inventory
	public void inventoryClick(InventoryClickEvent e) {
		e.setCancelled(true);
	}

	@EventHandler
	// Disable dropping items
	public void onItemDrop(PlayerDropItemEvent e) {
		e.setCancelled(true);
	}

	@EventHandler
	// Disable swapping items
	public void onItemSwap(PlayerSwapHandItemsEvent e) {
		e.setCancelled(true);
	}

	// When a player's state changes, check if round has ended
	public void playerStateChange() {

		if (state == GameState.PAUSE || state == GameState.END) return;

		// If only one player is alive, he wins
		if (handlers.getPlayers(PlayerState.HEALTHY, PlayerState.INCUBATING, PlayerState.INFECTED).size() == 1) {
			winner = handlers.getPlayers(PlayerState.HEALTHY, PlayerState.INCUBATING, PlayerState.INFECTED).get(0);
			setState(GameState.END);
		}

		// If no one is infected, all survivers win
		else if (handlers.getPlayers(PlayerState.INCUBATING, PlayerState.INFECTED).size() == 0) {
			setState(GameState.END);
		}
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

	// Set the game state and take action accordingly
	private void setState(GameState state) {

		if (this.state == state) return;
		this.state = state;

		switch (state) {
			case STARTING:

				// Reset
				initialize();
				for (Player player : this.getServer().getOnlinePlayers()) {
					player.teleport(world.getSpawnLocation());
					new PlayerHandler(player, PlayerState.HEALTHY);
				}

				// Run clocks
				updater.runTaskTimer(this, 0, 1);

				// Send chat message and start countdown
				text.sendMessage("starting");
				Timer startTimer = new Timer(10*20, startCallback);
				for (Player player : handlers.getPlayers()) {
					handlers.get(player).setGlobalTimer(startTimer);
				}
				break;
			case INGAME:

				// Set start date
				startTime = new Date().getTime();

				// Infect random player
				Collection<? extends Player> players = this.getServer().getOnlinePlayers();
				int infected = new Random().nextInt(players.size());
				int i = 0;
				for (Player player : players) {
					if (i == infected) this.handlers.get(player).getInfectedBy(null);
					else text.sendTitle("start", player);
				}
				break;
			case END:

				Timer endTimer = new Timer(10*20, endCallback);
				for (Player player : getServer().getOnlinePlayers()) {

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
						text.sendTitle("one-winner", player, new String[][]{{"name", winner.getDisplayName()}});
					else
						text.sendTitle("multiple-winners", player);

					// Set end timer
					handlers.get(player).setGlobalTimer(endTimer);
				}
				break;
			case PAUSE:

				// End clocks
				updater.cancel();
		}
	}

	// Initialize all non-final members
	private void initialize() {
		winner = null;
		updater = new Updater();
		playerVelocity = new PlayerVelocity();
		handlers = new PlayerList();
	}
}
