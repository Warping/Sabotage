package bubbles.sabotage.plugin.fileIO;

import bubbles.sabotage.plugin.Kit;
import bubbles.sabotage.plugin.Main;
import bubbles.sabotage.plugin.groups.SabKits;
import bubbles.sabotage.plugin.items.customitem.CustomItem;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;

import static bubbles.sabotage.plugin.Main.applog;

/**
 * Reads and writes kits as individual human-editable YAML files, one file per kit, under
 * {@code plugins/Sabotage/<game>/kits/}. Default kits bundled with the plugin (in
 * {@code src/main/resources/kits/}) are extracted to that folder the first time they are
 * missing, so server owners can hand-edit them after the plugin has already been compiled.
 */
public final class KitIO {

	private static final String ARMOR_SLOT_NAMES_RESOURCE_PREFIX = "kits/";
	private static final String[] ARMOR_SLOT_NAMES = {"boots", "leggings", "chestplate", "helmet"};

	private KitIO() {}

	public static SabKits loadKits(String gameName) {
		Main plugin = Main.getPlugin(Main.class);
		File dir = kitsDir(gameName);
		if (!dir.exists()) {
			dir.mkdirs();
		}
		extractDefaults(plugin, dir);

		SabKits kits = new SabKits(plugin);
		File[] files = dir.listFiles((d, fileName) -> fileName.endsWith(".yml"));
		if (files == null) {
			return kits;
		}
		Arrays.sort(files, Comparator.comparing(File::getName));
		for (File file : files) {
			try {
				Kit kit = parseKit(file);
				if (kit != null) {
					kits.addKit(kit);
				}
			} catch (Exception e) {
				applog.log(Level.WARNING, "Failed to load kit file " + file.getName() + ": " + e.getMessage());
			}
		}
		return kits;
	}

	private static File kitsDir(String gameName) {
		return new File("plugins/Sabotage/" + gameName + "/kits/");
	}

	/**
	 * Discovers every kit bundled under {@code src/main/resources/kits/} by scanning the
	 * plugin's own jar (or, when running unpacked from an IDE, the classes directory) rather
	 * than relying on a hand-maintained id list - so adding, removing, or renaming a bundled
	 * kit file never requires a matching Java code change.
	 */
	private static List<String> listBundledKitIds(Main plugin) {
		List<String> ids = new ArrayList<>();
		try {
			File source = new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
			if (source.isFile()) {
				try (JarFile jar = new JarFile(source)) {
					Enumeration<JarEntry> entries = jar.entries();
					while (entries.hasMoreElements()) {
						String name = entries.nextElement().getName();
						if (name.startsWith(ARMOR_SLOT_NAMES_RESOURCE_PREFIX) && name.endsWith(".yml")) {
							ids.add(name.substring(ARMOR_SLOT_NAMES_RESOURCE_PREFIX.length(), name.length() - ".yml".length()));
						}
					}
				}
			} else {
				File kitsResourceDir = new File(source, "kits");
				File[] files = kitsResourceDir.listFiles((d, n) -> n.endsWith(".yml"));
				if (files != null) {
					for (File f : files) {
						ids.add(f.getName().substring(0, f.getName().length() - ".yml".length()));
					}
				}
			}
		} catch (IOException | URISyntaxException e) {
			applog.log(Level.WARNING, "Failed to list bundled default kits: " + e.getMessage());
		}
		return ids;
	}

	private static void extractDefaults(Main plugin, File dir) {
		for (String id : listBundledKitIds(plugin)) {
			File target = new File(dir, id + ".yml");
			if (target.exists()) {
				continue;
			}
			try (InputStream in = plugin.getResource("kits/" + id + ".yml")) {
				if (in == null) {
					continue;
				}
				Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
				applog.log(Level.INFO, "Extracted default kit: " + id + ".yml");
			} catch (IOException e) {
				applog.log(Level.WARNING, "Failed to extract default kit " + id + ": " + e.getMessage());
			}
		}
	}

