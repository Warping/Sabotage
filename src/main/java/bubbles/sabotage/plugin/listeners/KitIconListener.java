package bubbles.sabotage.plugin.listeners;

import bubbles.sabotage.plugin.KitIcon;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.PlayerInventory;

/**
 * Keeps the kit icon in the rightmost hotbar slot fixed: it cannot be moved, swapped, dragged,
 * dropped, used, or worn down while it is there.
 */
public class KitIconListener implements Listener {

	@EventHandler
	public void onClick(InventoryClickEvent e) {
		if (!(e.getWhoClicked() instanceof Player)) {
			return;
		}
		Player p = (Player) e.getWhoClicked();
		if (!KitIcon.isLocked(p)) {
			return;
		}
		PlayerInventory inv = p.getInventory();
		boolean clickedIcon = inv.equals(e.getClickedInventory()) && e.getSlot() == KitIcon.SLOT;
		boolean numberKeySwap = e.getHotbarButton() == KitIcon.SLOT;
		if (clickedIcon || numberKeySwap) {
			e.setCancelled(true);
		}
	}

	@EventHandler
	public void onDrag(InventoryDragEvent e) {
		if (!(e.getWhoClicked() instanceof Player)) {
			return;
		}
		Player p = (Player) e.getWhoClicked();
		if (!KitIcon.isLocked(p)) {
			return;
		}
		for (int raw : e.getRawSlots()) {
			if (e.getView().getInventory(raw) instanceof PlayerInventory && e.getView().convertSlot(raw) == KitIcon.SLOT) {
				e.setCancelled(true);
				return;
			}
		}
	}

	@EventHandler
	public void onSwapHands(PlayerSwapHandItemsEvent e) {
		if (KitIcon.isKitIcon(e.getMainHandItem()) || KitIcon.isKitIcon(e.getOffHandItem())) {
			e.setCancelled(true);
		}
	}

	@EventHandler
	public void onInteract(PlayerInteractEvent e) {
		if (KitIcon.isKitIcon(e.getItem())) {
			e.setCancelled(true);
		}
	}

	@EventHandler
	public void onInteractEntity(PlayerInteractEntityEvent e) {
		if (KitIcon.isKitIcon(e.getPlayer().getInventory().getItemInMainHand())) {
			e.setCancelled(true);
		}
	}

	@EventHandler
	public void onInteractAtEntity(PlayerInteractAtEntityEvent e) {
		if (KitIcon.isKitIcon(e.getPlayer().getInventory().getItemInMainHand())) {
			e.setCancelled(true);
		}
	}

	@EventHandler
	public void onConsume(PlayerItemConsumeEvent e) {
		if (KitIcon.isKitIcon(e.getItem())) {
			e.setCancelled(true);
		}
	}

	@EventHandler
	public void onItemDamage(PlayerItemDamageEvent e) {
		if (KitIcon.isKitIcon(e.getItem())) {
			e.setCancelled(true);
		}
	}

	@EventHandler
	public void onDeath(PlayerDeathEvent e) {
		e.getDrops().removeIf(KitIcon::isKitIcon);
	}
}
