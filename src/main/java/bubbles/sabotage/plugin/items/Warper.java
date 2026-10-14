package bubbles.sabotage.plugin.items;

import bubbles.sabotage.plugin.Commands;
import bubbles.sabotage.plugin.Main;
import bubbles.sabotage.plugin.counter.Counter;
import bubbles.sabotage.plugin.items.customitem.CustomItem;
import bubbles.sabotage.plugin.util.Text;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.*;
import java.util.logging.Level;
import static bubbles.sabotage.plugin.Main.applog;

public class Warper extends CustomItem {

	private final HashMap<Player, Queue<Location>> recentPos = new HashMap<>();
	private final int MAX_WARP_TIME = 15;

	public Warper() {
		super();

		ItemStack item = new ItemStack(Material.BLAZE_ROD);
		ItemMeta im = item.getItemMeta();

		im.displayName(Text.of(ChatColor.DARK_PURPLE + "Warper"));

		List<String> lore = new ArrayList<>();
		lore.add(ChatColor.GOLD + "Right Click an enemy to warp them");
		lore.add(ChatColor.GOLD + "to where you were 10 seconds ago");
		im.lore(Text.of(lore));

		item.setItemMeta(im);
		setItem(item);

		new Counter(20L) {
			int tickCount = 0;
			@Override
			public void run() {
				tickCount++;
				if (tickCount % 20 == 0 && Commands.isDebugMode()) {
					Main.applog.log(Level.INFO, "[Warper Debug Counter] Running... Players in world: " + getGame().getWorld().getPlayers().size());
				}
				
				for (Player p : getGame().getWorld().getPlayers()) {
					boolean hasItem = contains(p, getItem(), 1);
					if (Commands.isDebugMode() && tickCount % 20 == 0) {
						Main.applog.log(Level.INFO, "[Warper Debug Counter] " + p.getName() + " has Warper? " + hasItem);
					}
					
					if (!hasItem) {
						recentPos.remove(p);
						continue;
					}
					recentPos.putIfAbsent(p, new LinkedList<>());
					Queue<Location> queue = recentPos.get(p);
					if (queue.size() >= MAX_WARP_TIME) {
						queue.poll();
					}
					queue.add(p.getLocation());
					recentPos.put(p, queue);
					if (Commands.isDebugMode() && tickCount % 20 == 0) {
						Main.applog.log(Level.INFO, "[Warper Debug Counter] " + p.getName() + " queue size: " + queue.size());
					}
					if (queue.size() == MAX_WARP_TIME - 1) {
						getPlugin().getServer().getScheduler().runTaskLater(getPlugin(), () -> {
							p.sendMessage(ChatColor.GREEN + "You can now Warp!");
							@NonNull Sound blockNoteBlockHarp = Sound.BLOCK_NOTE_BLOCK_HARP;
							if (blockNoteBlockHarp != null) {
								p.getWorld().playSound(p, blockNoteBlockHarp, 0.5F, 2);
							} else {
								applog.log(Level.WARNING, "Sound BLOCK_NOTE_BLOCK_HARP is null!");
							}
						}, 20L);
					}
				}
			}
		};
	}

