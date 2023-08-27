package bubbles.sabotage.plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import bubbles.sabotage.plugin.game.Game;

@SerializableAs("Kit")
public class Kit implements ConfigurationSerializable {
	
	private static Game game = Main.getPlugin(Main.class).getGame();
	
	private String name;
	private ItemStack icon;
	private ItemStack[] items;
	private Collection<PotionEffect> potionEffects;
	private int price;
	
	public Kit(String name, ItemStack icon, int price, ItemStack[] items, Collection<PotionEffect> potionEffects) {
		this.items = items;
		this.potionEffects = potionEffects;
		this.name = name.trim().toLowerCase();
		icon.setAmount(1);
		this.icon = icon;
		this.price = price;
	}
	
	public Kit(String name, int price, Player p) {
		this.items = p.getInventory().getContents();
		this.potionEffects = p.getActivePotionEffects();
		this.name = name.trim().toLowerCase();
		this.icon = p.getEquipment().getItemInMainHand();
		this.price = price;
	}
	
	@SuppressWarnings("unchecked")
	public Kit(Map<String, Object> config) {
		ArrayList<ItemStack> items = (ArrayList<ItemStack>) config.get("items");
		Collection<PotionEffect> effects = (Collection<PotionEffect>) config.get("potionEffects");
		this.items = items.toArray(new ItemStack[items.size()]);
		this.potionEffects = effects;
		this.name = (String) config.get("name");
		this.icon = (ItemStack) config.get("icon");
		this.price = (int) config.get("price");
		
	}
	
	@Override
	public Map<String, Object> serialize() {
		Map<String, Object> map = new HashMap<>();
		ArrayList<ItemStack> _items = new ArrayList<ItemStack>(Arrays.asList(items));
		map.put("name", name);
		map.put("price", price);
		map.put("icon", icon);
		map.put("items", _items);
		map.put("potionEffects", potionEffects);
		return map;
	}
	
	public static boolean load(Player p, Kit k) {
		if (k==null) {
			return false;
		}
		game.clear(p);
		p.getInventory().setContents(k.items);
		p.updateInventory();
		p.addPotionEffects(k.potionEffects);
		return true;
	}
	
	public String getDisplayName() {
		String second = name.substring(1, name.length());
		String first = name.substring(0, 1).toUpperCase();
		String _name = first + second;
		return ChatColor.GREEN + _name;
	}

	public String getName() {
		return name;
	}

	public void setIcon(ItemStack icon) {
		this.icon = icon;
	}

	public ItemStack getIcon() {
		return icon;
	}

	public ItemStack[] getItems() {
		return items;
	}

	public Collection<PotionEffect> getPotionEffects() {
		return potionEffects;
	}

	public int getPrice() {
		return price;
	}
	
	@Override
	public String toString() {
		return name;
	}
	

}
