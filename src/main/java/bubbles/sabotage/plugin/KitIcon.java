package bubbles.sabotage.plugin;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * The selected kit's icon, pinned to the rightmost hotbar slot. The icon is tagged in its
 * PersistentDataContainer so it can be recognized (and locked) regardless of other changes.
 */
public final class KitIcon {

	public static final int SLOT = Kit.HOTBAR_SIZE - 1;
	private static final NamespacedKey KEY = new NamespacedKey(Main.getPlugin(Main.class), "kit_icon");

	private KitIcon() {
	}

	public static boolean isKitIcon(ItemStack item) {
		if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
			return false;
		}
		return item.getItemMeta().getPersistentDataContainer().has(KEY, PersistentDataType.BYTE);
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
		meta.getPersistentDataContainer().set(KEY, PersistentDataType.BYTE, (byte) 1);
		icon.setItemMeta(meta);
		inv.setItem(SLOT, icon);
		p.updateInventory();
	}
}
