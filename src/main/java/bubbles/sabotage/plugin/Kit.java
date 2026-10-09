package bubbles.sabotage.plugin;

import bubbles.sabotage.plugin.game.Game;
import bubbles.sabotage.plugin.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A selectable loadout. A kit is just data: a hotbar (slot 0 is always the Blaze Powder
 * defuse kit, implicit on every kit), four armor pieces, an offhand item, and any starting
 * potion effects. Each kit is backed by its own human-editable YAML file; see
 * {@link bubbles.sabotage.plugin.fileIO.KitIO}.
 */
public class Kit {

	public static final int HOTBAR_SIZE = 9;
	public static final int ARMOR_SIZE = 4; // Order: boots, leggings, chestplate, helmet

	private static Game game = Main.getPlugin(Main.class).getGame();

	private final String name;
	private final int price;
	private final ItemStack iconItem;
	private final List<String> description;
	private final ItemStack[] hotbar; // size HOTBAR_SIZE, index 0 is always the defuse kit
	private final ItemStack[] armor; // size ARMOR_SIZE: boots, leggings, chestplate, helmet
	private final ItemStack offhand;
	private final Collection<PotionEffect> potionEffects;

	public Kit(String name, int price, ItemStack iconItem, List<String> description, ItemStack[] hotbar,
			ItemStack[] armor, ItemStack offhand, Collection<PotionEffect> potionEffects) {
		this.name = name.trim().toLowerCase();
		this.price = price;
		this.iconItem = iconItem != null ? iconItem : new ItemStack(Material.STONE);
		this.description = description != null ? description : new ArrayList<>();
		this.hotbar = hotbar != null ? hotbar : new ItemStack[HOTBAR_SIZE];
		this.armor = armor != null ? armor : new ItemStack[ARMOR_SIZE];
		this.offhand = offhand;
		this.potionEffects = potionEffects != null ? potionEffects : new ArrayList<>();
	}

	/**
	 * Builds a kit from a player's current hotbar, armor, and offhand (ignoring their main
	 * inventory storage and slot 0, which is always the defuse kit). Used by
	 * {@code /sab kit add} to snapshot a loadout; the resulting file can be hand-edited
	 * afterward to set its description.
	 */
	public static Kit fromPlayer(String name, int price, Player p) {
		PlayerInventory inv = p.getInventory();
		ItemStack[] hotbar = new ItemStack[HOTBAR_SIZE];
		hotbar[0] = new ItemStack(Material.BLAZE_POWDER);
		for (int i = 1; i < HOTBAR_SIZE; i++) {
			ItemStack item = inv.getItem(i);
			hotbar[i] = item != null && item.getType() != Material.AIR ? item.clone() : null;
		}
		ItemStack[] armorSrc = inv.getArmorContents();
		ItemStack[] armor = new ItemStack[ARMOR_SIZE];
		for (int i = 0; i < ARMOR_SIZE && i < armorSrc.length; i++) {
			armor[i] = armorSrc[i] != null && armorSrc[i].getType() != Material.AIR ? armorSrc[i].clone() : null;
		}
		ItemStack offhand = inv.getItemInOffHand();
		if (offhand == null || offhand.getType() == Material.AIR) {
			offhand = null;
		} else {
			offhand = offhand.clone();
		}
		Material icon = hotbar[1] != null ? hotbar[1].getType() : Material.STONE;
		ItemStack iconItem = hotbar[1] != null ? hotbar[1].clone() : new ItemStack(Material.STONE);
		List<String> description = new ArrayList<>();
		description.add("No description set yet.");
		return new Kit(name, price, iconItem, description, hotbar, armor, offhand, p.getActivePotionEffects());
	}

	public static boolean load(Player p, Kit k) {
		if (k == null) {
			return false;
		}
		game.clear(p);
		PlayerInventory inv = p.getInventory();
		for (int i = 0; i < HOTBAR_SIZE; i++) {
			inv.setItem(i, k.hotbar[i]);
		}
		inv.setArmorContents(k.armor);
		inv.setItemInOffHand(k.offhand != null ? k.offhand : new ItemStack(Material.AIR));
		p.updateInventory();
		p.addPotionEffects(k.potionEffects);
		return true;
	}

	public String getCapitalizedName() {
		return name.substring(0, 1).toUpperCase() + name.substring(1);
	}

	public String getDisplayName() {
		return ChatColor.GREEN + getCapitalizedName();
	}

	/**
	 * Builds the icon shown in the kit-selection GUI: a bold kit name, cost in green, and
	 * the kit's short description as colored lore. Deterministically rebuilt from the kit's
	 * own fields each time, so repeated calls produce value-equal ItemStacks.
	 */
	public ItemStack getIcon() {
		ItemStack icon = iconItem.clone();
		ItemMeta im = icon.getItemMeta();
		if (im == null) {
			im = Bukkit.getItemFactory().getItemMeta(icon.getType());
		}
		im.displayName(Text.of(ChatColor.LIGHT_PURPLE.toString() + ChatColor.BOLD + "Kit " + getCapitalizedName()));
		List<String> lore = new ArrayList<>();
		lore.add(ChatColor.GRAY + "Cost: " + ChatColor.GREEN + price);
		for (String line : description) {
			lore.add(ChatColor.GOLD + line);
		}
		im.lore(Text.of(lore));
		icon.setItemMeta(im);
		return icon;
	}

	public String getName() {
		return name;
	}

	public int getPrice() {
		return price;
	}

	public Material getIconMaterial() {
		return iconItem.getType();
	}

	public ItemStack getIconItem() {
		return iconItem;
	}

	public List<String> getDescription() {
		return description;
	}

	public ItemStack[] getHotbar() {
		return hotbar;
	}

	public ItemStack[] getArmor() {
		return armor;
	}

	public ItemStack getOffhand() {
		return offhand;
	}

	public Collection<PotionEffect> getPotionEffects() {
		return potionEffects;
	}

	@Override
	public String toString() {
		return name;
	}

}
