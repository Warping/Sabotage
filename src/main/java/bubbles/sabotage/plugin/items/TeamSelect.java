package bubbles.sabotage.plugin.items;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import bubbles.sabotage.plugin.GUI;
import bubbles.sabotage.plugin.items.customitem.CustomItem;

public class TeamSelect extends CustomItem implements Listener {
	
	GUI gui = this.getGame().getTeams().getTeamGUI();
	
	public TeamSelect() {
		super();
		
		//Set variable item by changing Material.BLAZE_ROD to some other Material
		
		ItemStack item = new ItemStack(Material.NETHER_STAR);
		ItemMeta im = item.getItemMeta();
		
		// Change the item meta and item details below
		
		im.setDisplayName(ChatColor.GREEN + "Team Selector");
		
		// End of changes
		
		item.setItemMeta(im);
		setItem(item);
		
	}
	
	@Override
	protected void onRightClickAir(PlayerInteractEvent e, boolean mainHand) {
		gui.open(e.getPlayer());
	}
	
	@Override
	protected void onRightClickBlock(PlayerInteractEvent e, boolean mainHand) {
		gui.open(e.getPlayer());
	}
	
	@Override
	protected void onRightClickPlayer(PlayerInteractAtEntityEvent e, boolean mainHand) {
		gui.open(e.getPlayer());
	}
	
	@EventHandler
	public void onInventoryClick(InventoryClickEvent e) {
		if (e.getCurrentItem()!=null) {
			if (e.getInventory().equals(gui.getInv())) {
				e.setCancelled(true);
				gui.execute((Player)e.getWhoClicked(), e.getCurrentItem());
				gui.closeInventory(e.getWhoClicked());
			}
		}
	}
	

}
