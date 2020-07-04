package me.noodian.corona.player;

import me.noodian.corona.*;
import me.noodian.corona.time.*;
import me.noodian.corona.ui.*;
import me.noodian.util.NameEncoder;
import org.bukkit.*;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.potion.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

import java.util.ArrayList;
import java.util.Arrays;

public class PlayerHandler extends Ticking implements Listener {

	public static final int COUGH_ID = 0, SNEEZE_ID = 1;

	private final PlayerPacketHandler packetHandler;
	private final HpDisplay hpBar;
	private final XpDisplay xpBar;
	private final ScoreDisplay scoreDisplay;
	private final Player player;
	private final Timer[] cooldowns;
	private Timer deathTimer, infectionTimer, globalTimer;
	private PlayerState state;
	
	public PlayerHandler(Player player, PlayerState state) {
		this.player = player;
		this.state = state;
		this.cooldowns = new Timer[2];
		this.packetHandler = new PlayerPacketHandler(player);
		this.hpBar = new HpDisplay(this);
		this.xpBar = new XpDisplay(this);
		this.scoreDisplay = new ScoreDisplay(this);
		
		// Disable flight
		player.setFlying(false);
		player.setAllowFlight(false);
		
		// Set self visible
		ArrayList<Player> otherPlayers = Game.get().getHandlers().getPlayers();
		for (Player other : otherPlayers)
			other.showPlayer(Game.get(), player);
		
		// Gamemode adventure
		player.setGameMode(GameMode.ADVENTURE);
		
		player.teleport(Game.get().getCurrentWorld().getSpawnLocation());
		Bukkit.getPluginManager().registerEvents(this, Game.get());
		Game.get().getHandlers().add(this);
		Game.get().getVelocity().add(this.player);
		clearUi();
		start();
	}

	@Override
	// If infected, play sounds and particles
	public void tick() {

		// Play effects
		if (state == PlayerState.INCUBATING)
			Game.get().getCurrentWorld().spawnParticle(Particle.SPELL, player.getEyeLocation(), 10, 0.5, 0.3, 0.5);
		else if (state == PlayerState.INFECTED)
			Game.get().getCurrentWorld().spawnParticle(Particle.SLIME, player.getEyeLocation(), 1, 0.5, -1.0, 0.5);

		// Update which XP countdown to display
		int held = player.getInventory().getHeldItemSlot();
		if (held < cooldowns.length && cooldowns[held] != null)
			xpBar.subscribeTo(cooldowns[held]);
		else if (infectionTimer != null)
			xpBar.subscribeTo(infectionTimer);
		else
			xpBar.subscribeTo(globalTimer);
	}

	@Override
	// Safely remove player handler
	public void remove() {
		clearUi();
		packetHandler.remove();
		hpBar.remove();
		xpBar.remove();
		scoreDisplay.remove();
		Game.get().getVelocity().remove(this.player);
		Game.get().getHandlers().remove(this);
		stop();
	}
	
	@EventHandler
	// Disable moving items in inventory
	public void inventoryClick(InventoryClickEvent e) {
		if (this.player == e.getWhoClicked())
			e.setCancelled(true);
	}

	@EventHandler
	// Disable dropping items
	public void onItemDrop(PlayerDropItemEvent e) {
		if (this.player == e.getPlayer())
			e.setCancelled(true);
	}

	@EventHandler
	// Disable swapping items
	public void onItemSwap(PlayerSwapHandItemsEvent e) {
		if (this.player == e.getPlayer())
			e.setCancelled(true);
	}

	@EventHandler
	// Safety measure, even though players can't die
	public void onPlayerRespawn(PlayerSpawnLocationEvent e) {
		if (e.getPlayer() == player)
			e.getPlayer().teleport(Game.get().getCurrentWorld().getSpawnLocation());
	}
	
