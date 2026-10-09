package bubbles.sabotage.plugin;

import bubbles.sabotage.plugin.fileIO.EditModeIO;
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
import java.util.List;
import java.util.Map;
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
 * Inventories are persisted to disk via YAML files (keyed by player UUID) and survive server restarts.
 */
public final class EditMode {

	// Present only while a player is currently in edit mode; holds the loadout to restore on exit.
	private final HashMap<UUID, Snapshot> activeSessions = new HashMap<>();
	
	// Debug flag for verbose edit mode logging
	private static boolean debugMode = false;

	public boolean isEditing(Player p) {
		return activeSessions.containsKey(p.getUniqueId());
	}

	public static void setDebugMode(boolean enabled) {
		debugMode = enabled;
	}

	public static boolean isDebugMode() {
		return debugMode;
	}

	public void toggle(Player p) {
		if (isEditing(p)) {
			exit(p);
		} else {
			enter(p);
		}
	}

	/**
	 * Called when a player joins the server. Restores their saved inventory state
	 * (either survival or creative) and their game mode from the previous session.
	 */
	public void restoreInventoryState(Player p) {
		debugLog("JOIN: Player " + p.getName() + " joined");
		
		// Check what mode the player was in when they logged out
		GameMode currentMode = EditModeIO.getCurrentMode(p);
		debugLog("JOIN: Current mode from YAML: " + currentMode);
		
		if (currentMode == null) {
			debugLog("JOIN: No saved states - new player");
			return;
		}
		
		// Load both survival and creative states
		Map<String, Object> savedSurvivalState = EditModeIO.loadInventoryState(p, GameMode.SURVIVAL);
		Map<String, Object> savedCreativeState = EditModeIO.loadInventoryState(p, GameMode.CREATIVE);
		
		debugLog("JOIN: Survival state exists: " + (savedSurvivalState != null));
		debugLog("JOIN: Creative state exists: " + (savedCreativeState != null));
		
		if (currentMode == GameMode.CREATIVE) {
			debugLog("JOIN: Restoring to CREATIVE mode");
			// Player was in creative/edit mode - restore to edit mode
			// Use the survival state as the "previousState" they'll return to when exiting edit mode
			if (savedSurvivalState != null) {
				logInventory(p, "JOIN - Loading survival state as previousState", (List<?>) savedSurvivalState.get("contents"));
				activeSessions.put(p.getUniqueId(), createSnapshotFromMap(p, savedSurvivalState));
				debugLog("JOIN: Saved survival state to activeSessions as previousState");
			} else {
				activeSessions.put(p.getUniqueId(), Snapshot.capture(p));
				debugLog("JOIN: No survival state, using current player state as previousState");
			}
			if (savedCreativeState != null) {
				logInventory(p, "JOIN - Loading creative state", (List<?>) savedCreativeState.get("contents"));
				EditModeIO.restoreInventoryState(p, savedCreativeState);
				Snapshot afterRestore = Snapshot.capture(p);
				logInventory(p, "JOIN - After restoring creative state", afterRestore.contents);
			} else {
				debugLog("JOIN: No creative state saved");
			}
			p.setGameMode(GameMode.CREATIVE);
			p.setAllowFlight(true);
			p.setFlying(true);
			debugLog("JOIN: Player marked as editing (isEditing=" + isEditing(p) + ")");
			p.sendMessage("\u00A7aEdit mode resumed! You're back in edit mode with your previous inventory.");
		} else if (currentMode == GameMode.SURVIVAL) {
			debugLog("JOIN: Restoring to SURVIVAL mode");
			// Player was in survival mode - restore to survival
			if (savedSurvivalState != null) {
				logInventory(p, "JOIN - Loading survival state", (List<?>) savedSurvivalState.get("contents"));
				EditModeIO.restoreInventoryState(p, savedSurvivalState);
				Snapshot afterRestore = Snapshot.capture(p);
				logInventory(p, "JOIN - After restoring survival state", afterRestore.contents);
				p.sendMessage("\u00A7aYour inventory has been restored from your last session.");
			}
		}
	}

