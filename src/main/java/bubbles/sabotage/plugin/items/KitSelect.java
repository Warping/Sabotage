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
import bubbles.sabotage.plugin.util.Text;

public class KitSelect extends CustomItem implements Listener {
	
	public KitSelect() {
		super();
		
		//Set variable item by changing Material.BLAZE_ROD to some other Material
		
		ItemStack item = new ItemStack(Material.FEATHER);
		ItemMeta im = item.getItemMeta();
		
		// Change the item meta and item details below
		
		im.displayName(Text.of(ChatColor.GREEN + "Kit Selector"));
		
		// End of changes
		
		item.setItemMeta(im);
		setItem(item);
		
	}
	
	@Override
	protected void onRightClickAir(PlayerInteractEvent e, boolean mainHand) {
		getGUI().open(e.getPlayer());
	}
	
	@Override
	protected void onRightClickBlock(PlayerInteractEvent e, boolean mainHand) {
		getGUI().open(e.getPlayer());
	}
	
	@Override
	protected void onRightClickPlayer(PlayerInteractAtEntityEvent e, boolean mainHand) {
		getGUI().open(e.getPlayer());
	}
	
	@EventHandler
	public void onInventoryClick(InventoryClickEvent e) {
		if (e.getCurrentItem()!=null) {
			GUI gui = getGUI();
			if (e.getInventory().equals(gui.getInv())) {
				e.setCancelled(true);
				gui.execute((Player)e.getWhoClicked(), e.getCurrentItem());
				gui.closeInventory(e.getWhoClicked());
			}
		}
	}

	// Always resolved fresh rather than cached: Game.setKits() swaps in a new SabKits (and
	// therefore a new backing GUI) once kits finish loading, which happens after custom items
	// are constructed, so a field captured at construction time would be stale.
	public GUI getGUI() {
		return getGame().getKits().getKitGUI();
	}

}