	public static Kit parseKit(File file) {
		YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
		String name = config.getString("name", file.getName().replace(".yml", ""));
		int price = config.getInt("price", 0);
		String iconName = config.getString("icon");
		Material icon = iconName != null ? Material.matchMaterial(iconName) : null;
		List<String> description = config.getStringList("description");

		ItemStack[] hotbar = new ItemStack[Kit.HOTBAR_SIZE];
		hotbar[0] = new ItemStack(Material.BLAZE_POWDER); // Defuse kit, implicit on every kit
		ConfigurationSection hotbarSection = config.getConfigurationSection("hotbar");
		if (hotbarSection != null) {
			for (String key : hotbarSection.getKeys(false)) {
				int slot;
				try {
					slot = Integer.parseInt(key);
				} catch (NumberFormatException e) {
					continue;
				}
				if (slot < 1 || slot >= Kit.HOTBAR_SIZE) {
					continue;
				}
				hotbar[slot] = parseItemSpec(hotbarSection.get(key));
			}
		}

		ItemStack[] armor = new ItemStack[Kit.ARMOR_SIZE];
		ConfigurationSection armorSection = config.getConfigurationSection("armor");
		if (armorSection != null) {
			for (int i = 0; i < ARMOR_SLOT_NAMES.length; i++) {
				if (armorSection.contains(ARMOR_SLOT_NAMES[i])) {
					armor[i] = parseItemSpec(armorSection.get(ARMOR_SLOT_NAMES[i]));
				}
			}
		}

		ItemStack offhand = config.contains("offhand") ? parseItemSpec(config.get("offhand")) : null;

		Collection<PotionEffect> potionEffects = new ArrayList<>();
		for (Map<?, ?> raw : config.getMapList("potionEffects")) {
			PotionEffect effect = parsePotionEffect(raw);
			if (effect != null) {
				potionEffects.add(effect);
			}
		}

		if (icon == null) {
			icon = hotbar[1] != null ? hotbar[1].getType() : Material.STONE;
		}

		return new Kit(name, price, icon, description, hotbar, armor, offhand, potionEffects);
	}

	private static ItemStack parseItemSpec(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof String) {
			Material material = Material.matchMaterial((String) raw);
			return material != null ? new ItemStack(material) : null;
		}
		Map<String, Object> map = asMap(raw);
		if (map == null) {
			return null;
		}

		Object customKey = map.get("custom");
		if (customKey != null) {
			CustomItem customItem = Main.getPlugin(Main.class).getCustomItems().get(customKey.toString());
			if (customItem == null) {
				applog.log(Level.WARNING, "Unknown custom item referenced in kit file: " + customKey);
				return null;
			}
			ItemStack item = customItem.getItem().clone();
			Object amount = map.get("amount");
			if (amount instanceof Number) {
				item.setAmount(((Number) amount).intValue());
			}
			return item;
		}

		Object materialName = map.get("material");
		Material material = materialName != null ? Material.matchMaterial(materialName.toString()) : null;
		if (material == null) {
			return null;
		}
		ItemStack item = new ItemStack(material);
		Object amount = map.get("amount");
		if (amount instanceof Number) {
			item.setAmount(((Number) amount).intValue());
		}

		boolean metaChanged = false;
		ItemMeta meta = item.getItemMeta();