	/**
	 * Creates a Snapshot from a loaded state map (used for login restoration).
	 * This snapshot can be restored later when exiting edit mode.
	 */
	private Snapshot createSnapshotFromMap(Player p, Map<String, Object> state) {
		@SuppressWarnings("unchecked")
		List<ItemStack> contents = (List<ItemStack>) state.get("contents");
		@SuppressWarnings("unchecked")
		List<ItemStack> armor = (List<ItemStack>) state.get("armor");
		ItemStack offhand = (ItemStack) state.get("offhand");
		@SuppressWarnings("unchecked")
		List<PotionEffect> potionEffects = (List<PotionEffect>) state.get("potionEffects");
		
		// Properly convert lists to arrays with correct sizes
		ItemStack[] contentsArray = new ItemStack[36];
		if (contents != null) {
			for (int i = 0; i < Math.min(contents.size(), 36); i++) {
				contentsArray[i] = contents.get(i);
			}
		}
		
		ItemStack[] armorArray = new ItemStack[4];
		if (armor != null) {
			for (int i = 0; i < Math.min(armor.size(), 4); i++) {
				armorArray[i] = armor.get(i);
			}
		}
		
		return new Snapshot(
				contentsArray,
				armorArray,
				offhand != null ? offhand : new ItemStack(Material.AIR),
				potionEffects != null ? potionEffects : new ArrayList<>(),
				(double) state.get("health"),
				(double) state.get("maxHealth"),
				(int) state.get("foodLevel"),
				(float) state.get("saturation"),
				(float) state.get("exp"),
				(int) state.get("level"),
				(GameMode) state.get("gameMode"),
				(boolean) state.get("allowFlight"),
				(boolean) state.get("flying"));
	}

