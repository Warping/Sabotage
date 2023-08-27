package bubbles.sabotage.plugin.items.customitem;


import bubbles.sabotage.plugin.Main;
import bubbles.sabotage.plugin.game.Game;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.logging.Level;

import static bubbles.sabotage.plugin.Main.applog;

public abstract class CustomItem implements Listener {

	private Level LOG_LEVEL = Level.INFO;
	private ItemStack customItem;
	private Main plugin = Main.getPlugin(Main.class);
	
	public CustomItem() {
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	
	public void give(Player player, int count) {
		for (int i = 0; i < count; i++) {
			player.getInventory().addItem(customItem);
		}
	}

	public void give(Player player, ItemStack item, int count) {
		for (int i = 0; i < count; i++) {
			player.getInventory().addItem(item);
		}
	}

	public void consume(Player player, int count) {
		consume(player, customItem, count);
	}
	
	public void consume(Player player, ItemStack item, int count) {
		ItemStack offHandItem = player.getInventory().getItemInOffHand().clone();
		offHandItem.setAmount(count);
		if (player.getInventory().containsAtLeast(item, count)) {
			for (int i = 0; i < count; i++) {
				player.getInventory().removeItem(item);
			}
		} else if (offHandItem.equals(item)) { //Inventory doesn't check Off Hand Item (wtf?)
			int newValue = player.getInventory().getItemInOffHand().getAmount() - count;
			if (newValue > 0) {
				player.getInventory().getItemInOffHand().setAmount(newValue);
			} else {
				player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
			}
		} else {
			applog.log(LOG_LEVEL, "Cant Remove Item from");
		}
	}

	public void consumeAll(Player player, ItemStack item) {
		player.getInventory().remove(item.getType());
	}

	public boolean contains(Player player, ItemStack item, int count) {
		if (player.getInventory().containsAtLeast(item, count)) {
			return true;
		} else if (player.getInventory().getItemInOffHand().getType().equals(item.getType())
				&& player.getInventory().getItemInOffHand().getAmount() >= count) {
			return true;
		} else {
			return false;
		}
	}
	
	public void setItem(ItemStack item) {
		customItem = item.clone();
		customItem.setAmount(1);
	}
	
	public ItemStack getItem() {
		return customItem;
	}

	@EventHandler
	public void onClickEvent(PlayerInteractEvent e) {
		if (e.getItem()==null) {
			return;
		}
		ItemStack eventItem = e.getItem().clone();
		eventItem.setAmount(1);
		if (eventItem.equals(customItem)) {
			switch (e.getAction()) {
			case LEFT_CLICK_AIR:
				applog.log(LOG_LEVEL,"Left Click Air!");
				onLeftClickAir(e);
				break;
			case LEFT_CLICK_BLOCK:
				applog.log(LOG_LEVEL,"Left Click Block!");
				onLeftClickBlock(e);
				break;
			case RIGHT_CLICK_AIR:
				if (e.getHand()==EquipmentSlot.HAND) {
					applog.log(LOG_LEVEL,"Right Click Air! MainHand");
					onRightClickAir(e, true);
				} else if (e.getHand()==EquipmentSlot.OFF_HAND) {
					applog.log(LOG_LEVEL,"Right Click Air! OffHand");
					onRightClickAir(e, false);
				}
				break;
			case RIGHT_CLICK_BLOCK:
				if (e.getHand()==EquipmentSlot.HAND) {
					applog.log(LOG_LEVEL,"Right Click Block! MainHand");
					onRightClickBlock(e, true);
				} else if (e.getHand()==EquipmentSlot.OFF_HAND) {
					applog.log(LOG_LEVEL,"Right Click Block! OffHand");
					onRightClickBlock(e, false);
				}
				break;
			}
		}
		
	}

	@EventHandler
	public void onAttackE(EntityDamageByEntityEvent e) {
		if (e.getEntity() instanceof Player) {
			Player attacker = null;
			if (e.getDamager() instanceof Player) {
				attacker = (Player) e.getDamager();
			}
			if (e.getDamager() instanceof Arrow) {
				if (((Arrow) e.getDamager()).getShooter() instanceof Player) {
					attacker = (Player) ((Arrow) e.getDamager()).getShooter();
				}
			} else {
				return;
			}
			ItemStack eventItem1 = attacker.getInventory().getItemInMainHand().clone();
			ItemStack eventItem2 = attacker.getInventory().getItemInOffHand().clone();
			eventItem1.setAmount(1);
			eventItem2.setAmount(1);
			if (eventItem1.equals(customItem)) {
				applog.log(LOG_LEVEL,"Attacked! MainHand");
				onAttack(e, true);
			} else if (eventItem2.equals(customItem)) {
				applog.log(LOG_LEVEL,"Attacked! OffHand");
				onAttack(e, false);	
			}
		}
	}

	@EventHandler
	public void onProjectileHit(ProjectileHitEvent e) {
		if (e.getEntity().getShooter() instanceof Player) {
			Player shooter = (Player) e.getEntity().getShooter();
			ItemStack eventItem1 = shooter.getInventory().getItemInMainHand().clone();
			ItemStack eventItem2 = shooter.getInventory().getItemInOffHand().clone();
			eventItem1.setAmount(1);
			eventItem2.setAmount(1);
			if (eventItem1.equals(customItem)) {
				if (e.getHitBlock()!=null) {
					applog.log(LOG_LEVEL,"Hit Block! MainHand");
					onShotBlock(e,true);
				} else if (e.getHitEntity() instanceof Player) {
					applog.log(LOG_LEVEL,"Hit Player! MainHand");
					onShotPlayer(e,true);
				}
			} else if (eventItem2.equals(customItem)) {
				if (e.getHitBlock()!=null) {
					applog.log(LOG_LEVEL,"Hit Block! OffHand");
					onShotBlock(e,false);
				} else if (e.getHitEntity() instanceof Player) {
					applog.log(LOG_LEVEL,"Hit Player! OffHand");
					onShotPlayer(e,false);
				}
			}
		}
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractAtEntityEvent e) {
		if (e.getRightClicked() instanceof Player) {
			Player attacker = e.getPlayer();
			ItemStack eventItem1 = attacker.getInventory().getItemInMainHand().clone();
			ItemStack eventItem2 = attacker.getInventory().getItemInOffHand().clone();
			eventItem1.setAmount(1);
			eventItem1.setAmount(1);
			if (e.getHand()==EquipmentSlot.OFF_HAND && eventItem2.equals(customItem)) {
				applog.log(LOG_LEVEL,"Right Click Player! OffHand");
				onRightClickPlayer(e,false);
			} else if (e.getHand()==EquipmentSlot.HAND && eventItem1.equals(customItem)){
				applog.log(LOG_LEVEL,"Right Click Player! MainHand");
				onRightClickPlayer(e,true);
			}
		}	
	}
	
	@EventHandler
	public void onShootE(EntityShootBowEvent e) {
		if (e.getEntity() instanceof Player) {
			ItemStack eventItem1 = ((Player) e.getEntity()).getInventory().getItemInMainHand().clone();
			ItemStack eventItem2 = ((Player) e.getEntity()).getInventory().getItemInOffHand().clone();
			if (eventItem1.equals(customItem) && e.getHand()==EquipmentSlot.HAND) {
				applog.log(LOG_LEVEL,"Shot Bow! MainHand");
				onShoot(e,true);
			} else if (eventItem2.equals(customItem) && e.getHand()==EquipmentSlot.OFF_HAND) {
				applog.log(LOG_LEVEL,"Shot Bow! OffHand");
				onShoot(e,false);
			}

		}
	}

	@EventHandler
	public void onKillE(PlayerDeathEvent e) {
		Player victim = e.getEntity();
		Player attacker = victim.getKiller();
		if (attacker == null) {
			return;
		}
		if (attacker.getInventory().contains(customItem)) {
			applog.log(LOG_LEVEL,"Kill Event! " + attacker.getName() + " killed " + victim.getName() + " with " + customItem.getType().name() + ".");
			onKill(e, attacker, victim);
		}
		if (victim.getInventory().contains(customItem)) {
			applog.log(LOG_LEVEL,"Death Event!");
			onDeath(e);
		}
	}

	public Game getGame() {
		return plugin.getGame();
	}
	
	public Main getPlugin() {
		return plugin;
	}

	protected void onShoot(EntityShootBowEvent e, boolean mainHand) {}
	
	protected void onRightClickAir(PlayerInteractEvent e, boolean mainHand) {}
	protected void onRightClickBlock(PlayerInteractEvent e, boolean mainHand) {}
	
	protected void onRightClickPlayer(PlayerInteractAtEntityEvent e, boolean mainHand) {}
	
	protected void onLeftClickAir(PlayerInteractEvent e) {}
	protected void onLeftClickBlock(PlayerInteractEvent e) {}
	
	protected void onAttack(EntityDamageByEntityEvent e, boolean mainHand) {}
	
	protected void onShotBlock(ProjectileHitEvent e, boolean mainHand) {}
	protected void onShotPlayer(ProjectileHitEvent e, boolean mainHand) {}
	
	protected void onDeath(PlayerDeathEvent e) {}

	protected void onKill(PlayerDeathEvent e, Player attacker, Player victim) {}

	public void stop() {
	}
}
