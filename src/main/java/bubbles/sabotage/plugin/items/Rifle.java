package bubbles.sabotage.plugin.items;

import bubbles.sabotage.plugin.items.customitem.CustomItem;
import bubbles.sabotage.plugin.util.Text;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Rifle extends CustomItem {
    
    private static final int ARROW_AMMO = 3;
    private static final long RELOAD_DELAY = 200L;
    private final HashMap<Player, Integer> arrowCount = new HashMap<>();
    private final HashMap<Player, Boolean> reloading = new HashMap<>();

    public Rifle() {
        super();

        ItemStack item = new ItemStack(Material.BOW);
        ItemMeta im = item.getItemMeta();

        im.displayName(Text.of(ChatColor.DARK_RED + "Rifle"));

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GOLD + "1-10 blocks: 2 hearts");
        lore.add(ChatColor.GOLD + "20-40 blocks: 4 hearts");
        lore.add(ChatColor.GOLD + "50+ blocks: instant kill");
        lore.add(ChatColor.RED + "3 arrows, 10s reload");
        im.lore(Text.of(lore));

        im.setUnbreakable(true);

        item.setItemMeta(im);
        setItem(item);
    }

    @Override
    protected void onShoot(EntityShootBowEvent e, boolean mainHand) {
        if (!(e.getEntity() instanceof Player)) {
            return;
        }

        Player shooter = (Player) e.getEntity();
        arrowCount.putIfAbsent(shooter, ARROW_AMMO);
        int remaining = arrowCount.get(shooter);

        if (remaining <= 0) {
            e.setCancelled(true);
            return;
        }

        arrowCount.put(shooter, remaining - 1);

        if (arrowCount.get(shooter) == 0) {
            checkReload(shooter);
        }
    }

    @Override
    protected void onShotPlayer(ProjectileHitEvent e, boolean mainHand) {
        if (!(e.getEntity() instanceof Arrow)) {
            return;
        }

        if (!(e.getHitEntity() instanceof Player)) {
            return;
        }

        e.setCancelled(true);

        Arrow arrow = (Arrow) e.getEntity();
        Player shooter = (Player) arrow.getShooter();
        Player victim = (Player) e.getHitEntity();

        double distance = shooter.getLocation().distance(victim.getLocation());
        double damage = 0;

        if (distance >= 1 && distance <= 20) {
            damage = 2.0;
        } else if (distance >= 20 && distance <= 40) {
            damage = 4.0;
        } else if (distance >= 50) {
            damage = 100.0; // Instant kill
            return;
        } else {
            return;
        }

        DamageSource source = DamageSource.builder(DamageType.ARROW)
            .withCausingEntity(shooter) // Explicitly attributes credit for the hit to the shooter
            .withDirectEntity(arrow)     // Tracks the arrow as the direct projectile weapon
            .build();
            
        if (source != null) {
            victim.damage(damage, source);
        }
    }

    private void checkReload(Player shooter) {
        if (arrowCount.getOrDefault(shooter, 0) == 0) {
            reloading.put(shooter, true);
            getPlugin().getServer().getScheduler().runTaskLater(getPlugin(), () -> {
                if (reloading.getOrDefault(shooter, false)) {
                    arrowCount.put(shooter, ARROW_AMMO);
                    reloading.put(shooter, false);
                    give(shooter, new ItemStack(Material.ARROW), ARROW_AMMO);
                    shooter.sendMessage(ChatColor.YELLOW + "Rifle reloaded!");
                }
            }, RELOAD_DELAY);
        }
    }

    @Override
    protected void onDeath(PlayerDeathEvent e) {
        Player player = e.getEntity();
        arrowCount.put(player, ARROW_AMMO);
        reloading.put(player, false);
    }

    public void stop() {
        arrowCount.clear();
        reloading.clear();
    }
}