	private void enter(Player p) {
		Snapshot snapshot = Snapshot.capture(p);
		activeSessions.put(p.getUniqueId(), snapshot);
		
		logInventory(p, "ENTER EDIT MODE - Saving survival inventory", snapshot.contents);
		
		// Save the survival state to disk
		EditModeIO.saveInventoryState(p,
				snapshot.contents,
				snapshot.armor,
				snapshot.offhand,
				snapshot.potionEffects,
				snapshot.health,
				snapshot.maxHealth,
				snapshot.foodLevel,
				snapshot.saturation,
				snapshot.exp,
				snapshot.level,
				GameMode.SURVIVAL,
				snapshot.allowFlight,
				snapshot.flying);
		
		debugLog("ENTER: Saved survival inventory to disk");

		// Try to load saved creative state from disk
		Map<String, Object> savedState = EditModeIO.loadInventoryState(p, GameMode.CREATIVE);
		if (savedState != null) {
			logInventory(p, "ENTER EDIT MODE - Loading creative inventory from disk", (List<?>) savedState.get("contents"));
			EditModeIO.restoreInventoryState(p, savedState);
			Snapshot afterRestore = Snapshot.capture(p);
			logInventory(p, "ENTER EDIT MODE - After restoring creative from disk", afterRestore.contents);
		} else {
			// First time entering edit mode - provide blank creative inventory
			logInventory(p, "ENTER EDIT MODE - First time, creating blank creative inventory", new ItemStack[0]);
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
		
		// Now update the currentMode flag to CREATIVE
		Snapshot currentCreativeState = Snapshot.capture(p);
		logInventory(p, "ENTER EDIT MODE - About to save currentMode=CREATIVE with inventory", currentCreativeState.contents);
		EditModeIO.saveInventoryState(p,
				currentCreativeState.contents,
				currentCreativeState.armor,
				currentCreativeState.offhand,
				currentCreativeState.potionEffects,
				currentCreativeState.health,
				currentCreativeState.maxHealth,
				currentCreativeState.foodLevel,
				currentCreativeState.saturation,
				currentCreativeState.exp,
				currentCreativeState.level,
				GameMode.CREATIVE,
				currentCreativeState.allowFlight,
				currentCreativeState.flying);
		
		debugLog("ENTER: Set currentMode=CREATIVE");

		p.sendMessage("\u00A7aEdit mode enabled! Your loadout has been saved - run /sab edit again to restore it.");
	}

	private void exit(Player p) {
		Snapshot currentSnapshot = Snapshot.capture(p);
		
		logInventory(p, "EXIT EDIT MODE - Current creative inventory", currentSnapshot.contents);
		
		// Save the creative mode inventory to disk
		EditModeIO.saveInventoryState(p,
				currentSnapshot.contents,
				currentSnapshot.armor,
				currentSnapshot.offhand,
				currentSnapshot.potionEffects,
				currentSnapshot.health,
				currentSnapshot.maxHealth,
				currentSnapshot.foodLevel,
				currentSnapshot.saturation,
				currentSnapshot.exp,
				currentSnapshot.level,
				GameMode.CREATIVE,
				currentSnapshot.allowFlight,
				currentSnapshot.flying);
		
		debugLog("EXIT: Saved creative inventory to YAML");

		Snapshot previousState = activeSessions.remove(p.getUniqueId());
		if (previousState == null) {
			debugLog("EXIT: ERROR - No previous state found!");
			return;
		}
		
		logInventory(p, "EXIT EDIT MODE - Previous survival inventory", previousState.contents);
		
		// Save the survival mode inventory to disk with SURVIVAL gameMode
		// (this tells us on login that the player was in survival mode)
		EditModeIO.saveInventoryState(p,
				previousState.contents,
				previousState.armor,
				previousState.offhand,
				previousState.potionEffects,
				previousState.health,
				previousState.maxHealth,
				previousState.foodLevel,
				previousState.saturation,
				previousState.exp,
				previousState.level,
				GameMode.SURVIVAL,
				previousState.allowFlight,
				previousState.flying);
		
		debugLog("EXIT: Saved survival inventory to YAML with gameMode=SURVIVAL");
		
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

		Snapshot(ItemStack[] contents, ItemStack[] armor, ItemStack offhand,
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

	private void logInventory(Player p, String context, ItemStack[] items) {
		if (!debugMode) {
			return;
		}
		if (items == null) {
			System.out.println("[EditMode] " + context + ": NULL array");
			return;
		}
		StringBuilder sb = new StringBuilder();
		sb.append("[EditMode] ").append(context).append(" (").append(items.length).append(" slots): ");
		int count = 0;
		for (int i = 0; i < items.length && i < 10; i++) {
			if (items[i] != null && items[i].getType() != Material.AIR) {
				if (count > 0) sb.append(", ");
				sb.append("[").append(i).append("]").append(items[i].getType()).append("x").append(items[i].getAmount());
				count++;
			}
		}
		if (count == 0) sb.append("EMPTY");
		System.out.println(sb.toString());
	}

	private void logInventory(Player p, String context, List<?> items) {
		if (!debugMode) {
			return;
		}
		if (items == null) {
			System.out.println("[EditMode] " + context + ": NULL list");
			return;
		}
		StringBuilder sb = new StringBuilder();
		sb.append("[EditMode] ").append(context).append(" (").append(items.size()).append(" items): ");
		int count = 0;
		for (int i = 0; i < items.size() && i < 10; i++) {
			Object item = items.get(i);
			if (item instanceof ItemStack) {
				ItemStack stack = (ItemStack) item;
				if (stack != null && stack.getType() != Material.AIR) {
					if (count > 0) sb.append(", ");
					sb.append("[").append(i).append("]").append(stack.getType()).append("x").append(stack.getAmount());
					count++;
				}
			}
		}
		if (count == 0) sb.append("EMPTY");
		System.out.println(sb.toString());
	}

	/**
	 * Saves the player's current creative inventory to disk.
	 * Called when they close their inventory screen while in edit mode.
	 */
	public void saveCreativeInventoryToDisk(Player p) {
		if (!isEditing(p)) {
			return;
		}

		Snapshot current = Snapshot.capture(p);
		logInventory(p, "AUTO-SAVE CREATIVE - Saving current inventory", current.contents);
		
		EditModeIO.saveInventoryState(p,
				current.contents,
				current.armor,
				current.offhand,
				current.potionEffects,
				current.health,
				current.maxHealth,
				current.foodLevel,
				current.saturation,
				current.exp,
				current.level,
				GameMode.CREATIVE,
				current.allowFlight,
				current.flying);
		
		debugLog("AUTO-SAVE: Creative inventory saved to disk");
	}

	private static void debugLog(String message) {
		if (debugMode) {
			System.out.println("[EditMode] " + message);
		}
	}
}

