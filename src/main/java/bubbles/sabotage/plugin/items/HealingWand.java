package bubbles.sabotage.plugin.items;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import bubbles.sabotage.plugin.items.customitem.CustomItem;

public class HealingWand extends CustomItem {
	
	final double healBy = 0.5;

	public HealingWand() {
		super();
		
		//Set variable item by changing Material.BLAZE_ROD to some other Material
		
		ItemStack item = new ItemStack(Material.BLAZE_ROD);
		ItemMeta im = item.getItemMeta();
		
		// Change the item meta and item details below
		
		im.setDisplayName(ChatColor.LIGHT_PURPLE + "Healing Wand");
		
		List<String> lore = new ArrayList<>();
		lore.add(ChatColor.GOLD + "Heals teammates via right click");
		im.setLore(lore);
		
		
		// End of changes
		
		item.setItemMeta(im);
		setItem(item);
		
	}
	
	private void heal(Player attacker, Player victim) {
		if (getGame().getTeams().onSameTeam(attacker, victim)) {
			double health = victim.getHealth();
			health += healBy;
			if (health >= 20.0) {
				attacker.sendMessage(ChatColor.GREEN + "Fully Healed : " + ChatColor.WHITE + victim.getName());
			} else {
				victim.setHealth(health);
				attacker.sendMessage(ChatColor.GREEN + "Healing " + ChatColor.WHITE + victim.getName() + ChatColor.GREEN + " : " + ChatColor.RED + victim.getHealth());
			}
		} else {
			attacker.sendMessage(ChatColor.RED + "Can't heal enemies!");
		}
	}
	
	@Override
	protected void onRightClickPlayer(PlayerInteractAtEntityEvent e, boolean mainHand) {
		Player player = e.getPlayer();
		Player victim = (Player) e.getRightClicked();
		heal(player, victim);
		
	}

	@Override
	protected void onDeath(PlayerDeathEvent e) {
		
	}

}
