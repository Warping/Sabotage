package bubbles.sabotage.plugin.items;

import bubbles.sabotage.plugin.counter.Counter;
import bubbles.sabotage.plugin.groups.SabTeams;
import bubbles.sabotage.plugin.items.customitem.CustomItem;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkEffectMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Grenade extends CustomItem {
    private final float SLOW_THROW = 0.45F;
    private final float FAST_THROW = 0.95F;
    private final float POWER = 3.0F;
    private final long FUSE_DELAY = 80L;
    private final int MAX_NADES = 4;
    private static final long RELOAD_DELAY = 110L;
    private final HashMap<Player, Boolean> reloading = new HashMap<>();
    private final Material AMMO_TYPE = Material.FIREWORK_STAR;
    public Grenade() {
        super();

        //Set variable item by changing Material.BLahBlah to some other Material

        ItemStack item = new ItemStack(Material.GOLDEN_HOE);
        ItemMeta im = item.getItemMeta();

        // Change the item meta and item details below

        im.setDisplayName(ChatColor.GREEN + "Grenade Lobber");

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.RED + "Highly explosive!");
        lore.add(ChatColor.GOLD + "Left Click to shoot. ");
        lore.add(ChatColor.GOLD + "Right Click to Perform a close shot");
        lore.add(ChatColor.GOLD + "Shift Right Click to reload");
        im.setLore(lore);


        // End of changes

        item.setItemMeta(im);
        setItem(item);

    }

    @Override
    protected void onRightClickAir(PlayerInteractEvent e, boolean mainHand) {
        if (e.getPlayer().isSneaking()) {mainReload(e.getPlayer());}
        else {mainFire(e.getPlayer(), SLOW_THROW);}
    }

    @Override
    protected void onRightClickBlock(PlayerInteractEvent e, boolean mainHand) {
        if (e.getPlayer().isSneaking()) {mainReload(e.getPlayer());}
        else {mainFire(e.getPlayer(), SLOW_THROW);}
    }

    @Override
    protected void onRightClickPlayer(PlayerInteractAtEntityEvent e, boolean mainHand) {
        if (e.getPlayer().isSneaking()) {mainReload(e.getPlayer());}
        else {mainFire(e.getPlayer(), SLOW_THROW);}
    }

    @Override
    protected void onLeftClickAir(PlayerInteractEvent e) {
        mainFire(e.getPlayer(), FAST_THROW);
    }

    @Override
    protected void onLeftClickBlock(PlayerInteractEvent e) {
        mainFire(e.getPlayer(), FAST_THROW);
    }

    @Override
    protected void onDeath(PlayerDeathEvent e) {
        reloading.put(e.getEntity(), false);
    }

    private void mainFire(Player shooter, float throwSpeed) {
        reloading.putIfAbsent(shooter, false);
        if (!contains(shooter, new ItemStack(AMMO_TYPE), 1) || reloading.get(shooter)) {
            shooter.getWorld().playSound(shooter, Sound.BLOCK_STONE_BUTTON_CLICK_OFF, 10.0F, 2.0F);
            return;
        }
        float speed = throwSpeed;
        consume(shooter, new ItemStack(AMMO_TYPE),1);
        throwGrenade(shooter, getGrenade(shooter), speed);
        if (!contains(shooter, new ItemStack(AMMO_TYPE), 1)) {
            mainReload(shooter);
        }
    }

    private void mainReload(Player shooter) {
        reloading.putIfAbsent(shooter, false);
        if (contains(shooter, new ItemStack(AMMO_TYPE), MAX_NADES) || reloading.get(shooter)) {
            shooter.getWorld().playSound(shooter, Sound.BLOCK_STONE_BUTTON_CLICK_OFF, 10.0F, 2.0F);
            return;
        }
        consumeAll(shooter, new ItemStack(AMMO_TYPE));
        shooter.getWorld().playSound(shooter, Sound.BLOCK_PISTON_EXTEND, 10F, 1.2F);
        reloading.put(shooter, true);
        getPlugin().getServer().getScheduler().scheduleSyncDelayedTask(getPlugin(), () -> {
            reloading.putIfAbsent(shooter, false);
            if (reloading.get(shooter)) {
                reloading.put(shooter, false);
                give(shooter, new ItemStack(AMMO_TYPE), MAX_NADES);
                shooter.getWorld().playSound(shooter, Sound.BLOCK_PISTON_CONTRACT, 10F, 1.2F);
            }
        }, RELOAD_DELAY);
    }

    private ItemStack getGrenade(Player shooter) {
        Color color = SabTeams.getDyeColor(getGame().getTeams().getTeamOfPlayer(shooter));
        ItemStack grenade = new ItemStack(Material.FIREWORK_STAR);
        FireworkEffectMeta im = (FireworkEffectMeta) grenade.getItemMeta();
        FireworkEffect dyeColor = FireworkEffect.builder().withColor(color != null ? color : Color.BLACK).build();
        if (im != null) {
            im.setEffect(dyeColor);
        }
        grenade.setItemMeta(im);
        return grenade;
    }

    private void throwGrenade(Player shooter, ItemStack item, float vel) {
        shooter.getWorld().playSound(shooter, Sound.ENTITY_COW_STEP, 10F, 1.7F);
        Item grenade = shooter.getWorld().dropItem(shooter.getEyeLocation(), item);
        grenade.setPickupDelay(1000);
        grenade.setVelocity(shooter.getLocation().getDirection().multiply(vel));
        grenade.setGlowing(true);
        Counter particleTrial = new Counter(1L) {
            @Override
            public void run() {
                shooter.getWorld().spawnParticle(Particle.FLAME, grenade.getLocation().clone().add(0,0.4,0), 5, 0, 0, 0, 0);
            }
        };
        getPlugin().getServer().getScheduler().scheduleSyncDelayedTask(getPlugin(), () -> {
            Location explosionLoc = grenade.getLocation();
            particleTrial.cancel();
            shooter.getWorld().createExplosion(explosionLoc, POWER, false, false);
            grenade.remove();
        }, FUSE_DELAY);
    }

    public void stop() {
        reloading.clear();
    }
}
