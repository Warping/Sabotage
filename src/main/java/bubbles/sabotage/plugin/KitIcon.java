package bubbles.sabotage.plugin;

import static bubbles.sabotage.plugin.Main.applog;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.logging.Level;

/**
 * The selected kit's icon, pinned to the rightmost hotbar slot. The icon is tagged in its
 * PersistentDataContainer so it can be recognized (and locked) regardless of other changes.
 */
public final class KitIcon {

	public static final int SLOT = Kit.HOTBAR_SIZE - 1;
	private static final NamespacedKey KEY = new NamespacedKey(JavaPlugin.getPlugin(Main.class), "kit_icon");

	private KitIcon() {
	}

	public static boolean isKitIcon(ItemStack item) {
		if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
			return false;
		}
		@NonNull NamespacedKey key = KEY;
		if (key == null) {
			applog.log(Level.WARNING, "[DEBUG] isKitIcon: KEY is null");
			return false;
		}
		@NonNull PersistentDataType<Byte, Byte> byteType = PersistentDataType.BYTE;
		if (byteType == null) {
			applog.log(Level.WARNING, "[DEBUG] isKitIcon: byteType is null");
			return false;
		}
		return item.getItemMeta().getPersistentDataContainer().has(key, byteType);
	}

	public static boolean isLocked(Player p) {
		return isKitIcon(p.getInventory().getItem(SLOT));
	}

	/**
	 * Puts the kit's icon in the rightmost hotbar slot, replacing any existing icon. A kit item
	 * already occupying that slot is moved into the player's inventory rather than lost.
	 */
	public static void place(Player p, Kit kit) {
		if (kit == null) {
			return;
		}
		@NonNull NamespacedKey key = KEY;
		if (key == null) {
			applog.log(Level.WARNING, "[DEBUG] place: KEY is null");
			return;
		}
		@NonNull PersistentDataType<Byte, Byte> byteType = PersistentDataType.BYTE;
		if (byteType == null) {
			applog.log(Level.WARNING, "[DEBUG] place: byteType is null");
			return;
		}
		PlayerInventory inv = p.getInventory();
		ItemStack displaced = inv.getItem(SLOT);
		inv.setItem(SLOT, null);
		if (displaced != null && displaced.getType() != Material.AIR && !isKitIcon(displaced)) {
			inv.addItem(displaced).values().forEach(leftover ->
					p.getWorld().dropItemNaturally(p.getLocation(), leftover));
		}
		ItemStack icon = kit.getIcon();
		icon.setAmount(1);
		ItemMeta meta = icon.getItemMeta();
		meta.getPersistentDataContainer().set(key, byteType, (byte) 1);
		icon.setItemMeta(meta);
		inv.setItem(SLOT, icon);
		p.updateInventory();
	}
}
