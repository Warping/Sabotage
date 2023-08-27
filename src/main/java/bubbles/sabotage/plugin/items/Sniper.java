package bubbles.sabotage.plugin.items;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import bubbles.sabotage.plugin.items.customitem.CustomItem;

public class Sniper extends CustomItem implements Listener {
	
	private double damageMult = 0.6;

	public Sniper() {
		
		super();
		
		//Set variable item by changing Material.BLAZE_ROD to some other Material
		
		ItemStack item = new ItemStack(Material.BOW);
		ItemMeta im = item.getItemMeta();
		
		// Change the item meta and item details below
		
		im.setDisplayName(ChatColor.AQUA + "Sniper");
		
		List<String> lore = new ArrayList<>();
		lore.add("Hit players from far to do massive damage!");
		im.setLore(lore);
		
		im.addEnchant(Enchantment.ARROW_INFINITE, 1, false);
		im.setUnbreakable(true);
		
		
		// End of changes
		
		item.setItemMeta(im);
		setItem(item);
		
	}
	
	@Override
	protected void onAttack(EntityDamageByEntityEvent e, boolean mainHand) {
		Arrow arrow = (Arrow) e.getDamager();
		Player shooter = (Player) arrow.getShooter();
		Player victim = (Player) e.getEntity();
		double dist = shooter.getLocation().distance(victim.getLocation());
		if (dist >= 15) {
			shooter.attack(victim);
			e.setDamage(dist * damageMult);
			shooter.sendMessage(ChatColor.GREEN + "Hit " + victim.getName() + " from " + Math.floor(dist) + " blocks away!");
		} else {
			e.setDamage(1);
		}
	}

}
