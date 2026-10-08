package bubbles.sabotage.plugin;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;

/**
 * Lets an admin temporarily switch into Creative mode to edit the map (place/break blocks,
 * water, etc.) without losing their current loadout. Toggled with {@code /sab edit}:
 * <ul>
 * <li>Entering saves the player's entire current state (inventory, armor, offhand, potion
 * effects, health, hunger, XP, flight, and game mode) and switches them to Creative - either
 * resuming the Creative inventory/build materials left over from their last edit session, or
 * starting from a clean slate the very first time.</li>
 * <li>Exiting saves whatever Creative inventory/materials they were holding (so the next
 * {@code /sab edit} resumes exactly where they left off) and restores the state captured on
 * entry.</li>
 * </ul>
 * Snapshots are in-memory only (not persisted to disk) and are lost if the server restarts.
 */
public final class EditMode {

	// Present only while a player is currently in edit mode; holds the loadout to restore on exit.
	private final HashMap<UUID, Snapshot> activeSessions = new HashMap<>();
	// Persists across edit sessions (until server restart) so re-entering edit mode resumes
	// the same Creative inventory/build materials instead of handing out a blank slate every time.
	private final HashMap<UUID, Snapshot> savedCreativeState = new HashMap<>();

	public boolean isEditing(Player p) {
		return activeSessions.containsKey(p.getUniqueId());
	}

	public void toggle(Player p) {
		if (isEditing(p)) {
			exit(p);
		} else {
			enter(p);
		}
	}

	private void enter(Player p) {
		activeSessions.put(p.getUniqueId(), Snapshot.capture(p));

		Snapshot resumeState = savedCreativeState.get(p.getUniqueId());
		if (resumeState != null) {
			resumeState.restore(p);
		} else {
			PlayerInventory inv = p.getInventory();
			inv.clear();
			inv.setArmorContents(new ItemStack[4]);
			inv.setItemInOffHand(new ItemStack(Material.AIR));
			for (PotionEffect effect : new ArrayList<>(p.getActivePotionEffects())) {
				p.removePotionEffect(effect.getType());
			}
			p.setHealth(p.getAttribute(Attribute.MAX_HEALTH).getBaseValue());
			p.setFoodLevel(20);
			p.setSaturation(20f);
			p.setExp(0f);
			p.setLevel(0);
		}
		p.setGameMode(GameMode.CREATIVE);
		p.setAllowFlight(true);
		p.setFlying(true);

		p.sendMessage("\u00A7aEdit mode enabled! Your loadout has been saved - run /sab edit again to restore it.");
	}

	private void exit(Player p) {
		savedCreativeState.put(p.getUniqueId(), Snapshot.capture(p));

		Snapshot previousState = activeSessions.remove(p.getUniqueId());
		if (previousState == null) {
			return;
		}
		previousState.restore(p);

		p.sendMessage("\u00A7aEdit mode disabled! Your previous loadout has been restored.");
	}

	private static final class Snapshot {
		private final ItemStack[] contents;
		private final ItemStack[] armor;
		private final ItemStack offhand;
		private final Collection<PotionEffect> potionEffects;
		private final double health;
		private final double maxHealth;
		private final int foodLevel;
		private final float saturation;
		private final float exp;
		private final int level;
		private final GameMode gameMode;
		private final boolean allowFlight;
		private final boolean flying;

		private Snapshot(ItemStack[] contents, ItemStack[] armor, ItemStack offhand,
				Collection<PotionEffect> potionEffects, double health, double maxHealth, int foodLevel,
				float saturation, float exp, int level, GameMode gameMode, boolean allowFlight, boolean flying) {
			this.contents = contents;
			this.armor = armor;
			this.offhand = offhand;
			this.potionEffects = potionEffects;
			this.health = health;
			this.maxHealth = maxHealth;
			this.foodLevel = foodLevel;
			this.saturation = saturation;
			this.exp = exp;
			this.level = level;
			this.gameMode = gameMode;
			this.allowFlight = allowFlight;
			this.flying = flying;
		}

		private static Snapshot capture(Player p) {
			PlayerInventory inv = p.getInventory();
			return new Snapshot(
					inv.getContents().clone(),
					inv.getArmorContents().clone(),
					inv.getItemInOffHand().clone(),
					new ArrayList<>(p.getActivePotionEffects()),
					p.getHealth(),
					p.getAttribute(Attribute.MAX_HEALTH).getBaseValue(),
					p.getFoodLevel(),
					p.getSaturation(),
					p.getExp(),
					p.getLevel(),
					p.getGameMode(),
					p.getAllowFlight(),
					p.isFlying());
		}

		private void restore(Player p) {
			PlayerInventory inv = p.getInventory();
			inv.setContents(contents);
			inv.setArmorContents(armor);
			inv.setItemInOffHand(offhand);
			for (PotionEffect effect : new ArrayList<>(p.getActivePotionEffects())) {
				p.removePotionEffect(effect.getType());
			}
			p.addPotionEffects(potionEffects);
			p.getAttribute(Attribute.MAX_HEALTH).setBaseValue(maxHealth);
			p.setHealth(Math.min(health, maxHealth));
			p.setFoodLevel(foodLevel);
			p.setSaturation(saturation);
			p.setExp(exp);
			p.setLevel(level);
			p.setGameMode(gameMode);
			p.setAllowFlight(allowFlight);
			p.setFlying(flying);
		}
	}
}
