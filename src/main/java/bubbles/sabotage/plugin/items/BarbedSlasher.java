package bubbles.sabotage.plugin.items;

import bubbles.sabotage.plugin.Commands;
import bubbles.sabotage.plugin.items.customitem.CustomItem;
import bubbles.sabotage.plugin.util.Text;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import static bubbles.sabotage.plugin.Main.applog;

// An iron sword that prevents natural regeneration and causes bleeding particles
public class BarbedSlasher extends CustomItem {

	private static final int BLEED_DURATION = 10 * 20; // 10 seconds in ticks
	private static final int PARTICLE_INTERVAL = 10; // Spawn particles every 0.5 seconds (10 ticks)
	private static final int PARTICLES_PER_SPAWN = 15; // Number of particles to spawn per interval

	// Wrapper class to track both the end time and the task
	private static class BleedTracker {
		long endTime;
		BukkitTask task;

		BleedTracker(long endTime, BukkitTask task) {
			this.endTime = endTime;
			this.task = task;
		}
	}

	// Map to track which players are currently bleeding with their tracker info
	private final Map<UUID, BleedTracker> bleedingPlayers = new HashMap<>();

	public BarbedSlasher() {
		super();

		ItemStack item = new ItemStack(Material.IRON_SWORD);
		ItemMeta im = item.getItemMeta();

		im.displayName(Text.of(ChatColor.RED + "Barbed Slasher"));
		im.lore(Text.of(buildLore()));

		item.setItemMeta(im);
		setItem(item);
	}

	private List<String> buildLore() {
		List<String> lore = new ArrayList<>();
		lore.add(ChatColor.DARK_RED + "Inflicts bleeding on hit!");
		lore.add(ChatColor.GRAY + "Prevents natural regeneration for 10 seconds");
		lore.add(ChatColor.GRAY + "Victims spurt redstone particles");
		return lore;
	}

	@EventHandler
	public void onBarbedSlasherAttack(EntityDamageByEntityEvent e) {
		if (!(e.getEntity() instanceof Player) || !(e.getDamager() instanceof Player)) {
			return;
		}
		
		Player victim = (Player) e.getEntity();
		Player attacker = (Player) e.getDamager();
		
		ItemStack mainHand = attacker.getInventory().getItemInMainHand();
		ItemStack offHand = attacker.getInventory().getItemInOffHand();
		
		if (!isCustomItem(mainHand) && !isCustomItem(offHand)) {
			return;
		}
		
		if (getGame().getTeams().onSameTeam(attacker, victim)) {
			return;
		}
		
		UUID victimId = victim.getUniqueId();
		
		// Check if victim was already bleeding and cancel old task
		if (bleedingPlayers.containsKey(victimId)) {
			BleedTracker tracker = bleedingPlayers.get(victimId);
			if (Commands.isDebugMode()) applog.log(Level.INFO, "[DEBUG] Barbed Slasher: Cancelling existing bleed task for " + victim.getName());
			tracker.task.cancel();
		}
		
		// Log initial hit
		if (Commands.isDebugMode()) applog.log(Level.INFO, "[DEBUG] Barbed Slasher: " + attacker.getName() + " hit " + victim.getName() + 
				" | Victim Health: " + victim.getHealth() + " | Was already bleeding: " + bleedingPlayers.containsKey(victimId));
		
		// Track this player as bleeding
		long bleedEndTime = System.currentTimeMillis() + (BLEED_DURATION * 50); // Convert ticks to milliseconds
		
		if (Commands.isDebugMode()) applog.log(Level.INFO, "[DEBUG] Barbed Slasher: Bleed effect STARTED for " + victim.getName() + 
				" (will end in 10 seconds)");
		
		// Start particle effect task and prevent regen
		startBleedingParticles(victim, bleedEndTime);
	}

	private void startBleedingParticles(Player victim, long bleedEndTime) {
		BukkitScheduler scheduler = getPlugin().getServer().getScheduler();
		
		BukkitTask[] taskRef = new BukkitTask[1];
		int[] tickCounter = {0}; // Counter to track ticks for sound interval
		
		taskRef[0] = scheduler.runTaskTimer(getPlugin(), () -> {
			// Check if the bleed effect has expired
			if (System.currentTimeMillis() >= bleedEndTime) {
				if (Commands.isDebugMode()) applog.log(Level.INFO, "[DEBUG] Barbed Slasher: Bleed effect ENDED for " + victim.getName() + 
						" | Final Health: " + victim.getHealth());
				bleedingPlayers.remove(victim.getUniqueId());
				taskRef[0].cancel();
				return;
			}
			
			if (!bleedingPlayers.containsKey(victim.getUniqueId())) {
				return;
			}
			
			Location victimLoc = victim.getLocation().add(0, 1, 0); // Spawn at head level
			
			// Spawn red dust particles (main blood effect)
			victim.getWorld().spawnParticle(
				Particle.DUST,
				victimLoc,
				PARTICLES_PER_SPAWN,
				0.4, // X offset (larger spread)
				0.5, // Y offset
				0.4, // Z offset
				1.5, // Speed
				new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 0, 0), 1.2f) // Larger particle size
			);
			
			// Add additional red particles for more visibility
			victim.getWorld().spawnParticle(
				Particle.DUST,
				victimLoc.clone().add(Math.random() - 0.5, Math.random() - 0.5, Math.random() - 0.5),
				8,
				0.2,
				0.2,
				0.2,
				0.8,
				new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 50, 50), 1.0f)
			);
			
			// Play sound every second (every 2 iterations since PARTICLE_INTERVAL is 10 ticks)
			tickCounter[0]++;
			if (tickCounter[0] % 2 == 0) {
				victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.5f, 1.4f);
			}
		}, 0L, PARTICLE_INTERVAL);
		
		// Store the task so we can cancel it if hit again
		bleedingPlayers.put(victim.getUniqueId(), new BleedTracker(bleedEndTime, taskRef[0]));
	}

	@EventHandler
	public void onPlayerRegen(EntityRegainHealthEvent e) {
		if (!(e.getEntity() instanceof Player)) {
			return;
		}
		
		Player player = (Player) e.getEntity();
		
		// Cancel natural regen if player is bleeding
		if (bleedingPlayers.containsKey(player.getUniqueId())) {
			if (Commands.isDebugMode()) applog.log(Level.INFO, "[DEBUG] Barbed Slasher: Blocked regen for " + player.getName() + 
					" | Reason: " + e.getRegainReason() + " | Amount blocked: " + e.getAmount() + 
					" | Current Health: " + player.getHealth());
			e.setCancelled(true);
		}
	}
}
