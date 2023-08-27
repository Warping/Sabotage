package bubbles.sabotage.plugin.groups;

import bubbles.sabotage.plugin.GUI;
import bubbles.sabotage.plugin.Kit;
import bubbles.sabotage.plugin.Main;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SerializableAs("SabKits")
public class SabKits implements ConfigurationSerializable {
	
	private Main plugin;
	private ArrayList<Kit> kits;
	private GUI kitGUI;
	
	public SabKits(Main _plugin, ArrayList<Kit> kits) {
		this.plugin = _plugin;
		this.kits = kits;
		ItemStack[] items = new ItemStack[kits.size()];
		String[] cmds = new String[kits.size()];
		for (int i = 0; i < kits.size(); i++) {
			Kit kit = kits.get(i);
			cmds[i] = "/kit " + kit.getName();
			ItemStack is = kit.getIcon();
			ItemMeta im = is.getItemMeta();
			im.setDisplayName(ChatColor.LIGHT_PURPLE + "Kit " + kit.getDisplayName());
			is.setItemMeta(im);
			items[i] = is;
			kit.setIcon(is);
		}
		this.kitGUI = new GUI(plugin, ChatColor.DARK_PURPLE + "Kits", items, cmds);
	}
	
	@SuppressWarnings("unchecked")
	public SabKits(Map<String, Object> map) {
		this.kits = (ArrayList<Kit>) map.get("kits");
		this.plugin = Main.getPlugin(Main.class);
		ItemStack[] items = new ItemStack[kits.size()];
		String[] cmds = new String[kits.size()];
		for (int i = 0; i < kits.size(); i++) {
			Kit kit = kits.get(i);
			cmds[i] = "/kit " + kit.getName();
			ItemStack is = kit.getIcon();
			ItemMeta im = is.getItemMeta();
			im.setDisplayName(ChatColor.LIGHT_PURPLE + "Kit " + kit.getDisplayName());
			List<String> lore = new ArrayList<String>();
			lore.add(ChatColor.GOLD + "Cost: " + ChatColor.GREEN + kit.getPrice());
			im.setLore(lore);
			is.setAmount(1);
			is.setItemMeta(im);
			kit.setIcon(is);
			items[i] = kit.getIcon();
		}
		this.kitGUI = new GUI(plugin, ChatColor.DARK_PURPLE + "Kits", items, cmds);
	}
	
	@Override
	public Map<String, Object> serialize() {
		Map<String, Object> map = new HashMap<>();
		map.put("kits", kits);
		return map;
	}
	
	public SabKits(Main _plugin) {
		this.plugin = _plugin;
		this.kitGUI = new GUI(plugin, ChatColor.DARK_PURPLE + "Kits");
		this.kits = new ArrayList<Kit>();
	}
	
	public void addKit(Kit kit) {
		ItemStack is = kit.getIcon();
		ItemMeta im = is.getItemMeta();
		im.setDisplayName(ChatColor.LIGHT_PURPLE + "Kit " + kit.getDisplayName());
		List<String> lore = new ArrayList<String>();
		lore.add(ChatColor.GOLD + "Cost: " + ChatColor.GREEN + kit.getPrice());
		im.setLore(lore);
		ItemStack _is = new ItemStack(is);
		_is.setItemMeta(im);
		_is.setAmount(1);
		kitGUI.addSlot(_is, "/kit " + kit.getName());
		kit.setIcon(_is);
		kits.add(kit);
	}
	
	public Kit getKit(String name) {
		name = name.trim().toLowerCase();
		for (Kit kit : kits) {
			if (kit.getName().equals(name)) {
				return kit;
			}
		}
		return null;
	}
	
	public ArrayList<Kit> getKits() {
		return kits;
	}
	
	public GUI getKitGUI() {
		return kitGUI;
	}

	public void removeKit(Kit kit) {
		kits.remove(kit);
		kitGUI.removeSlot(kit.getIcon());
	}
	
	@Override
	public String toString() {
		String kitList = "";
		if (kits.size()==0) {
			return "No Kits!";
		} else if (kits.size()==1) {
			return kits.get(0).getDisplayName();
		} else if (kits.size()==2) {
			return kits.get(0).getDisplayName() + " and " + kits.get(1).getDisplayName();
		}
		for (int i = 0; i < kits.size() - 1; i++) {
			kitList += kits.get(i).getDisplayName() + ", ";
		}
		kitList += "and " + kits.get(kits.size() - 1).getDisplayName();
		return kitList;
	}
}
