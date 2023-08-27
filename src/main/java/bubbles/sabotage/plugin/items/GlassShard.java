package bubbles.sabotage.plugin.items;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import bubbles.sabotage.plugin.items.customitem.CustomItem;

public class GlassShard extends CustomItem {
	
	private HashMap<Player, Boolean> activated = new HashMap<Player, Boolean>();
	
	public GlassShard() {
		super();
		getPlugin().getServer().getPluginManager().registerEvents(this, getPlugin());
		
		//Set variable item by changing Material.BLAZE_ROD to some other Material
		
		ItemStack item = new ItemStack(Material.PRISMARINE_SHARD);
		ItemMeta im = item.getItemMeta();
		
		// Change the item meta and item details below
		
		im.setDisplayName(ChatColor.LIGHT_PURPLE + "Glass Shard");
		
		List<String> lore = new ArrayList<>();
		lore.add("Gives the user immense strength at the cost of healing!");
		lore.add("Use this ability wisely!");
		im.setLore(lore);
		
		
		// End of changes
		
		item.setItemMeta(im);
		setItem(item);
		
	}
	
	private void deactivate(Player player) {
		activated.put(player, false);
		for (PotionEffect potion : player.getActivePotionEffects()) {
			if (potion.getType().equals(PotionEffectType.INCREASE_DAMAGE) || potion.getType().equals(PotionEffectType.NIGHT_VISION)) {
				player.removePotionEffect(potion.getType());
			}
		}
	}
	
	private void activate(Player player) {
		activated.put(player, true);
		player.damage(2);
		player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 99999, 1, false, false, true));
		player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 99999, 2, false, false, true));
	}
	
	private void click(Player player) {
		if (activated.get(player)!=null) {
			if (activated.get(player)) {
				deactivate(player);
			} else {
				activate(player);
			}
		}
	}
	
	@EventHandler
	private void onHealing(EntityRegainHealthEvent e) {
		if (e.getEntity() instanceof Player && e.getRegainReason()==RegainReason.SATIATED) {
			if (activated.get(e.getEntity())!=null && activated.get(e.getEntity())) {
				e.setCancelled(true);
			}
		}
	}
	
	@Override
	protected void onRightClickAir(PlayerInteractEvent e, boolean mainHand) {
		click(e.getPlayer());
	}
	
	@Override
	protected void onRightClickBlock(PlayerInteractEvent e, boolean mainHand) {
		click(e.getPlayer());
	}

	@Override
	protected void onDeath(PlayerDeathEvent e) {
		deactivate(e.getEntity());
	}

	public void stop() {
		for (Player player : activated.keySet()) {
			deactivate(player);
		}
		activated.clear();
	}

}