	@EventHandler
	// Shoot projectile based on held item
	public void onPlayerInteract(PlayerInteractEvent e) {
		
		if (e.getPlayer() != player
				|| e.getAction() != Action.RIGHT_CLICK_AIR
				|| e.getItem() == null
				|| e.getItem().getItemMeta() == null
		) return;

		int id = NameEncoder.decode(e.getItem().getItemMeta().getDisplayName());
		if (id == COUGH_ID) {

			// Cough
			new Cough(this, 2.0d, 200L);
			usedItem(PlayerHandler.COUGH_ID);
			Game.get().getCurrentWorld().playSound(e.getPlayer().getEyeLocation(), Sound.ENTITY_WITHER_BREAK_BLOCK, 1.0f, 1.0f);
		} else if (id == SNEEZE_ID) {

			// Sneeze
			Snowball snowball = e.getPlayer().launchProjectile(Snowball.class);
			new Sneeze(snowball);
			usedItem(PlayerHandler.SNEEZE_ID);
			Game.get().getCurrentWorld().playSound(e.getPlayer().getEyeLocation(), Sound.ENTITY_ENDER_DRAGON_SHOOT, 1.0f, 1.0f);
		}
	}

	// Make the packet handler show a title
	public void showTitle(String text) {
		packetHandler.showTitle(text);
	}

	// Return the player object
	public Player getPlayer() {
		return player;
	}

	// Set off timer to infect self and heal other
	public void getInfectedBy(PlayerHandler other) {
		
		if (setState(PlayerState.INCUBATING)) {
			
			// When xp has ticked down, get infected and heal infector
			TimerCallback infectionCallback = args -> {
				if (setState(PlayerState.INFECTED))
					if (args.length == 1 && args[0] instanceof PlayerHandler)
						((PlayerHandler) args[0]).setState(PlayerState.HEALTHY);
			};
			
			// Start timer
			this.infectionTimer = new Timer(5*20, infectionCallback, new Object[]{other});
		}
	}

	// Cough / Sneeze
	private void usedItem(int itemId) {
		
		// When cooldown has finished, give back the item
		TimerCallback cooldownCallback = args -> {
			if (state == PlayerState.INFECTED && args.length == 1 && args[0] instanceof Integer) {
				switch ((int)args[0]) {
					case COUGH_ID:
						giveCough();
						player.playSound(player.getEyeLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 7, COUGH_ID);
						break;
					case SNEEZE_ID:
						giveSneeze();
						player.playSound(player.getEyeLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 7, SNEEZE_ID);
				}
			}
		};
		
		// Cough / Sneeze
		switch (itemId) {
			case COUGH_ID:
				giveFiller(COUGH_ID, Game.get().getTextManager().get("item.cough"));
				cooldowns[COUGH_ID] = new Timer(5*20, cooldownCallback, new Object[]{COUGH_ID});
				break;
			case SNEEZE_ID:
				giveFiller(SNEEZE_ID, Game.get().getTextManager().get("item.sneeze"));
				cooldowns[SNEEZE_ID] = new Timer(10*20, cooldownCallback, new Object[]{SNEEZE_ID});
		}
	}

	// Set the start timer
	public void setGlobalTimer(Timer globalTimer) {
		this.globalTimer = globalTimer;
	}

	// Set the scoreboard
	public void setScoreboard(Scoreboard scoreboard) {
		this.scoreDisplay.subscribeTo(scoreboard);
	}

