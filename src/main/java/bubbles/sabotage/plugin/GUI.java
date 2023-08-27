package bubbles.sabotage.plugin;

import bubbles.sabotage.plugin.items.customitem.CustomItem;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;

public class GUI {
	
	private String name; //Name of GUI
	private Inventory inv; //Internal Inventory of GUI
	private ItemStack[] items; //Items in GUI
	private String[] cmds; // Commands in GUI
	//private GUIListener listen;
	private Main plugin;
	
	public GUI(Main plugin, String name, ItemStack[] items, String[] cmds) {
		this.plugin = plugin;
		this.name = name;
		this.items = items;
		this.cmds = cmds;
		for (int i = 0; i < items.length; i++) {
			if (items[i]==null) {
				items[i] = new ItemStack(Material.AIR);
				this.cmds[i] = "";
			} else if (cmds[i].equals(null)) {
				this.cmds[i] = "";
			}
		}
		createInv();
		//listen = new GUIListener(this);
	}
	
	public GUI(Main plugin, String name) {
		this.plugin = plugin;
		this.name = name;
		this.items = new ItemStack[0];
		this.cmds = new String[0];
		createInv();
		//listen = new GUIListener(this);
	}
	
	private void createInv() {
		int size = ((items.length / 9) + 1) * 9;
		inv = plugin.getServer().createInventory(null, size, name);
		inv.setContents(items);
	}
	
	public void addSlot(ItemStack newItem, String newCmd) {
		ArrayList<ItemStack> _items = new ArrayList<ItemStack>(Arrays.asList(items));
		ArrayList<String> _cmds = new ArrayList<String>(Arrays.asList(cmds));
		_items.add(newItem);
		_cmds.add(newCmd);
		items = _items.toArray(new ItemStack[_items.size()]);
		cmds = _cmds.toArray(new String[_cmds.size()]);
		createInv();
	}
	
	public void removeSlot(ItemStack item) {
		ArrayList<ItemStack> _items = new ArrayList<ItemStack>(Arrays.asList(items));
		ArrayList<String> _cmds = new ArrayList<String>(Arrays.asList(cmds));
		_cmds.remove(_items.indexOf(item));
		_items.remove(item);
		items = _items.toArray(new ItemStack[_items.size()]);
		cmds = _cmds.toArray(new String[_cmds.size()]);
		createInv();
	}
	
	public void removeSlot(int index) {
		ArrayList<ItemStack> _items = new ArrayList<ItemStack>(Arrays.asList(items));
		ArrayList<String> _cmds = new ArrayList<String>(Arrays.asList(cmds));
		_items.remove(index);
		_cmds.remove(index);
		items = _items.toArray(new ItemStack[_items.size()]);
		cmds = _cmds.toArray(new String[_cmds.size()]);
		createInv();
	}
	
	public void addSlot() {
		ArrayList<ItemStack> _items = new ArrayList<ItemStack>(Arrays.asList(items));
		ArrayList<String> _cmds = new ArrayList<String>(Arrays.asList(cmds));
		_items.add(new ItemStack(Material.AIR));
		_cmds.add("");
		items = _items.toArray(new ItemStack[_items.size()]);
		cmds = _cmds.toArray(new String[_cmds.size()]);
		createInv();
	}
	
	public String getCmd(ItemStack currentItem) {
		for (int i = 0; i < items.length; i++) {
			if (items[i].equals(currentItem)) {
				return cmds[i];
			}
		}
		return "";
	}

	public Main getPlugin() {
		return plugin;
	}

	public String getName() {
		return name;
	}

	public Inventory getInv() {
		return inv;
	}

	public void open(Player player) {
		player.openInventory(inv);
	}

	public void execute(Player player, ItemStack currentItem) {
		String cmd = getCmd(currentItem);
		if (cmd.startsWith("/")) {
			player.performCommand(cmd.substring(1));
		} else {
			player.chat(cmd);
		}
	}

	public void execute(CustomItem citem, Player player, ItemStack currentItem) {
		String cmd = getCmd(currentItem);
		if (cmd.startsWith("/")) {
			player.performCommand(cmd.substring(1));
		} else {
			player.chat(cmd);
		}
	}

	public void closeInventory(HumanEntity player) {
		player.closeInventory();
	}
	
}
