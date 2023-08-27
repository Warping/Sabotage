package bubbles.sabotage.plugin.listeners;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scoreboard.Team;

import bubbles.sabotage.plugin.Bomb;

public final class BombListener implements Listener {
	
	private Bomb bomb;
	
	public BombListener(Bomb bomb) {
		bomb.getPlugin().getServer().getPluginManager().registerEvents(this, bomb.getPlugin());
		this.bomb = bomb;
	}
	
	@EventHandler
	public void onBombInteract(PlayerInteractEvent e) {
		if (e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
			if (e.getMaterial().equals(Material.BLAZE_POWDER)) {	
				if (e.getClickedBlock().getLocation().equals(bomb.getArmLoc()) && !bomb.isArmed() && !bomb.isExploded()) {
					int dist = (int) (Math.floor(e.getPlayer().getLocation().distance(bomb.getArmLoc())) + 1);
					if (dist >= 4) {
						return;
					}
					if (bomb.getOwner()!=null) {
						if (bomb.getOwner().getEntries().contains(e.getPlayer().getName())) {
							bomb.denyArm(e.getPlayer());
							return;
						}
					}
					for (Team team : bomb.getSabTeams().getTeams()) {
						if (team.getEntries().contains(e.getPlayer().getName())) {
							Player player = e.getPlayer();
							bomb.arming(player, team);
						}
					}
				} else if (e.getClickedBlock().getLocation().equals(bomb.getDisarmLoc()) && bomb.isArmed() && !bomb.isExploded()) {
					if (bomb.getOwner()!=null) {
						Player player = e.getPlayer();
						if (bomb.getOwner().getEntries().contains(e.getPlayer().getName())) {
							bomb.disarming(player, bomb.getOwner());
						} else {
							bomb.denyDisarm(player);
						}
						return;
					}
					for (Team team : bomb.getSabTeams().getTeams()) {
						if (team.equals(bomb.getArmedBy()) && team.getEntries().contains(e.getPlayer().getName())) {
							bomb.denyDisarm(e.getPlayer());
						}
						else if (!team.equals(bomb.getArmedBy()) && team.getEntries().contains(e.getPlayer().getName())) {
							Player player = e.getPlayer();
							bomb.disarming(player, team);
						}
					}
				} else if (bomb.isExploded() && e.getClickedBlock().getLocation().equals(bomb.getDisarmLoc())) {
					bomb.explodedMsg(e.getPlayer());
				}
			}
		}
	}
	
	@EventHandler
	public void onBreakBomb(BlockBreakEvent e) {
		if (e.getBlock().getLocation().equals(bomb.getDisarmLoc()) || e.getBlock().getLocation().equals(bomb.getArmLoc())) {
			e.setCancelled(true);
		}
	}
	
	@EventHandler
	public void onIgniteBomb(PlayerInteractEvent e) {
		if (e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
			if (e.getClickedBlock().getLocation().equals(bomb.getDisarmLoc()) || e.getClickedBlock().getLocation().equals(bomb.getArmLoc()) && !e.getMaterial().equals(Material.BLAZE_POWDER)) {
				e.setCancelled(true);
			}
		}
	}
	
	@EventHandler 
	public void onArrowIgniteBomb(EntityChangeBlockEvent e) {
		if (e.getBlock().getLocation().equals(bomb.getDisarmLoc()) || e.getBlock().getLocation().equals(bomb.getArmLoc())) {
			if (e.getEntity().getType().equals(EntityType.ARROW) && e.getBlock().getType().equals(Material.TNT)) {
				e.setCancelled(true);
			}
		}
	}
	
}