	private void warp(Player attacker, Player victim) {
		if (Commands.isDebugMode()) {
			Main.applog.log(Level.INFO, "[Warper Debug] Warp attempt: " + attacker.getName() + " -> " + victim.getName());
		}
		
		if (recentPos.get(attacker) == null) {
			if (Commands.isDebugMode()) {
				Main.applog.log(Level.INFO, "[Warper Debug] No location history for " + attacker.getName());
			}
			return;
		}
		
		if (Commands.isDebugMode()) {
			Main.applog.log(Level.INFO, "[Warper Debug] Queue size: " + recentPos.get(attacker).size() + " (need " + MAX_WARP_TIME + ")");
		}
		
		if (recentPos.get(attacker).size() < MAX_WARP_TIME) {
			attacker.sendMessage(ChatColor.RED + "Cannot warp this early! Wait " + (MAX_WARP_TIME - recentPos.get(attacker).size()) + " seconds");
			@NonNull Sound blockNoteBlockBass = Sound.BLOCK_NOTE_BLOCK_BASS;
			if (blockNoteBlockBass != null) {
				attacker.getWorld().playSound(attacker, blockNoteBlockBass, 0.5F, 1);
			} else {
				applog.log(Level.WARNING, "Sound BLOCK_NOTE_BLOCK_BASS is null!");
			}
			if (Commands.isDebugMode()) {
				Main.applog.log(Level.INFO, "[Warper Debug] Warp blocked - insufficient cooldown");
			}
			return;
		}

		// Check if players are on the same team
		if (getGame().getTeams().onSameTeam(attacker, victim)) {
			attacker.sendMessage(ChatColor.RED + "Can't warp teammates!");
			@NonNull Sound blockNoteBlockBass2 = Sound.BLOCK_NOTE_BLOCK_BASS;
			if (blockNoteBlockBass2 != null) {
				attacker.getWorld().playSound(attacker, blockNoteBlockBass2, 0.5F, 1);
			} else {
				applog.log(Level.WARNING, "Sound BLOCK_NOTE_BLOCK_BASS is null!");
			}
			if (Commands.isDebugMode()) {
				Main.applog.log(Level.INFO, "[Warper Debug] Warp blocked - same team");
			}
			return;
		}

		if (Commands.isDebugMode()) {
			Main.applog.log(Level.INFO, "[Warper Debug] Warp executing - teleporting " + victim.getName());
		}

		victim.getWorld().spawnParticle(Particle.CRIT, victim.getLocation(), 100, 0.1, 0.5, 0.1, 1);
		@NonNull Sound entityEndermanTeleport = Sound.ENTITY_ENDERMAN_TELEPORT;
		if (entityEndermanTeleport != null) {
			victim.getWorld().playSound(victim, entityEndermanTeleport, 0.5F, 1);
		} else {
			applog.log(Level.WARNING, "Sound ENTITY_ENDERMAN_TELEPORT is null!");
		}
		victim.teleport(recentPos.get(attacker).peek());
		@NonNull Sound entityEndermanTeleport2 = Sound.ENTITY_ENDERMAN_TELEPORT;
		if (entityEndermanTeleport2 != null) {
			victim.getWorld().playSound(victim, entityEndermanTeleport2, 0.5F, 1);
		} else {
			applog.log(Level.WARNING, "Sound ENTITY_ENDERMAN_TELEPORT is null!");
		}
		victim.getWorld().spawnParticle(Particle.CRIT, victim.getLocation(), 100, 0.1, 0.5, 0.1, 1);

		attacker.sendMessage(ChatColor.GREEN + "Warped " + victim.getName() + " to your location from 10 seconds ago!");
		victim.sendMessage(ChatColor.RED + attacker.getName() + " warped you!");
		
		if (Commands.isDebugMode()) {
			Main.applog.log(Level.INFO, "[Warper Debug] Warp successful");
		}
	}

	@Override
	protected void onRightClickPlayer(PlayerInteractAtEntityEvent e, boolean mainHand) {
		if (Commands.isDebugMode()) {
			Main.applog.log(Level.INFO, "[Warper Debug] onRightClickPlayer called");
		}
		Player attacker = e.getPlayer();
		if (!(e.getRightClicked() instanceof Player)) {
			if (Commands.isDebugMode()) {
				Main.applog.log(Level.INFO, "[Warper Debug] Right clicked entity is not a player: " + e.getRightClicked().getClass().getName());
			}
			return;
		}
		Player victim = (Player) e.getRightClicked();
		if (Commands.isDebugMode()) {
			Main.applog.log(Level.INFO, "[Warper Debug] Attacker: " + attacker.getName() + ", Victim: " + victim.getName());
		}
		warp(attacker, victim);
	}

	@Override
	protected void onRightClickAir(PlayerInteractEvent e, boolean mainHand) {
		// No effect when clicking air
	}

	@Override
	protected void onRightClickBlock(PlayerInteractEvent e, boolean mainHand) {
		// No effect when clicking blocks
	}

	@Override
	protected void onLeftClickAir(PlayerInteractEvent e) {
		// No effect
	}

	@Override
	protected void onLeftClickBlock(PlayerInteractEvent e) {
		// No effect
	}

	@Override
	public void stop() {
		recentPos.clear();
	}
}
