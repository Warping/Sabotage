package bubbles.sabotage.plugin.items.customitem;


import bubbles.sabotage.plugin.Commands;
import bubbles.sabotage.plugin.Main;
import bubbles.sabotage.plugin.game.Game;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.logging.Level;

import static bubbles.sabotage.plugin.Main.applog;

public abstract class CustomItem implements Listener {

	private Level LOG_LEVEL = Level.INFO;
	private ItemStack customItem;
	private Main plugin = Main.getPlugin(Main.class);
	// Tag written into every custom item's PersistentDataContainer so the plugin can
	// still recognize it after its ItemMeta (enchants, lore, name, etc.) is modified in-place.
	private final NamespacedKey idKey = new NamespacedKey(plugin, "custom_item_id");

	public CustomItem() {
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Unique identifier for this custom item type. Defaults to the concrete class name, which is
	 * stable and distinct per CustomItem subclass. Override if multiple instances of the same
	 * subclass need to be told apart (not currently needed by any item).
	 */
	protected String getItemId() {
		return getClass().getName();
	}

	/**
	 * Checks whether the given ItemStack is (a copy/instance of) this custom item, based on the
	 * persistent tag stamped by {@link #setItem(ItemStack)} rather than full ItemStack equality.
	 * This means the actual item in a player's inventory can have its enchants/lore/name/etc.
	 * freely modified (e.g. to add a Sharpness level on kill) without the plugin losing track of
	 * what it is.
	 */
	public boolean isCustomItem(ItemStack item) {
		if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
			return false;
		}
		String id = item.getItemMeta().getPersistentDataContainer().get(idKey, PersistentDataType.STRING);
		return getItemId().equals(id);
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
		if (isCustomItem(item)) {
			// Tag-based removal: individual copies may have diverging ItemMeta (enchants, lore,
			// etc.), so Bukkit's meta-sensitive removeItem()/containsAtLeast() can't be trusted.
			consumeTaggedItem(player, count);
			return;
		}
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

	private void consumeTaggedItem(Player player, int count) {
		int remaining = count;
		ItemStack[] contents = player.getInventory().getContents();
		for (int i = 0; i < contents.length && remaining > 0; i++) {
			ItemStack stack = contents[i];
			if (!isCustomItem(stack)) {
				continue;
			}
			int take = Math.min(remaining, stack.getAmount());
			if (stack.getAmount() - take <= 0) {
				player.getInventory().setItem(i, null);
			} else {
				stack.setAmount(stack.getAmount() - take);
			}
			remaining -= take;
		}
		if (remaining > 0) {
			ItemStack offHand = player.getInventory().getItemInOffHand();
			if (isCustomItem(offHand)) {
				int take = Math.min(remaining, offHand.getAmount());
				if (offHand.getAmount() - take <= 0) {
					player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
				} else {
					offHand.setAmount(offHand.getAmount() - take);
				}
				remaining -= take;
			}
		}
		if (remaining > 0) {
			applog.log(LOG_LEVEL, "Cant Remove Item from");
		}
	}

	public void consumeAll(Player player, ItemStack item) {
		player.getInventory().remove(item.getType());
	}

	public boolean contains(Player player, ItemStack item, int count) {
		if (isCustomItem(item)) {
			return countTaggedItems(player) >= count;
		}
		if (player.getInventory().containsAtLeast(item, count)) {
			return true;
		} else if (player.getInventory().getItemInOffHand().getType().equals(item.getType())
				&& player.getInventory().getItemInOffHand().getAmount() >= count) {
			return true;
		} else {
			return false;
		}
	}

	private int countTaggedItems(Player player) {
		int total = 0;
		for (ItemStack stack : player.getInventory().getContents()) {
			if (isCustomItem(stack)) {
				total += stack.getAmount();
			}
		}
		ItemStack offHand = player.getInventory().getItemInOffHand();
		if (isCustomItem(offHand)) {
			total += offHand.getAmount();
		}
		return total;
	}
	
	public void setItem(ItemStack item) {
		ItemStack stamped = item.clone();
		stamped.setAmount(1);
		ItemMeta meta = stamped.getItemMeta();
		meta.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, getItemId());
		stamped.setItemMeta(meta);
		customItem = stamped;
	}
	
	public ItemStack getItem() {
		return customItem;
	}

