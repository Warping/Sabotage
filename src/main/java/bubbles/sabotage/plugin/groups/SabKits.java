package bubbles.sabotage.plugin.groups;

import bubbles.sabotage.plugin.GUI;
import bubbles.sabotage.plugin.Kit;
import bubbles.sabotage.plugin.Main;
import net.md_5.bungee.api.ChatColor;

import java.util.ArrayList;

public class SabKits {

	private Main plugin;
	private ArrayList<Kit> kits;
	private GUI kitGUI;

	public SabKits(Main _plugin) {
		this.plugin = _plugin;
		this.kitGUI = new GUI(plugin, ChatColor.DARK_PURPLE + "Kits");
		this.kits = new ArrayList<Kit>();
	}

	public void addKit(Kit kit) {
		kitGUI.addSlot(kit.getIcon(), "/kit " + kit.getName());
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
