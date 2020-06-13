package me.noodian.corona;

import me.noodian.corona.player.PlayerHandler;
import me.noodian.corona.player.PlayerList;
import me.noodian.corona.player.PlayerState;

import me.noodian.corona.time.*;
import me.noodian.corona.time.Timer;
import me.noodian.util.Chat;
import me.noodian.util.NameEncoder;
import me.noodian.util.PlayerVelocity;
import org.bukkit.*;
import org.bukkit.FireworkEffect.Builder;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public final class Corona extends JavaPlugin implements Listener, TimerCallback {

	private static Corona INSTANCE;

	public World world;
	public PlayerList handlers;
	public Chat chat;
	public FileConfiguration config;
	public UpdateManager updateManager;
	public PlayerVelocity playerVelocity;

	private GameState state;
	private ArrayList<Player> winners;
	private long startTime;

	// Get the singleton's instance
	public static Corona getInstance() {
		return INSTANCE;
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
		config = this.getConfig();

		// Init members
		INSTANCE = this;
		handlers = new PlayerList();
		chat = new Chat(this.getServer(), config);
		state = GameState.PAUSE;
		winners = new ArrayList<>(3);
		updateManager = new UpdateManager();
		playerVelocity = new PlayerVelocity();
		world = getServer().getWorld("world");

		if (world == null) {
			System.out.println("World not found!");
			Bukkit.getPluginManager().disablePlugin(this);
		}
		updateManager.add(playerVelocity);
	}

	// When start countdown has finished, start game
	@Override
	public void finished(Object... args) {
		setState(GameState.INGAME);
	}

	// Add player to manager, unpause plugin if needed
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e) {

		switch (state) {
			case PAUSE:
				setState(GameState.STARTING);
			case STARTING:
				handlers.add(e.getPlayer(), PlayerState.HEALTHY);
				break;
			case INGAME:
			case END:
				handlers.add(e.getPlayer(), PlayerState.DEAD);
		}
	}

	// Remove player from manager, pause plugin if needed
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent e) {
		handlers.remove(e.getPlayer());

		if (getServer().getOnlinePlayers().size() <= 0) {
			setState(GameState.PAUSE);
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
			updateManager.add(new Cough(e.getPlayer(), 2.0d, 200L));
			handlers.get(e.getPlayer()).usedItem(PlayerHandler.COUGH_ID);
			Corona.getInstance().world.playSound(e.getPlayer().getEyeLocation(), Sound.ENTITY_WITHER_BREAK_BLOCK, 1.0f, 1.0f);
		} else if (id == PlayerHandler.SNEEZE_ID) {

			// Sneeze
			Snowball snowball = e.getPlayer().launchProjectile(Snowball.class);
			updateManager.add(new Sneeze(snowball));
			handlers.get(e.getPlayer()).usedItem(PlayerHandler.SNEEZE_ID);
			Corona.getInstance().world.playSound(e.getPlayer().getEyeLocation(), Sound.ENTITY_ENDER_DRAGON_SHOOT, 1.0f, 1.0f);
		}
	}

	// Infect player on sneeze hit
	@EventHandler
	public void onSneezeHit(EntityDamageByEntityEvent e) {

		if (e.getDamager() instanceof Snowball
				&& e.getEntity() instanceof Player
				&& e.getDamager() != e.getEntity()
		) {
			Snowball snowball = (Snowball)e.getDamager();
			if (snowball.getShooter() instanceof Player) {
				PlayerHandler infected = handlers.get((Player)e.getEntity());
				PlayerHandler infector = handlers.get((Player)e.getDamager());
				infected.getInfectedBy(infector);
			}
		}
	}

	// When a player's state changes, check if round has ended
	public void playerStateChange() {

		// If only one player is alive, he wins
		PlayerState[] livingStates = {PlayerState.HEALTHY, PlayerState.INCUBATING, PlayerState.INFECTED};
		PlayerState[] infectedStates = {PlayerState.INCUBATING, PlayerState.INFECTED};
		if (handlers.getPlayers(livingStates).size() == 1 || handlers.getPlayers(infectedStates).size() == 0) {
			winners = handlers.getPlayers(livingStates);
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

		switch (state) {
			case STARTING:

				// Run clocks
				updateManager.runTaskTimer(this, 0, 1);

				// Send chat message and start countdown
				chat.sendMessage("game-start");
				Timer countdown = new Timer(10*20, this);
				for (Player player : handlers.getPlayers()) {
					handlers.get(player).setStartTimer(countdown);
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
					if (i == infected) this.handlers.get(player).setState(PlayerState.INCUBATING);
					else chat.sendTitle("start", player);
				}
				break;
			case END:

				for (Player player : getServer().getOnlinePlayers()) {

					// Kill everyone
					this.handlers.get(player).setState(PlayerState.DEAD);

					// Fireworks
					Firework firework = player.getWorld().spawn(player.getLocation(), Firework.class);
					FireworkMeta fireworkMeta = firework.getFireworkMeta();
					Builder builder = FireworkEffect.builder();

					ArrayList<Color> colors = new ArrayList<>();
					colors.add(Color.GREEN);
					colors.add(Color.WHITE);

					fireworkMeta.addEffect(builder.trail(true).build());
					fireworkMeta.addEffect(builder.withFade(colors).build());
					fireworkMeta.addEffect(builder.with(FireworkEffect.Type.STAR).build());
					fireworkMeta.setPower(2);
					firework.setFireworkMeta(fireworkMeta);

					// Send winner title
					if (winners.size() == 1)
						chat.sendTitle("onewinner", player, new String[][]{{"name", winners.get(0).getDisplayName()}});
					else
						chat.sendTitle("multiplewinners", player);
				}
				break;
			case PAUSE:

				// End clocks
				updateManager.cancel();
		}

		this.state = state;
	}
}
