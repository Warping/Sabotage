package bubbles.sabotage.plugin.items;

import bubbles.sabotage.plugin.items.customitem.CustomItem;
import bubbles.sabotage.plugin.util.Text;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
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
    private final HashMap<Player, Long> reloadStartTime = new HashMap<>();

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

        Arrow arrow = (Arrow) e.getEntity();
        Player shooter = (Player) arrow.getShooter();
        Player victim = (Player) e.getHitEntity();

        double distance = shooter.getLocation().distance(victim.getLocation());
        double damage = 0;

        if (distance >= 1 && distance <= 10) {
            damage = 4.0;
        } else if (distance >= 20 && distance <= 40) {
            damage = 8.0;
        } else if (distance >= 50) {
            victim.setHealth(0);
            return;
        } else {
            return;
        }

        victim.damage(damage);
    }

    private void checkReload(Player shooter) {
        if (arrowCount.getOrDefault(shooter, 0) == 0) {
            reloading.put(shooter, true);
            reloadStartTime.put(shooter, System.currentTimeMillis());
            getPlugin().getServer().getScheduler().runTaskLater(getPlugin(), () -> {
                if (reloading.getOrDefault(shooter, false)) {
                    arrowCount.put(shooter, ARROW_AMMO);
                    reloading.put(shooter, false);
                    reloadStartTime.remove(shooter);
                    give(shooter, new ItemStack(Material.ARROW), ARROW_AMMO);
                    shooter.sendMessage(ChatColor.YELLOW + "Rifle reloaded!");
                }
            }, RELOAD_DELAY);
        }
    }

    @Override
    protected void onRightClickAir(PlayerInteractEvent e, boolean mainHand) {
        Player player = e.getPlayer();
        if (arrowCount.getOrDefault(player, 0) == 0 && reloading.getOrDefault(player, false)) {
            long elapsedMs = System.currentTimeMillis() - reloadStartTime.getOrDefault(player, System.currentTimeMillis());
            long totalMs = RELOAD_DELAY * 50;
            long remainingMs = Math.max(0, totalMs - elapsedMs);
            double remainingSeconds = remainingMs / 1000.0;
            player.sendMessage(ChatColor.RED + String.format("Reloading... %.1fs remaining", remainingSeconds));
        }
    }

    @Override
    protected void onDeath(PlayerDeathEvent e) {
        Player player = e.getEntity();
        arrowCount.put(player, ARROW_AMMO);
        reloading.put(player, false);
        reloadStartTime.remove(player);
    }

    public void stop() {
        arrowCount.clear();
        reloading.clear();
        reloadStartTime.clear();
    }
}

