package me.noodian.corona.player;

import me.noodian.corona.Corona;
import me.noodian.corona.Ticking;
import me.noodian.corona.UpdateManager;
import me.noodian.util.NameEncoder;
import me.noodian.util.countdown.BarType;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import me.noodian.util.countdown.CountdownCallback;
import me.noodian.util.countdown.Countdown;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.Random;


class PlayerHandler implements CountdownCallback, Ticking {

	private static final int COUGH_ID = 0, SNEEZE_ID = 1;

	final PlayerPacketHandler packetHandler;

	private final Player player;
	private final PlayerManager manager;
	private PlayerState state;
	private Countdown hpCountdown;
	private Countdown[] cooldowns;

	PlayerHandler(Player player, PlayerState state, PlayerManager manager) {
		this.player = player;
		this.state = state;
		this.manager = manager;
		this.cooldowns = new Countdown[2];
		this.packetHandler = new PlayerPacketHandler(player);

		UpdateManager.GetInstance().Objects.add(this);
	}

	@Override
	// When timer finishes, reset cooldown / get infected / die
	public void Finished(Countdown countdown) {

		// Infected -> Dead
		if (hpCountdown == countdown)
			setState(PlayerState.DEAD);

		// Incubating -> Infected
		else if (xpCountdown == countdown)
			setState(PlayerState.INFECTED);

		// Cough cooldown finished
		else if (cooldowns[COUGH_ID] == countdown)
			giveCough();

		// Sneeze cooldown finished
		else if (cooldowns[SNEEZE_ID] == countdown)
			giveSneeze();
	}

	@Override
	// If infected, play sounds and particles
	public void Tick() {

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

		// Update hp countdown
		if (hpCountdown != null)
			hpCountdown.tick();

		// Update countdown, override with cooldown
		int held = player.getInventory().getHeldItemSlot();
		if (xpCountdown != null)
			xpCountdown.tick();
		for (int i = 0; i < cooldowns.length; i++) {
			if (i == held)
				cooldowns[i].tick();
			else
				cooldowns[i].tick(false);
		}
	}

	// Get the player's state
	PlayerState getState() {
		return state;
	}

	// Change the state of the player. Returns true if successful.
	boolean setState(PlayerState state) {

		if (this.state == PlayerState.DEAD || this.state == state) return false;

		switch (state) {
			case HEALTHY:

				clearEverything();
				break;
			case INCUBATING:

				// Disregard if already infected
				if (this.state == PlayerState.INFECTED) return false;

				// Show title
				Corona.GetInstance().ServerChat.sendTitle("infected", player);

				// Clear inventory
				player.getInventory().clear();

				// Add effects
				PotionEffect sickness = new PotionEffect(PotionEffectType.CONFUSION, 5, 1);
				PotionEffect blindness = new PotionEffect(PotionEffectType.BLINDNESS, 5,  1);
				PotionEffect slowness = new PotionEffect(PotionEffectType.SLOW, 5, 1);
				player.addPotionEffects(Arrays.asList(sickness, blindness, slowness));
				Bukkit.getServer().getWorld("world").playSound(player.getEyeLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.0f);

				// Run countdown to infection
				xpCountdown = new Countdown(5, this, this.player, BarType.XP);

				break;
			case INFECTED:

				// Clear inventory
				player.getInventory().clear();

				// Set helmet
				player.getInventory().setHelmet(new ItemStack(Material.SLIME_BLOCK));

				// Run countdown to death
				int time = 25 - Corona.GetInstance().GetGameDuration() / 5;
				hpCountdown = new Countdown(time, this, this.player, BarType.HEALTH);

				// Add items
				giveCough();
				giveSneeze();

				break;
			case DEAD:

				clearEverything();

				// Play sound
				Bukkit.getServer().getWorld("world").playSound(player.getEyeLocation(), Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1.0f, 1.0f);

				// Allow flying
				player.setAllowFlight(true);

				// Set self invisible to alive
				PlayerState[] livingStates = {PlayerState.HEALTHY, PlayerState.INCUBATING, PlayerState.INFECTED};
				for (Player alive : manager.getPlayers(livingStates)) {
					alive.hidePlayer(Corona.GetInstance(), player);
				}

				// Set dead visible to self
				for (Player dead : manager.getPlayers(PlayerState.DEAD)) {
					dead.hidePlayer(Corona.GetInstance(), player);
				}
		}

		this.state = state;
		return true;
	}

	// Change the countdown instance
	void setXpCountdown(Countdown countdown) {
		xpCountdown = countdown;
	}

	// Cough / Sneeze
	void usedItem(int itemId) {

		switch (itemId) {
			case COUGH_ID:
				player.getInventory().clear(COUGH_ID);
				cooldowns[COUGH_ID] = new Countdown(5, this, player, BarType.XP);
				break;
			case SNEEZE_ID:
				player.getInventory().clear(SNEEZE_ID);
				cooldowns[SNEEZE_ID] = new Countdown(10, this, player, BarType.XP);
		}
	}

	// Clear player countdown, inventory, and effects
	private void clearEverything() {
		hpCountdown = null;
		xpCountdown = null;
		cooldowns = new Countdown[2];
		player.getInventory().clear();
		for (PotionEffect effect : player.getActivePotionEffects())
			player.removePotionEffect(effect.getType());
	}

	// Fill the slot of the id with the cough item
	private void giveCough() {
		ItemStack cough = new ItemStack(Material.SLIME_BLOCK);
		ItemMeta meta = cough.getItemMeta();
		if (meta != null) {
			meta.setDisplayName(Corona.GetInstance().Config.getString("item.cough") + NameEncoder.Encode(COUGH_ID));
			cough.setItemMeta(meta);
		}
		player.getInventory().setItem(COUGH_ID, cough);
	}

	// Fill the slot of the id with the sneeze item
	private void giveSneeze() {
		ItemStack sneeze = new ItemStack(Material.SLIME_BALL);
		ItemMeta meta = sneeze.getItemMeta();
		if (meta != null) {
			meta.setDisplayName(Corona.GetInstance().Config.getString("item.sneeze") + NameEncoder.Encode(SNEEZE_ID));
			sneeze.setItemMeta(meta);
		}
		player.getInventory().setItem(SNEEZE_ID, sneeze);
	}
}