	// Change the state of the player. Returns true if successful.
	public boolean setState(PlayerState state) {

		if (this.state == PlayerState.DEAD || this.state == state) return false;

		switch (state) {
			case HEALTHY:

				clearUi();

				break;
			case INCUBATING:

				// Disregard if already infected
				if (this.state == PlayerState.INFECTED) return false;

				clearUi();
				Game.get().getTextManager().sendTitle("infected", player);

				// Add effects
				PotionEffect nausea = new PotionEffect(PotionEffectType.CONFUSION, 8 * 20, 5);
				PotionEffect blindness = new PotionEffect(PotionEffectType.BLINDNESS, 5 * 20,  1);
				PotionEffect slowness = new PotionEffect(PotionEffectType.SLOW, 5 * 20, 3);
				player.addPotionEffects(Arrays.asList(nausea, blindness, slowness));
				Game.get().getCurrentWorld().playSound(player.getEyeLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.0f);

				break;
			case INFECTED:

				// Manage inventory
				clearUi();
				player.getInventory().setHelmet(new ItemStack(Material.SLIME_BLOCK));
				giveCough();
				giveSneeze();

				// Run countdown to death
				TimerCallback deathCallback = args -> setState(PlayerState.DEAD);
				// infectionlength(gameduration) = initiallength - gameduration * drain
				int initiallength = Game.get().getConfig().getInt("config.initial-infection-length", 25);
				double drain = Game.get().getConfig().getDouble("config.infection-length-drain", 0.2);
				int ticks = (int) (initiallength * 20 - Game.get().getGameDuration() * drain);
				deathTimer = new Timer(ticks, deathCallback);
				hpBar.subscribeTo(deathTimer);

				break;
			case DEAD:
				// TODO: Infection timer doesn't get cleared
				clearUi();

				// Set self invisible to alive
				ArrayList<Player> livingPlayers = Game.get().getHandlers().getPlayers(
						PlayerState.HEALTHY, PlayerState.INCUBATING, PlayerState.INFECTED);
				for (Player alive : livingPlayers)
					alive.hidePlayer(Game.get(), player);

				// Set dead visible to self
				ArrayList<Player> deadPlayers = Game.get().getHandlers().getPlayers(PlayerState.DEAD);
				for (Player dead : deadPlayers)
					player.showPlayer(Game.get(), dead);

				// UI
				Game.get().getCurrentWorld().playSound(player.getEyeLocation(), Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1.0f, 1.0f);
				if (livingPlayers.size() > 2)
					Game.get().getTextManager().sendTitle("dead", player);

				// Fly up
				player.setAllowFlight(true);
				player.setFlying(true);
				player.setVelocity(new Vector(0, 5, 0));
		}
		
		PlayerStateChangeEvent event = new PlayerStateChangeEvent(this, this.state);
		this.state = state;
		Bukkit.getServer().getPluginManager().callEvent(event);
		return true;
	}

	// Return the state
	public PlayerState getState() {
		return state;
	}

	// Clear player countdown, inventory, and effects
	private void clearUi() {

		// Clear timers
		if (deathTimer != null) {
			deathTimer.remove();
			deathTimer = null;
		}
		if (infectionTimer != null) {
			infectionTimer.remove();
			infectionTimer = null;
		}
		globalTimer = null;
		for (int i = 0; i < cooldowns.length; i++) {
			if (cooldowns[i] != null) {
				cooldowns[i].remove();
				cooldowns[i] = null;
			}
		}

		// Clear hunger and Hp
		player.setFoodLevel(20);
		hpBar.update();

		// Clear inventory
		player.getInventory().setArmorContents(null);
		player.getInventory().clear();

		// Clear effects
		for (PotionEffect effect : player.getActivePotionEffects())
			player.removePotionEffect(effect.getType());
	}

	// Fill the slot of the id with the cough item
	private void giveCough() {
		ItemStack cough = new ItemStack(Material.SLIME_BLOCK);
		ItemMeta meta = cough.getItemMeta();
		if (meta != null) {
			meta.setDisplayName(Game.get().getTextManager().get("item.cough") + NameEncoder.encode(COUGH_ID));
			cough.setItemMeta(meta);
		}
		player.getInventory().setItem(COUGH_ID, cough);
	}

	// Fill the slot of the id with the sneeze item
	private void giveSneeze() {
		ItemStack sneeze = new ItemStack(Material.SLIME_BALL);
		ItemMeta meta = sneeze.getItemMeta();
		if (meta != null) {
			meta.setDisplayName(Game.get().getTextManager().get("item.sneeze") + NameEncoder.encode(SNEEZE_ID));
			sneeze.setItemMeta(meta);
		}
		player.getInventory().setItem(SNEEZE_ID, sneeze);
	}

	// Fill the slot with an empty item
	private void giveFiller(int slot, String name) {
		ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
		ItemMeta meta = filler.getItemMeta();
		if (meta != null) {
			meta.setDisplayName("§8" + name);
			filler.setItemMeta(meta);
		}
		player.getInventory().setItem(slot, filler);
	}
}
