package bubbles.sabotage.plugin.items;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import bubbles.sabotage.plugin.items.customitem.CustomItem;

public class GrapplingHook extends CustomItem implements Listener {
	
	private HashMap<Player, Boolean> grappled = new HashMap<Player, Boolean>();
	
	public GrapplingHook() {
		super();
		
		//Set variable item by changing Material.BLAZE_ROD to some other Material
		
		ItemStack item = new ItemStack(Material.BOW);
		ItemMeta im = item.getItemMeta();
		
		// Change the item meta and item details below
		
		im.setDisplayName(ChatColor.LIGHT_PURPLE + "Grappling Hook");
		
		List<String> lore = new ArrayList<>();
		lore.add("Grapples objects and players!");
		im.setLore(lore);
		
		im.addEnchant(Enchantment.ARROW_INFINITE, 1, false);
		im.setUnbreakable(true);
		
		
		// End of changes
		
		item.setItemMeta(im);
		setItem(item);
		
	}
	
	@EventHandler
	private void onFallDamage(EntityDamageEvent e) {
		if (e.getCause()==DamageCause.FALL && e.getEntity() instanceof Player) {
			Player player = (Player) e.getEntity();
			if (grappled.get(player)!=null) {
				if (grappled.get(player)==true) {
					e.setCancelled(true);
					grappled.put(player, false);
				}
			}
		}
	}
	
	private void hookHit(Player shooter, Player victim) {
		Vector origin = victim.getLocation().toVector();
		Location sLoc = shooter.getLocation();
		sLoc = sLoc.add(0.0, 2.0, 0);	
		Vector target = sLoc.toVector();
		Vector launch = target.subtract(origin);
		double distance = Math.sqrt(Math.pow(launch.getX(), 2) + Math.pow(launch.getZ(), 2));
		launch.normalize();
		grappled.put(victim, true);
		getPlugin().getServer().getScheduler().scheduleSyncDelayedTask(getPlugin(), new Runnable() {
			
			@Override
			public void run() {
				shooter.sendMessage(ChatColor.GREEN + "You grappled " + victim.getName() + " from " + distance + " blocks away!");
				victim.setVelocity(launch.multiply(0.25).multiply(distance));
			}
		}, 3L);
		getPlugin().getServer().getScheduler().scheduleSyncDelayedTask(getPlugin(), new Runnable() {
			
			@Override
			public void run() {
				if (grappled.get(victim)) {
					grappled.put(victim, false);
				}
			}
		}, 90L);
	}
	
	private void hookHit(Player shooter, Block block) {
		shooter.sendMessage(ChatColor.GREEN + "Grappling...");
		Vector origin = shooter.getLocation().toVector();
		Location bLoc = block.getLocation().add(0.0, 2.0, 0);
		Vector target = bLoc.toVector();
		Vector launch = target.subtract(origin);
		double distance = Math.sqrt(Math.pow(launch.getX(), 2) + Math.pow(launch.getZ(), 2));
		launch.normalize();
		shooter.setVelocity(launch.multiply(0.3).multiply(distance));
		grappled.put(shooter, true);
	}
	
	@Override
	protected void onAttack(EntityDamageByEntityEvent e, boolean mainHand) {
		Arrow arrow = (Arrow) e.getDamager();
		Player shooter = (Player) arrow.getShooter();
		Player victim = (Player) e.getEntity();
		e.setDamage(0);
		hookHit(shooter, victim);
	}
	
	@Override
	protected void onShotBlock(ProjectileHitEvent e, boolean mainHand) {
		Block block = e.getHitBlock();
		Arrow arrow = (Arrow) e.getEntity();
		Player shooter = (Player) arrow.getShooter();
		hookHit(shooter, block);
	}

	@Override
	protected void onDeath(PlayerDeathEvent e) {
		grappled.put(e.getEntity(), false);
	}

	public void stop() {
		grappled.clear();
	}

}
