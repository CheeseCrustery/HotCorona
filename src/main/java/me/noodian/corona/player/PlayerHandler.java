package me.noodian.corona.player;

import me.noodian.corona.Corona;
import me.noodian.corona.time.*;
import me.noodian.corona.ui.*;
import me.noodian.util.NameEncoder;
import org.bukkit.*;
import org.bukkit.potion.*;
import org.bukkit.attribute.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;

public class PlayerHandler extends Ticking {

	public static final int COUGH_ID = 0, SNEEZE_ID = 1;

	final PlayerPacketHandler packetHandler;

	private Player player;
	private PlayerState state;
	private final HpDisplay hpDisplay;
	private final XpDisplay xpDisplay;
	private final Timer[] cooldowns;
	private Timer deathTimer, infectionTimer, globalTimer;

	// When hp has ticked down, die
	public final TimerCallback deathCallback = args -> setState(PlayerState.DEAD);

	// When xp has ticked down, get infected and heal infector
	public final TimerCallback infectionCallback = args -> {
		if (setState(PlayerState.INFECTED)) {
			if (args.length == 1 && args[0] instanceof PlayerHandler) {
				((PlayerHandler) args[0]).setState(PlayerState.HEALTHY);
			}
		}
	};

	// When cooldown has finished, give back the cough
	public final TimerCallback cooldownCallback = args -> {
		if (state == PlayerState.INFECTED && args.length == 1 && args[0] instanceof Integer) {
			switch ((int)args[0]) {
				case COUGH_ID:
					giveCough();
					player.playSound(player.getEyeLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 7, COUGH_ID);
					break;
				case SNEEZE_ID:
					player.playSound(player.getEyeLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 7, SNEEZE_ID);
					giveSneeze();
			}
		}
	};

	public PlayerHandler(Player player, PlayerState state) {
		this.player = player;
		this.state = state;
		this.cooldowns = new Timer[2];
		this.packetHandler = new PlayerPacketHandler(player);

		this.hpDisplay = new HpDisplay(this);
		this.xpDisplay = new XpDisplay(this);

		AttributeInstance maxHealth = this.player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
		if (maxHealth != null) this.player.setHealth(maxHealth.getValue());
		clearEverything();

		Corona.get().handlers.add(this);
		start();
	}

	@Override
	// If infected, play sounds and particles
	public void tick() {

		// Play effects
		if (state == PlayerState.INCUBATING)
			Corona.get().world.spawnParticle(Particle.SPELL, player.getEyeLocation(), 10, 0.5, 0.3, 0.5);
		else if (state == PlayerState.INFECTED)
			Corona.get().world.spawnParticle(Particle.SLIME, player.getEyeLocation(), 1, 0.5, -1.0, 0.5);

		// Update which XP countdown to display
		int held = player.getInventory().getHeldItemSlot();
		if (held < cooldowns.length && cooldowns[held] != null)
			xpDisplay.subscribeTo(cooldowns[held]);
		else if (infectionTimer != null)
			xpDisplay.subscribeTo(infectionTimer);
		else
			xpDisplay.subscribeTo(globalTimer);
	}

	@Override
	// Safely remove player handler
	public void remove() {
		Corona.get().handlers.remove(this);
		packetHandler.remove();
		stop();
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
		if (setState(PlayerState.INCUBATING))
			this.infectionTimer = new Timer(5*20, infectionCallback, new Object[]{other});
	}

	// Cough / Sneeze
	public void usedItem(int itemId) {

		switch (itemId) {
			case COUGH_ID:
				giveFiller(COUGH_ID, Corona.get().text.get("item.cough"));
				cooldowns[COUGH_ID] = new Timer(5*20, this.cooldownCallback, new Object[]{COUGH_ID});
				break;
			case SNEEZE_ID:
				giveFiller(SNEEZE_ID, Corona.get().text.get("item.sneeze"));
				cooldowns[SNEEZE_ID] = new Timer(10*20, this.cooldownCallback, new Object[]{SNEEZE_ID});
		}
	}

	// Set the start timer
	public void setGlobalTimer(Timer globalTimer) {
		this.globalTimer = globalTimer;
	}

	// Change the state of the player. Returns true if successful.
	public boolean setState(PlayerState state) {

		if (this.state == PlayerState.DEAD || this.state == state) return false;

		switch (state) {
			case HEALTHY:

				clearEverything();

				break;
			case INCUBATING:

				// Disregard if already infected
				if (this.state == PlayerState.INFECTED) return false;

				clearEverything();
				Corona.get().text.sendTitle("infected", player);

				// Add effects
				PotionEffect nausea = new PotionEffect(PotionEffectType.CONFUSION, 8 * 20, 5);
				PotionEffect blindness = new PotionEffect(PotionEffectType.BLINDNESS, 5 * 20,  1);
				PotionEffect slowness = new PotionEffect(PotionEffectType.SLOW, 5 * 20, 3);
				player.addPotionEffects(Arrays.asList(nausea, blindness, slowness));
				Corona.get().world.playSound(player.getEyeLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.0f);

				break;
			case INFECTED:

				// Manage inventory
				clearEverything();
				player.getInventory().setHelmet(new ItemStack(Material.SLIME_BLOCK));
				giveCough();
				giveSneeze();

				// Run countdown to death
				// infectionlength(gameduration) = initiallength - gameduration * drain
				int initiallength = Corona.get().config.getInt("config.initial-infection-length", 25);
				double drain = Corona.get().config.getDouble("config.infection-length-drain", 0.2);
				int ticks = (int) (initiallength * 20 - Corona.get().getGameDuration() * drain);
				deathTimer = new Timer(ticks, deathCallback);
				hpDisplay.subscribeTo(deathTimer);

				break;
			case DEAD:

				clearEverything();

				// Set self invisible to alive
				ArrayList<Player> livingPlayers = Corona.get().handlers.getPlayers(
						PlayerState.HEALTHY, PlayerState.INCUBATING, PlayerState.INFECTED);
				for (Player alive : livingPlayers) {
					alive.hidePlayer(Corona.get(), player);
				}

				// Set dead visible to self
				ArrayList<Player> deadPlayers = Corona.get().handlers.getPlayers(PlayerState.DEAD);
				for (Player dead : deadPlayers) {
					dead.hidePlayer(Corona.get(), player);
				}

				// UI
				Corona.get().world.playSound(player.getEyeLocation(), Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1.0f, 1.0f);
				if (livingPlayers.size() > 2)
					Corona.get().text.sendTitle("dead", player);

				// Fly up
				player.setAllowFlight(true);
				player.setFlying(true);
				player.setVelocity(new Vector(0, 5, 0));
		}

		Corona.get().handlers.stateChange(this, this.state, state);
		this.state = state;
		return true;
	}

	// Return the state
	public PlayerState getState() {
		return state;
	}

	// Clear player countdown, inventory, and effects
	private void clearEverything() {

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

		// Clear hunger
		player.setFoodLevel(20);

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
			meta.setDisplayName(Corona.get().text.get("item.cough") + NameEncoder.encode(COUGH_ID));
			cough.setItemMeta(meta);
		}
		player.getInventory().setItem(COUGH_ID, cough);
	}

	// Fill the slot of the id with the sneeze item
	private void giveSneeze() {
		ItemStack sneeze = new ItemStack(Material.SLIME_BALL);
		ItemMeta meta = sneeze.getItemMeta();
		if (meta != null) {
			meta.setDisplayName(Corona.get().text.get("item.sneeze") + NameEncoder.encode(SNEEZE_ID));
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
