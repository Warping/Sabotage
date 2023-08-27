package bubbles.sabotage.plugin.items;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import bubbles.sabotage.plugin.items.customitem.CustomItem;

public class Trash extends CustomItem {
	
	public Trash() {
		super();
		
		//Set variable item by changing Material.BLAZE_ROD to some other Material
		
		ItemStack item = new ItemStack(Material.PUFFERFISH_BUCKET);
		ItemMeta im = item.getItemMeta();
		
		// Change the item meta and item details below
		
		im.setDisplayName(ChatColor.MAGIC + "Garbage");
		
		List<String> lore = new ArrayList<>();
		lore.add("What Danny Davito would want.");
		lore.add("What Danny Davito would want.");
		lore.add("What Danny Davito would want.");
		lore.add("What Danny Davito would want.");
		lore.add("What Danny Davito would want.");
		im.setLore(lore);
		
		
		// End of changes
		
		item.setItemMeta(im);
		setItem(item);
		
	}
	protected void onRightClickAir(PlayerInteractEvent e, boolean mainHand) {
		Player player=e.getPlayer();
		
		Location loc1=player.getLocation();
		Location loc2;
		double distance;
		
		
		for (Player onlinePlayer : this.getPlugin().getServer().getOnlinePlayers()) {
			if (getGame().getTeams().onSameTeam(player, onlinePlayer)) {
				continue;
			}
			loc2 = onlinePlayer.getLocation();
			
			distance = loc1.distance(loc2);
			if (distance < 3) {
				PotionEffect effect = new PotionEffect(PotionEffectType.POISON, 1200, 1, false, false);
				onlinePlayer.addPotionEffect(effect);
					
			}
		}
	}

}
