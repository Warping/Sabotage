package bubbles.sabotage.plugin.items;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import bubbles.sabotage.plugin.items.customitem.CustomItem;

public class Steak extends CustomItem {

	private static ItemStack item;
	
	public Steak() {
		super();
		
		//Set variable item by changing Material.BLAZE_ROD to some other Material
		
		item = new ItemStack(Material.COOKED_BEEF);
		ItemMeta im = item.getItemMeta();
		
		// Change the item meta and item details below
		
		im.setDisplayName(ChatColor.GREEN + "Steak");
		
		List<String> lore = new ArrayList<>();
		lore.add("Heals 4 hearts!");
		im.setLore(lore);
		
		
		// End of changes
		
		item.setItemMeta(im);
		setItem(item);
		
	}
	
	private void heal(Player p) {
		PotionEffect effect = new PotionEffect(PotionEffectType.HEAL, 1, 1, false, false);
		p.addPotionEffect(effect);
	}
	
	@Override
	protected void onRightClickAir(PlayerInteractEvent e, boolean mainHand) {
		heal(e.getPlayer());
		consume(e.getPlayer(), getItem(), 1);
	}
	
	@Override
	protected void onRightClickBlock(PlayerInteractEvent e, boolean mainHand) {
		heal(e.getPlayer());
		consume(e.getPlayer(), getItem(), 1);
	}
	
	@Override
	protected void onRightClickPlayer(PlayerInteractAtEntityEvent e, boolean mainHand) {
		heal(e.getPlayer());
		consume(e.getPlayer(), getItem(), 1);
	}

	public static ItemStack getStaticItem() {
		return item;
	}

}
