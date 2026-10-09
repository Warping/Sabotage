package bubbles.sabotage.plugin.listeners;

import bubbles.sabotage.plugin.Commands;
import bubbles.sabotage.plugin.game.Game;
import bubbles.sabotage.plugin.items.customitem.CustomItem;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.TNTPrimeEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.logging.Level;

import static bubbles.sabotage.plugin.Main.applog;

public class GameListener implements Listener {
	
	private Game game;
	
	public GameListener(Game game) {
		this.game = game;
		game.getPlugin().getServer().getPluginManager().registerEvents(this, game.getPlugin());
	}
	
	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		Player player = e.getPlayer();
		
		if (game.getPlugin().getEditMode().isDebugMode()) {
			System.out.println("[GameListener] onJoin START: " + player.getName());
		}
		
		// Restore player's inventory state (survival or edit mode) from previous session
		game.getPlugin().getEditMode().restoreInventoryState(player);
		
		if (game.getPlugin().getEditMode().isDebugMode()) {
			ItemStack[] inv = player.getInventory().getContents();
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < Math.min(inv.length, 10); i++) {
				if (inv[i] != null && inv[i].getType().toString() != "AIR") {
					if (sb.length() > 0) sb.append(", ");
					sb.append(inv[i].getType());
				}
			}
			System.out.println("[GameListener] After restore: " + (sb.length() == 0 ? "EMPTY" : sb.toString()));
		}
		
		// If player is in edit mode, skip normal game logic
		if (game.getPlugin().getEditMode().isEditing(player)) {
			if (game.getPlugin().getEditMode().isDebugMode()) {
				System.out.println("[GameListener] onJoin: Player is editing, skipping spectator()");
			}
			return;
		}
		
		if (game.getPlugin().getEditMode().isDebugMode()) {
			System.out.println("[GameListener] onJoin: Calling spectator()...");
		}
		game.showPlayer(player);
		player.setGameMode(GameMode.SURVIVAL);
		game.spectator(player);
		
		if (game.getPlugin().getEditMode().isDebugMode()) {
			ItemStack[] invAfter = player.getInventory().getContents();
			StringBuilder sb2 = new StringBuilder();
			for (int i = 0; i < Math.min(invAfter.length, 10); i++) {
				if (invAfter[i] != null && invAfter[i].getType().toString() != "AIR") {
					if (sb2.length() > 0) sb2.append(", ");
					sb2.append(invAfter[i].getType());
				}
			}
			System.out.println("[GameListener] After spectator(): " + (sb2.length() == 0 ? "EMPTY (CLEARED!)" : sb2.toString()));
		}
		if (game.isActive()) {
			// Game.spectator() above hid this player from everyone (since the game is active)
			// and gave them no kit. Unlike a mid-round death, a fresh join has no "waiting to
			// respawn" countdown to honor, so immediately promote them into the active round -
			// otherwise they are stuck as a permanently hidden, invulnerable, kit-less spectator
			// until they happen to die once (the only other code path that calls respawn()).
			game.respawn(player);
		}
		game.getPlugin().updateScoreboard();

		// Game.hidePlayer()/showPlayer() only update visibility for players who were already
		// online at the time they were called, so a player hidden (e.g. a spectator during an
		// active game) before this join would otherwise never be hidden/shown to the new
		// joiner, leaving them appearing as a frozen, non-updating entity. Explicitly sync the
		// new joiner's view of every other currently-online player here.
		for (Player other : game.getPlugin().getServer().getOnlinePlayers()) {
			if (other.equals(player)) {
				continue;
			}
			if (game.isActive() && !game.getPlayerStatus(other)) {
				if (Commands.isDebugMode()) applog.log(Level.INFO, "[DEBUG] onJoin sync: hiding " + other.getName() + " from new joiner " + player.getName());
				player.hidePlayer(game.getPlugin(), other);
			} else {
				if (Commands.isDebugMode()) applog.log(Level.INFO, "[DEBUG] onJoin sync: showing " + other.getName() + " to new joiner " + player.getName());
				player.showPlayer(game.getPlugin(), other);
			}
		}
	}
	
	@EventHandler
	public void onDeath(PlayerDeathEvent e) {
		Player player = e.getEntity();
		if (Commands.isDebugMode()) applog.log(Level.INFO, "[DEBUG] onDeath: " + player.getName() + " died (active=" + game.isActive() + ")");
		e.getDrops().clear();
		game.recordDeathLocation(player);
		// Deliberately NOT touching the player's health/inventory/flags here (that used to
		// happen via game.spectator() at this point): the player is still showing the vanilla
		// death screen at this moment (not yet "alive" again from the engine's perspective), and
		// resetting their living stats on a dead entity appears to be what was freezing the
		// client's own Respawn button. All of that is now deferred to onRespawn() below, which
		// only runs once the engine has actually finished the dead-to-alive transition.
	}

	@EventHandler
	public void onRespawn(PlayerRespawnEvent e) {
		Player player = e.getPlayer();
		if (Commands.isDebugMode()) applog.log(Level.INFO, "[DEBUG] onRespawn: " + player.getName() + " respawning (active=" + game.isActive() + ")");
		// This fires once the player has actually transitioned from dead to alive (whether
		// instantly, with doImmediateRespawn, or after they click the Respawn button) - unlike
		// the previous timer-only approach, re-applying spectator state and scheduling the
		// death countdown here means Game.respawn() (which gives the kit, teleports, and marks
		// the player active) always runs against a player the engine has already finished
		// transitioning, instead of racing that transition on a fixed 8-second clock.
		game.spectator(player);
		Location deathLoc = game.getDeathLocation(player);
		if (deathLoc != null) {
			e.setRespawnLocation(deathLoc);
		}
		if (game.isActive()) {
			game.deathCounter(player, 8);
		}
	}
	
	@EventHandler
	public void onVoid(EntityDamageEvent e) {
		if (e.getCause()==DamageCause.VOID && e.getEntity() instanceof Player) {
			Player p = (Player) e.getEntity();
			p.setHealth(0);
			p.teleport(game.getWorld().getSpawnLocation());
		}
	}
	
	@EventHandler
	public void onItemDrop(PlayerDropItemEvent e) {
		e.setCancelled(true);
	}
	
	@EventHandler
	public void onBreak(BlockBreakEvent e) {
		Player player = e.getPlayer();
		boolean editing = isEditing(player);
		if (Commands.isDebugMode()) applog.log(Level.INFO, "[DEBUG] onBreak: " + player.getName() + " broke " + e.getBlock().getType()
				+ " heldCustomItem=" + heldCustomItemKey(player) + " editing=" + editing
				+ " willCancel=" + !editing);
		if (editing) {
			return;
		}
		e.setCancelled(true);
	}

	@EventHandler
	public void onPlace(BlockPlaceEvent e) {
		if (isEditing(e.getPlayer())) {
			return;
		}
		e.setCancelled(true);
	}
	
	@EventHandler
	public void onBucket(PlayerBucketEmptyEvent e) {
		if (isEditing(e.getPlayer())) {
			return;
		}
		e.setCancelled(true);
	}
	
	@EventHandler
	public void onBucket(PlayerBucketFillEvent e) {
		if (isEditing(e.getPlayer())) {
			return;
		}
		e.setCancelled(true);
	}
	
	@EventHandler(priority = EventPriority.HIGH)
	public void onWorldInteract(PlayerInteractEvent e) {
		Player player = e.getPlayer();
		boolean editing = isEditing(player);
		boolean playing = game.getPlayerStatus(player);
		boolean willCancel = !playing && !editing;
		if (Commands.isDebugMode()) applog.log(Level.INFO, "[DEBUG] onWorldInteract: " + player.getName() + " action=" + e.getAction()
				+ " item=" + e.getItem() + " playerStatus=" + playing + " editing=" + editing
				+ " alreadyCancelled=" + e.isCancelled() + " willCancel=" + willCancel);
		if (willCancel) {
			e.setCancelled(true);
		}
	}
	
	@EventHandler
	public void onItemDamage(PlayerItemDamageEvent e) {
		e.setCancelled(true);
	}
	
	@EventHandler
	public void onHunger(FoodLevelChangeEvent e) {
		e.setCancelled(true);
	}
	
	
	@EventHandler(priority = EventPriority.HIGH)
	public void onAttack(EntityDamageByEntityEvent e) {
		if (e.getDamager().getType()==EntityType.PLAYER) {
			Player player = (Player) e.getDamager();
			if (!game.getPlayerStatus(player)) {
				e.setCancelled(true);
			}
		}
	}
	
	@EventHandler
	public void onProjectileHit(ProjectileHitEvent e) {
		if (e.getEntity() instanceof Arrow) {
			Arrow arrow = (Arrow) e.getEntity();
			arrow.remove();
		}
	}

	@EventHandler
	public void onIgnite(TNTPrimeEvent e) {
		e.setCancelled(true);
	}

	private boolean isEditing(Player player) {
		return game.getPlugin().getEditMode().isEditing(player);
	}

	// For debug logging only: identifies which registered custom item (if any) the player is
	// currently holding in their main hand, by comparing against each CustomItem's canonical
	// template (same equals-after-normalizing-amount check CustomItem itself uses to dispatch).
	private String heldCustomItemKey(Player player) {
		ItemStack held = player.getInventory().getItemInMainHand().clone();
		held.setAmount(1);
		for (Map.Entry<String, CustomItem> entry : game.getPlugin().getCustomItems().entrySet()) {
			ItemStack candidate = entry.getValue().getItem().clone();
			candidate.setAmount(1);
			if (candidate.equals(held)) {
				return entry.getKey();
			}
		}
		return "none";
	}

	@EventHandler
	public void onInventoryClose(org.bukkit.event.inventory.InventoryCloseEvent e) {
		if (!(e.getPlayer() instanceof Player)) {
			return;
		}
		Player player = (Player) e.getPlayer();
		
		if (game.getPlugin().getEditMode().isEditing(player)) {
			if (game.getPlugin().getEditMode().isDebugMode()) {
				System.out.println("[GameListener] onInventoryClose: " + player.getName() + " closed inventory while in edit mode - auto-saving creative inventory");
			}
			game.getPlugin().getEditMode().saveCreativeInventoryToDisk(player);
		}
	}
		
}