		Map<String, Object> enchantsMap = asMap(map.get("enchants"));
		if (enchantsMap != null) {
			for (Map.Entry<String, Object> entry : enchantsMap.entrySet()) {
				Enchantment enchant = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(entry.getKey().toLowerCase(Locale.ROOT)));
				if (enchant != null && entry.getValue() instanceof Number) {
					meta.addEnchant(enchant, ((Number) entry.getValue()).intValue(), true);
					metaChanged = true;
				}
			}
		}

		if (Boolean.TRUE.equals(map.get("unbreakable"))) {
			meta.setUnbreakable(true);
			metaChanged = true;
		}

		if (metaChanged) {
			item.setItemMeta(meta);
		}
		return item;
	}

	/**
	 * Coerces a parsed YAML node into a plain String-keyed map, whether Bukkit handed it back
	 * as a {@link ConfigurationSection} (the common case for nested mappings) or already as a
	 * {@link Map}. Returns null if raw is neither.
	 */
	@SuppressWarnings("unchecked")
	private static Map<String, Object> asMap(Object raw) {
		if (raw instanceof ConfigurationSection) {
			return ((ConfigurationSection) raw).getValues(false);
		} else if (raw instanceof Map) {
			return (Map<String, Object>) raw;
		}
		return null;
	}

	private static PotionEffect parsePotionEffect(Map<?, ?> raw) {
		Object typeName = raw.get("type");
		if (!(typeName instanceof String)) {
			return null;
		}
		PotionEffectType type = Registry.EFFECT.get(NamespacedKey.minecraft(((String) typeName).toLowerCase(Locale.ROOT)));
		if (type == null) {
			return null;
		}
		int amplifier = raw.get("amplifier") instanceof Number ? ((Number) raw.get("amplifier")).intValue() : 0;
		int duration = raw.get("duration") instanceof Number ? ((Number) raw.get("duration")).intValue() : 200;
		boolean hideParticles = raw.get("hideParticles") instanceof Boolean ? (Boolean) raw.get("hideParticles") : false;
		
		if (duration == -1) {
			duration = Integer.MAX_VALUE;
		}
		
		PotionEffect effect = new PotionEffect(type, Math.max(duration, 1), amplifier, true, !hideParticles);
		return effect;
	}

	public static void saveKit(Kit kit, String gameName) {
		File dir = kitsDir(gameName);
		if (!dir.exists()) {
			dir.mkdirs();
		}
		YamlConfiguration config = new YamlConfiguration();
		config.set("name", kit.getName());
		config.set("price", kit.getPrice());
		config.set("icon", kit.getIconMaterial().name());
		config.set("description", kit.getDescription());

		ItemStack[] hotbar = kit.getHotbar();
		for (int slot = 1; slot < hotbar.length; slot++) {
			if (hotbar[slot] != null) {
				config.set("hotbar." + slot, toItemSpec(hotbar[slot]));
			}
		}

		ItemStack[] armor = kit.getArmor();
		for (int i = 0; i < ARMOR_SLOT_NAMES.length && i < armor.length; i++) {
			if (armor[i] != null) {
				config.set("armor." + ARMOR_SLOT_NAMES[i], toItemSpec(armor[i]));
			}
		}

		if (kit.getOffhand() != null) {
			config.set("offhand", toItemSpec(kit.getOffhand()));
		}

		List<Map<String, Object>> potionEffects = new ArrayList<>();
		for (PotionEffect effect : kit.getPotionEffects()) {
			Map<String, Object> map = new LinkedHashMap<>();
			map.put("type", effect.getType().getKey().getKey());
			map.put("amplifier", effect.getAmplifier());
			int duration = effect.getDuration();
			if (duration == Integer.MAX_VALUE) {
				duration = -1;
			}
			map.put("duration", duration);
			if (!effect.hasParticles()) {
				map.put("hideParticles", true);
			}
			potionEffects.add(map);
		}
		config.set("potionEffects", potionEffects);

		File file = new File(dir, kit.getName() + ".yml");
		try {
			config.save(file);
		} catch (IOException e) {
			applog.log(Level.WARNING, "Failed to save kit " + kit.getName() + ": " + e.getMessage());
		}
	}

	public static void deleteKit(String kitName, String gameName) {
		File file = new File(kitsDir(gameName), kitName + ".yml");
		if (file.exists()) {
			file.delete();
		}
	}

	private static Object toItemSpec(ItemStack item) {
		for (Map.Entry<String, CustomItem> entry : Main.getPlugin(Main.class).getCustomItems().entrySet()) {
			ItemStack candidate = entry.getValue().getItem().clone();
			candidate.setAmount(item.getAmount());
			if (candidate.equals(item)) {
				Map<String, Object> spec = new LinkedHashMap<>();
				spec.put("custom", entry.getKey());
				if (item.getAmount() != 1) {
					spec.put("amount", item.getAmount());
				}
				return spec;
			}
		}

		Map<String, Object> enchantMap = null;
		boolean unbreakable = false;
		if (item.hasItemMeta()) {
			ItemMeta meta = item.getItemMeta();
			if (meta.hasEnchants()) {
				enchantMap = new LinkedHashMap<>();
				for (Map.Entry<Enchantment, Integer> e : meta.getEnchants().entrySet()) {
					enchantMap.put(e.getKey().getKey().getKey().toUpperCase(Locale.ROOT), e.getValue());
				}
			}
			unbreakable = meta.isUnbreakable();
		}

		if (enchantMap == null && !unbreakable && item.getAmount() == 1) {
			return item.getType().name();
		}

		Map<String, Object> spec = new LinkedHashMap<>();
		spec.put("material", item.getType().name());
		if (item.getAmount() != 1) {
			spec.put("amount", item.getAmount());
		}
		if (enchantMap != null && !enchantMap.isEmpty()) {
			spec.put("enchants", enchantMap);
		}
		if (unbreakable) {
			spec.put("unbreakable", true);
		}
		return spec;
	}

}
