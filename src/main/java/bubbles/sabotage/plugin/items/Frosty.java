package bubbles.sabotage.plugin.items;

import bubbles.sabotage.plugin.SabotageBootstrap;
import bubbles.sabotage.plugin.items.customitem.CustomItem;
import bubbles.sabotage.plugin.util.Text;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// An iron sword enchanted with Frost Bite: players it hits can't change their selected hotbar slot for 5 seconds
public class Frosty extends CustomItem {

	private static final int FREEZE_DURATION = 5 * 20; // 5 seconds in ticks
	private static final int PARTICLE_INTERVAL = 10; // Same cadence as Barbed Slasher
	private static final int PARTICLES_PER_SPAWN = 15;
	private static final BlockData ICE_BLOCK_DATA = Material.PACKED_ICE.createBlockData();

	// Tasks belonging to one freeze, cancelled together when it ends early or is replaced
	private static class FreezeTracker {
		final BukkitTask unfreeze;
		final BukkitTask particles;

		FreezeTracker(BukkitTask unfreeze, BukkitTask particles) {
			this.unfreeze = unfreeze;
			this.particles = particles;
		}

		void cancel() {
			unfreeze.cancel();
			particles.cancel();
		}
	}

	// Frozen players mapped to their active freeze
	private final Map<UUID, FreezeTracker> frozenPlayers = new HashMap<>();

	public Frosty() {
		super();

		ItemStack item = new ItemStack(Material.IRON_SWORD);
		ItemMeta im = item.getItemMeta();

		im.addEnchant(frostBiteEnchantment(), 1, true);
		im.displayName(Text.of(ChatColor.AQUA + "Frosty"));
		im.lore(Text.of(buildLore()));

		item.setItemMeta(im);
		setItem(item);
	}

	private static Enchantment frostBiteEnchantment() {
		return Registry.ENCHANTMENT.get(NamespacedKey.fromString(SabotageBootstrap.FROST_BITE_KEY));
	}

	private List<String> buildLore() {
		List<String> lore = new ArrayList<>();
		lore.add(ChatColor.AQUA + "Frost Bite: hit players can't change their held item");
		lore.add(ChatColor.GRAY + "Duration: 5 seconds");
		lore.add(ChatColor.GRAY + "Victims shed ice block particles");
		return lore;
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onFrostyAttack(EntityDamageByEntityEvent e) {
		if (!(e.getEntity() instanceof Player) || !(e.getDamager() instanceof Player)) {
			return;
		}

		Player victim = (Player) e.getEntity();
		Player attacker = (Player) e.getDamager();

		ItemStack weapon = attacker.getInventory().getItemInMainHand();
		if (!isCustomItem(weapon)) {
			weapon = attacker.getInventory().getItemInOffHand();
		}
		if (!isCustomItem(weapon) || !weapon.containsEnchantment(frostBiteEnchantment())) {
			return;
		}

		if (getGame().getTeams().onSameTeam(attacker, victim)) {
			return;
		}

		freeze(victim);
	}

	private void freeze(Player victim) {
		UUID victimId = victim.getUniqueId();
		BukkitScheduler scheduler = getPlugin().getServer().getScheduler();
		BukkitTask particles = scheduler.runTaskTimer(getPlugin(), () -> spawnFrostParticles(victim), 0L, PARTICLE_INTERVAL);
		BukkitTask unfreeze = scheduler.runTaskLater(getPlugin(), () -> endFreeze(victimId), FREEZE_DURATION);
		// Re-hitting a frozen player restarts the freeze, so the previous freeze's tasks are cancelled
		FreezeTracker previous = frozenPlayers.put(victimId, new FreezeTracker(unfreeze, particles));
		if (previous != null) {
			previous.cancel();
		}
	}

	private void spawnFrostParticles(Player victim) {
		if (!victim.isOnline()) {
			return;
		}
		World world = victim.getWorld();
		Location head = victim.getLocation().add(0, 1, 0);
		world.spawnParticle(Particle.BLOCK, head, PARTICLES_PER_SPAWN, 0.4, 0.5, 0.4, 1.5, ICE_BLOCK_DATA);
		world.spawnParticle(Particle.BLOCK,
				head.clone().add(Math.random() - 0.5, Math.random() - 0.5, Math.random() - 0.5),
				8, 0.2, 0.2, 0.2, 0.8, ICE_BLOCK_DATA);
	}

	private void endFreeze(UUID playerId) {
		FreezeTracker tracker = frozenPlayers.remove(playerId);
		if (tracker != null) {
			tracker.cancel();
		}
	}

	// Cancelling the event keeps the selected slot where it was when the freeze started.
	@EventHandler
	public void onFrozenHeldItemChange(PlayerItemHeldEvent e) {
		if (frozenPlayers.containsKey(e.getPlayer().getUniqueId())) {
			e.setCancelled(true);
		}
	}

	// Death lifts the freeze and stops the particles regardless of what the victim is carrying.
	@EventHandler
	public void onFrozenPlayerDeath(PlayerDeathEvent e) {
		endFreeze(e.getEntity().getUniqueId());
	}
}