	@EventHandler
	public void onClickEvent(PlayerInteractEvent e) {
		if (e.getItem()==null) {
			return;
		}
		if (isCustomItem(e.getItem())) {
			String who = e.getPlayer().getName();
			String tag = "[DEBUG] " + getClass().getSimpleName() + " used by " + who + ": ";
			switch (e.getAction()) {
			case LEFT_CLICK_AIR:
				if (Commands.isDebugMode()) applog.log(LOG_LEVEL, tag + "Left Click Air!");
				onLeftClickAir(e);
				break;
			case LEFT_CLICK_BLOCK:
				if (Commands.isDebugMode()) applog.log(LOG_LEVEL, tag + "Left Click Block!");
				onLeftClickBlock(e);
				break;
			case RIGHT_CLICK_AIR:
				if (e.getHand()==EquipmentSlot.HAND) {
					if (Commands.isDebugMode()) applog.log(LOG_LEVEL, tag + "Right Click Air! MainHand");
					onRightClickAir(e, true);
				} else if (e.getHand()==EquipmentSlot.OFF_HAND) {
					if (Commands.isDebugMode()) applog.log(LOG_LEVEL, tag + "Right Click Air! OffHand");
					onRightClickAir(e, false);
				}
				break;
			case RIGHT_CLICK_BLOCK:
				if (e.getHand()==EquipmentSlot.HAND) {
					if (Commands.isDebugMode()) applog.log(LOG_LEVEL, tag + "Right Click Block! MainHand");
					onRightClickBlock(e, true);
				} else if (e.getHand()==EquipmentSlot.OFF_HAND) {
					if (Commands.isDebugMode()) applog.log(LOG_LEVEL, tag + "Right Click Block! OffHand");
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
			} else if (e.getDamager() instanceof Arrow) {
				if (((Arrow) e.getDamager()).getShooter() instanceof Player) {
					attacker = (Player) ((Arrow) e.getDamager()).getShooter();
				}
			} else {
				return;
			}
			if (attacker != null) {
				if (isCustomItem(attacker.getInventory().getItemInMainHand())) {
					if (Commands.isDebugMode()) applog.log(LOG_LEVEL,"Attacked! MainHand");
					onAttack(e, true);
				} else if (isCustomItem(attacker.getInventory().getItemInOffHand())) {
					if (Commands.isDebugMode()) applog.log(LOG_LEVEL,"Attacked! OffHand");
					onAttack(e, false);	
				}
			}
		}
	}

	@EventHandler
	public void onProjectileHit(ProjectileHitEvent e) {
		if (e.getEntity().getShooter() instanceof Player) {
			Player shooter = (Player) e.getEntity().getShooter();
			ItemStack eventItem1 = shooter.getInventory().getItemInMainHand();
			ItemStack eventItem2 = shooter.getInventory().getItemInOffHand();
			if (isCustomItem(eventItem1)) {
				if (e.getHitBlock()!=null) {
					if (Commands.isDebugMode()) applog.log(LOG_LEVEL,"Hit Block! MainHand");
					onShotBlock(e,true);
				} else if (e.getHitEntity() instanceof Player) {
					if (Commands.isDebugMode()) applog.log(LOG_LEVEL,"Hit Player! MainHand");
					onShotPlayer(e,true);
				}
			} else if (isCustomItem(eventItem2)) {
				if (e.getHitBlock()!=null) {
					if (Commands.isDebugMode()) applog.log(LOG_LEVEL,"Hit Block! OffHand");
					onShotBlock(e,false);
				} else if (e.getHitEntity() instanceof Player) {
					if (Commands.isDebugMode()) applog.log(LOG_LEVEL,"Hit Player! OffHand");
					onShotPlayer(e,false);
				}
			}
		}
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractAtEntityEvent e) {
		if (e.getRightClicked() instanceof Player) {
			Player attacker = e.getPlayer();
			ItemStack eventItem1 = attacker.getInventory().getItemInMainHand();
			ItemStack eventItem2 = attacker.getInventory().getItemInOffHand();
			if (e.getHand()==EquipmentSlot.OFF_HAND && isCustomItem(eventItem2)) {
				if (Commands.isDebugMode()) applog.log(LOG_LEVEL,"Right Click Player! OffHand");
				onRightClickPlayer(e,false);
			} else if (e.getHand()==EquipmentSlot.HAND && isCustomItem(eventItem1)){
				if (Commands.isDebugMode()) applog.log(LOG_LEVEL,"Right Click Player! MainHand");
				onRightClickPlayer(e,true);
			}
		}	
	}
	
	@EventHandler
	public void onShootE(EntityShootBowEvent e) {
		if (e.getEntity() instanceof Player) {
			ItemStack eventItem1 = ((Player) e.getEntity()).getInventory().getItemInMainHand();
			ItemStack eventItem2 = ((Player) e.getEntity()).getInventory().getItemInOffHand();
			if (isCustomItem(eventItem1) && e.getHand()==EquipmentSlot.HAND) {
				if (Commands.isDebugMode()) applog.log(LOG_LEVEL,"Shot Bow! MainHand");
				onShoot(e,true);
			} else if (isCustomItem(eventItem2) && e.getHand()==EquipmentSlot.OFF_HAND) {
				if (Commands.isDebugMode()) applog.log(LOG_LEVEL,"Shot Bow! OffHand");
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
		if (countTaggedItems(attacker) > 0) {
			if (Commands.isDebugMode()) applog.log(LOG_LEVEL,"Kill Event! " + attacker.getName() + " killed " + victim.getName() + " with " + customItem.getType().name() + ".");
			onKill(e, attacker, victim);
		}
		if (countTaggedItems(victim) > 0) {
			if (Commands.isDebugMode()) applog.log(LOG_LEVEL,"Death Event!");
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
