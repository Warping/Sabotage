package bubbles.sabotage.plugin.fileIO;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class EditModeIO {

	private static final String EDIT_MODE_DIR = "plugins/Sabotage/editmode";

	static {
		File dir = new File(EDIT_MODE_DIR);
		if (!dir.exists()) {
			dir.mkdirs();
		}
	}

	/**
	 * Saves a player's inventory state (both normal and edit mode) to a YAML file.
	 * The filename is the player's UUID.
	 */
	public static void saveInventoryState(Player p, ItemStack[] contents, ItemStack[] armor, ItemStack offhand,
			Collection<PotionEffect> potionEffects, double health, double maxHealth, int foodLevel,
			float saturation, float exp, int level, GameMode gameMode, boolean allowFlight, boolean flying) {

		UUID uuid = p.getUniqueId();
		File file = new File(EDIT_MODE_DIR + "/" + uuid + ".yml");

		FileConfiguration config = YamlConfiguration.loadConfiguration(file);

		// Save the current game mode inventory (either survival or creative)
		String modeKey = gameMode == GameMode.CREATIVE ? "creative" : "survival";
		config.set(modeKey + ".contents", contents);
		config.set(modeKey + ".armor", armor);
		config.set(modeKey + ".offhand", offhand);
		config.set(modeKey + ".potionEffects", new ArrayList<>(potionEffects));
		config.set(modeKey + ".health", health);
		config.set(modeKey + ".maxHealth", maxHealth);
		config.set(modeKey + ".foodLevel", foodLevel);
		config.set(modeKey + ".saturation", saturation);
		config.set(modeKey + ".exp", exp);
		config.set(modeKey + ".level", level);
		config.set(modeKey + ".gameMode", gameMode.toString());
		config.set(modeKey + ".allowFlight", allowFlight);
		config.set(modeKey + ".flying", flying);
		config.set(modeKey + ".savedAt", System.currentTimeMillis());
		
		// Track which mode the player is currently in
		config.set("currentMode", gameMode.toString());

		try {
			config.save(file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Loads a player's saved inventory state from YAML file, or null if not found.
	 */
	public static Map<String, Object> loadInventoryState(Player p, GameMode targetMode) {
		UUID uuid = p.getUniqueId();
		File file = new File(EDIT_MODE_DIR + "/" + uuid + ".yml");

		if (!file.exists()) {
			return null;
		}

		FileConfiguration config = YamlConfiguration.loadConfiguration(file);
		String modeKey = targetMode == GameMode.CREATIVE ? "creative" : "survival";

		if (!config.contains(modeKey)) {
			return null;
		}

		Map<String, Object> state = new HashMap<>();
		state.put("contents", config.getList(modeKey + ".contents"));
		state.put("armor", config.getList(modeKey + ".armor"));
		state.put("offhand", config.get(modeKey + ".offhand"));
		state.put("potionEffects", config.getList(modeKey + ".potionEffects"));
		state.put("health", config.getDouble(modeKey + ".health"));
		state.put("maxHealth", config.getDouble(modeKey + ".maxHealth"));
		state.put("foodLevel", config.getInt(modeKey + ".foodLevel"));
		state.put("saturation", (float) config.getDouble(modeKey + ".saturation"));
		state.put("exp", (float) config.getDouble(modeKey + ".exp"));
		state.put("level", config.getInt(modeKey + ".level"));
		state.put("gameMode", GameMode.valueOf(config.getString(modeKey + ".gameMode")));
		state.put("allowFlight", config.getBoolean(modeKey + ".allowFlight"));
		state.put("flying", config.getBoolean(modeKey + ".flying"));

		return state;
	}

	/**
	 * Gets the current game mode the player was in when they logged out.
	 * Returns null if no saved state exists.
	 */
	public static GameMode getCurrentMode(Player p) {
		UUID uuid = p.getUniqueId();
		File file = new File(EDIT_MODE_DIR + "/" + uuid + ".yml");

		if (!file.exists()) {
			return null;
		}

		FileConfiguration config = YamlConfiguration.loadConfiguration(file);
		String currentModeStr = config.getString("currentMode");
		
		if (currentModeStr == null) {
			return null;
		}
		
		try {
			return GameMode.valueOf(currentModeStr);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	/**
	 * Checks if a player has a saved inventory state for the given game mode.
	 */
	public static boolean hasSavedState(Player p, GameMode gameMode) {
		UUID uuid = p.getUniqueId();
		File file = new File(EDIT_MODE_DIR + "/" + uuid + ".yml");

		if (!file.exists()) {
			return false;
		}

		FileConfiguration config = YamlConfiguration.loadConfiguration(file);
		String modeKey = gameMode == GameMode.CREATIVE ? "creative" : "survival";
		return config.contains(modeKey);
	}

	/**
	 * Restores a player's inventory state from the saved data.
	 */
	public static void restoreInventoryState(Player p, Map<String, Object> state) {
		if (state == null) {
			return;
		}

		PlayerInventory inv = p.getInventory();

		@SuppressWarnings("unchecked")
		List<ItemStack> contentsList = (List<ItemStack>) state.get("contents");
		if (bubbles.sabotage.plugin.EditMode.isDebugMode()) {
			System.out.println("[EditModeIO] RESTORE: contentsList size = " + (contentsList != null ? contentsList.size() : "null"));
			System.out.println("[EditModeIO] RESTORE: inv.getContents().length = " + inv.getContents().length);
		}
		
		if (contentsList != null) {
			if (bubbles.sabotage.plugin.EditMode.isDebugMode()) {
				System.out.println("[EditModeIO] RESTORE: Setting " + contentsList.size() + " contents items");
			}
			ItemStack[] contentsArray = new ItemStack[Math.max(contentsList.size(), inv.getContents().length)];
			for (int i = 0; i < contentsList.size(); i++) {
				contentsArray[i] = contentsList.get(i);
			}
			inv.setContents(contentsArray);
		}

		@SuppressWarnings("unchecked")
		List<ItemStack> armorList = (List<ItemStack>) state.get("armor");
		if (bubbles.sabotage.plugin.EditMode.isDebugMode()) {
			System.out.println("[EditModeIO] RESTORE: armorList = " + (armorList != null ? armorList.size() : "null"));
		}
		if (armorList != null) {
			ItemStack[] armorArray = new ItemStack[4];
			for (int i = 0; i < Math.min(armorList.size(), 4); i++) {
				armorArray[i] = armorList.get(i);
			}
			inv.setArmorContents(armorArray);
		}

		ItemStack offhand = (ItemStack) state.get("offhand");
		if (offhand != null) {
			inv.setItemInOffHand(offhand);
		}

		for (PotionEffect effect : new ArrayList<>(p.getActivePotionEffects())) {
			p.removePotionEffect(effect.getType());
		}

		@SuppressWarnings("unchecked")
		List<PotionEffect> potionEffects = (List<PotionEffect>) state.get("potionEffects");
		if (potionEffects != null) {
			p.addPotionEffects(potionEffects);
		}

		double maxHealth = (double) state.get("maxHealth");
		p.getAttribute(Attribute.MAX_HEALTH).setBaseValue(maxHealth);

		double health = (double) state.get("health");
		p.setHealth(Math.min(health, maxHealth));

		p.setFoodLevel((int) state.get("foodLevel"));
		p.setSaturation((float) state.get("saturation"));
		p.setExp((float) state.get("exp"));
		p.setLevel((int) state.get("level"));
		p.setAllowFlight((boolean) state.get("allowFlight"));
		p.setFlying((boolean) state.get("flying"));
	}
}
