package bubbles.sabotage.plugin.items;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import bubbles.sabotage.plugin.items.customitem.CustomItem;

public class Bullet extends CustomItem {
	
	public Bullet() {
		super();
		
		//Set variable item by changing Material.BLAZE_ROD to some other Material
		
		ItemStack item = new ItemStack(Material.IRON_NUGGET);
		ItemMeta im = item.getItemMeta();
		
		// Change the item meta and item details below
		
		im.setDisplayName(ChatColor.YELLOW + "Bullet");
		
		List<String> lore = new ArrayList<>();
		lore.add("Shoots at player instantly!");
		im.setLore(lore);
		
		
		// End of changes
		
		item.setItemMeta(im);
		setItem(item);
		
	}
	
	@Override
	protected void onRightClickAir(PlayerInteractEvent e, boolean mainHand) {
		
	}
	
	@Override
	protected void onRightClickBlock(PlayerInteractEvent e, boolean mainHand) {
		
	}
	
	@Override
	protected void onRightClickPlayer(PlayerInteractAtEntityEvent e, boolean mainHand) {
		
	}
	
	private void shoot() {
		
	}

}
