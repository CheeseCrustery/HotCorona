package me.noodian.corona.player;

import me.noodian.corona.Corona;
import me.noodian.corona.time.Ticking;
import me.noodian.corona.time.Timer;
import me.noodian.corona.time.TimerCallback;
import me.noodian.corona.ui.HpDisplay;
import me.noodian.corona.ui.XpDisplay;
import me.noodian.util.NameEncoder;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.Random;


public class PlayerHandler implements Ticking {

	public static final int COUGH_ID = 0, SNEEZE_ID = 1;

	final PlayerPacketHandler packetHandler;

	private final Player player;
	private final PlayerList manager;
	private PlayerState state;
	private final HpDisplay hpDisplay;
	private final XpDisplay xpDisplay;
	private final Timer[] cooldowns;
	private Timer deathTimer, infectionTimer, startTimer;

	PlayerHandler(Player player, PlayerState state, PlayerList manager) {
		this.player = player;
		this.state = state;
		this.manager = manager;
		this.cooldowns = new Timer[2];
		this.packetHandler = new PlayerPacketHandler(player);

		this.hpDisplay = new HpDisplay(this);
		this.xpDisplay = new XpDisplay(this);
	}

	@Override
	// If infected, play sounds and particles
	public void tick() {

		// Play effects when infected
		if (state == PlayerState.INFECTED) {
			Random random = new Random();
			World world = Bukkit.getServer().getWorld("world");
			if (world != null) {
				if (random.nextFloat() > 0.9)
					world.playSound(player.getEyeLocation(), Sound.ENTITY_SLIME_DEATH, 1.0f, 1.0f);
				else if (random.nextFloat() > 0.4)
					world.spawnParticle(Particle.SLIME, player.getLocation(), 1);
			}
		}

		// Update which XP countdown to display
		int held = player.getInventory().getHeldItemSlot();
		if (held < cooldowns.length && cooldowns[held] != null)
			xpDisplay.subscribeTo(cooldowns[held]);
		else if (infectionTimer != null)
			xpDisplay.subscribeTo(infectionTimer);
		else
			xpDisplay.subscribeTo(startTimer);
	}

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
					break;
				case SNEEZE_ID:
					giveSneeze();
			}
		}
	};

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
				player.getInventory().clear(COUGH_ID);
				cooldowns[COUGH_ID] = new Timer(5*20, this.cooldownCallback, new Object[]{COUGH_ID});
				break;
			case SNEEZE_ID:
				player.getInventory().clear(SNEEZE_ID);
				cooldowns[SNEEZE_ID] = new Timer(10*20, this.cooldownCallback, new Object[]{SNEEZE_ID});
		}
	}

	// Set the start timer
	public void setStartTimer(Timer startTimer) {
		this.startTimer = startTimer;
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

				// Show title
				Corona.getInstance().chat.sendTitle("infected", player);

				// Clear inventory
				player.getInventory().clear();

				// Add effects
				PotionEffect sickness = new PotionEffect(PotionEffectType.CONFUSION, 5, 1);
				PotionEffect blindness = new PotionEffect(PotionEffectType.BLINDNESS, 5,  1);
				PotionEffect slowness = new PotionEffect(PotionEffectType.SLOW, 5, 1);
				player.addPotionEffects(Arrays.asList(sickness, blindness, slowness));
				Corona.getInstance().world.playSound(player.getEyeLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.0f);

				break;
			case INFECTED:

				// Clear inventory
				player.getInventory().clear();

				// Set helmet
				player.getInventory().setHelmet(new ItemStack(Material.SLIME_BLOCK));

				// Run countdown to death
				int ticks = 25 - (Corona.getInstance().getGameDuration()*20) / 5;
				deathTimer = new Timer(ticks, deathCallback);
				hpDisplay.subscribeTo(deathTimer);

				// Add items
				giveCough();
				giveSneeze();

				break;
			case DEAD:

				clearEverything();

				// Play sound
				Corona.getInstance().world.playSound(player.getEyeLocation(), Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1.0f, 1.0f);

				// Allow flying
				player.setAllowFlight(true);

				// Set self invisible to alive
				PlayerState[] livingStates = {PlayerState.HEALTHY, PlayerState.INCUBATING, PlayerState.INFECTED};
				for (Player alive : manager.getPlayers(livingStates)) {
					alive.hidePlayer(Corona.getInstance(), player);
				}

				// Set dead visible to self
				for (Player dead : manager.getPlayers(PlayerState.DEAD)) {
					dead.hidePlayer(Corona.getInstance(), player);
				}
		}

		manager.stateChange(this, this.state, state);
		this.state = state;
		return true;
	}

	// Return the state
	PlayerState getState() {
		return state;
	}

	// Clear player countdown, inventory, and effects
	private void clearEverything() {

		if (deathTimer != null) {
			deathTimer.remove();
			deathTimer = null;
		}

		if (infectionTimer != null) {
			infectionTimer.remove();
			infectionTimer = null;
		}

		startTimer = null;

		for (int i = 0; i < cooldowns.length; i++) {
			if (cooldowns[i] != null) {
				cooldowns[i].remove();
				cooldowns[i] = null;
			}
		}
		player.getInventory().clear();
		for (PotionEffect effect : player.getActivePotionEffects())
			player.removePotionEffect(effect.getType());
	}

	// Fill the slot of the id with the cough item
	private void giveCough() {
		ItemStack cough = new ItemStack(Material.SLIME_BLOCK);
		ItemMeta meta = cough.getItemMeta();
		if (meta != null) {
			meta.setDisplayName(Corona.getInstance().config.getString("item.cough") + NameEncoder.encode(COUGH_ID));
			cough.setItemMeta(meta);
		}
		player.getInventory().setItem(COUGH_ID, cough);
	}

	// Fill the slot of the id with the sneeze item
	private void giveSneeze() {
		ItemStack sneeze = new ItemStack(Material.SLIME_BALL);
		ItemMeta meta = sneeze.getItemMeta();
		if (meta != null) {
			meta.setDisplayName(Corona.getInstance().config.getString("item.sneeze") + NameEncoder.encode(SNEEZE_ID));
			sneeze.setItemMeta(meta);
		}
		player.getInventory().setItem(SNEEZE_ID, sneeze);
	}
}
