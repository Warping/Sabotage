package bubbles.sabotage.plugin.items;

import bubbles.sabotage.plugin.Main;
import bubbles.sabotage.plugin.counter.Counter;
import bubbles.sabotage.plugin.groups.SabTeams;
import bubbles.sabotage.plugin.items.customitem.CustomItem;
import bubbles.sabotage.plugin.util.Text;
import bubbles.sabotage.plugin.util.Blocks;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkEffectMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

public class Grenade extends CustomItem {
    private final float SLOW_THROW = 0.45F;
    private final float FAST_THROW = 0.95F;
    private final float POWER = 10.0F;
    private final float RADIUS = 7.0F;
    private final long FUSE_DELAY = 100L;
    private final int MAX_NADES = 4;
    private static final long RELOAD_DELAY = 110L;
    private final boolean DEBUG = false;
    private final HashMap<Player, Boolean> reloading = new HashMap<>();
    private final Material AMMO_TYPE = Material.FIREWORK_STAR;
    public Grenade() {
        super();

        //Set variable item by changing Material.BLahBlah to some other Material

        ItemStack item = new ItemStack(Material.GOLDEN_HOE);
        ItemMeta im = item.getItemMeta();

        // Change the item meta and item details below

        im.displayName(Text.of(ChatColor.GREEN + "Grenade Lobber"));

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.RED + "Highly explosive!");
        lore.add(ChatColor.GOLD + "Left Click to shoot. ");
        lore.add(ChatColor.GOLD + "Right Click to Perform a close shot");
        lore.add(ChatColor.GOLD + "Shift Right Click to reload");
        im.lore(Text.of(lore));


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
        Player player = e.getEntity();
        reloading.put(player, false);
        
        // If this player just took self-damage from a grenade, update the death message
        if (lastSelfDamagedPlayer == player) {
            e.setDeathMessage(ChatColor.RED + player.getName() + " was blown up by their own grenade");
            lastSelfDamagedPlayer = null;
        }
    }

    private void mainFire(Player shooter, float throwSpeed) {
        reloading.putIfAbsent(shooter, false);
        if (!contains(shooter, new ItemStack(AMMO_TYPE), 1) || reloading.get(shooter)) {
            shooter.getWorld().playSound(shooter, Sound.BLOCK_STONE_BUTTON_CLICK_OFF, 0.5F, 2.0F);
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
            shooter.getWorld().playSound(shooter, Sound.BLOCK_STONE_BUTTON_CLICK_OFF, 0.5F, 2.0F);
            return;
        }
        consumeAll(shooter, new ItemStack(AMMO_TYPE));
        shooter.getWorld().playSound(shooter, Sound.BLOCK_PISTON_EXTEND, 0.5F, 1.2F);
        reloading.put(shooter, true);
        getPlugin().getServer().getScheduler().runTaskLater(getPlugin(), () -> {
            reloading.putIfAbsent(shooter, false);
            if (reloading.get(shooter)) {
                reloading.put(shooter, false);
                give(shooter, new ItemStack(AMMO_TYPE), MAX_NADES);
                shooter.getWorld().playSound(shooter, Sound.BLOCK_PISTON_CONTRACT, 0.5F, 1.2F);
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
        shooter.getWorld().playSound(shooter, Sound.ENTITY_COW_STEP, 1.0F, 1.7F);
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
        getPlugin().getServer().getScheduler().runTaskLater(getPlugin(), () -> {
            Location explosionLoc = grenade.getLocation();
            explodeGrenade(explosionLoc, shooter);
            particleTrial.cancel();
            
            grenade.remove();
        }, FUSE_DELAY);
    }

    private Player lastSelfDamagedPlayer = null;

    private void explodeGrenade(Location explosionLoc, Player shooter) {
        explosionLoc.getWorld().createExplosion(explosionLoc, 0, false, false);

        if (DEBUG) {
            Main.applog.log(Level.INFO, "[Grenade Debug] Explosion at " + explosionLoc.getX() + ", " + explosionLoc.getY() + ", " + explosionLoc.getZ() + " triggered by " + shooter.getName());
        }

        DamageSource damageSource = DamageSource.builder(DamageType.PLAYER_EXPLOSION)
            .withCausingEntity(shooter)
            .withDirectEntity(shooter)
            .withDamageLocation(explosionLoc)
            .build();
        
        DamageSource selfDamageSource = DamageSource.builder(DamageType.EXPLOSION)
            .withDamageLocation(explosionLoc)
            .build();
        
        // Get nearby entities directly from explosion location
        Collection<Entity> nearbyEntities = explosionLoc.getWorld().getNearbyEntities(explosionLoc, RADIUS, RADIUS, RADIUS);
        
        if (DEBUG) {
            Main.applog.log(Level.INFO, "[Grenade Debug] Found " + nearbyEntities.size() + " nearby entities");
        }
        
        nearbyEntities.forEach(entity -> {
            if (DEBUG) {
                Main.applog.log(Level.INFO, "[Grenade Debug] Checking entity: " + entity.getType() + " (" + entity.getClass().getSimpleName() + ")");
            }
            
            if (!(entity instanceof Player victim)) {
                if (DEBUG) {
                    Main.applog.log(Level.INFO, "[Grenade Debug] Entity is not a player, skipping");
                }
                return;
            }

            if (DEBUG) {
                Main.applog.log(Level.INFO, "[Grenade Debug] Player found: " + victim.getName());
            }

            // Allow self-damage, skip teammates
            if (victim != shooter) {
                boolean onSameTeam = getGame().getTeams().onSameTeam(shooter, victim);
                if (DEBUG) {
                    Main.applog.log(Level.INFO, "[Grenade Debug] Team check: " + victim.getName() + " on same team as shooter? " + onSameTeam);
                }
                if (onSameTeam) {
                    if (DEBUG) {
                        Main.applog.log(Level.INFO, "[Grenade Debug] " + victim.getName() + " is teammate, skipping");
                    }
                    return;
                }
            } else {
                if (DEBUG) {
                    Main.applog.log(Level.INFO, "[Grenade Debug] " + victim.getName() + " is the shooter, allowing self-damage");
                    Main.applog.log(Level.INFO, "[Grenade Debug] Shooter team: " + getGame().getTeams().getTeamOfPlayer(shooter));
                    Main.applog.log(Level.INFO, "[Grenade Debug] Shooter active player status: " + getGame().getPlayerStatus(shooter));
                }
            }

            Location victimLoc = victim.getLocation();
            double distance = victimLoc.distance(explosionLoc);
            
            if (DEBUG) {
                Main.applog.log(Level.INFO, "[Grenade Debug] " + victim.getName() + " distance: " + distance);
            }
            
            if (distance == 0) {
                distance = 0.1;
            }

            // Proximity damage math
            double damage = (1 / distance) * POWER;

            if (DEBUG) {
                Main.applog.log(Level.INFO, "[Grenade Debug] Calculated damage for " + victim.getName() + ": " + damage);
            }

            // Apply a damage penalty if a solid wall covers the blast line of sight
            if (distance > 0.001 && new Blocks().isBlockBetween(explosionLoc, victimLoc)) {
                damage = damage / 3;
                if (DEBUG) {
                    Main.applog.log(Level.INFO, "[Grenade Debug] Block between explosion and " + victim.getName() + ", damage reduced to: " + damage);
                }
            }

            // Apply custom tracked damage
            if (damageSource != null) {
                if (DEBUG) {
                    Main.applog.log(Level.INFO, "[Grenade Debug] Applying " + damage + " damage to " + victim.getName());
                    Main.applog.log(Level.INFO, "[Grenade Debug] Before damage - Health: " + victim.getHealth() + ", MaxHealth: " + victim.getMaxHealth());
                }
                
                // For self-damage, mark it and use simple damage() so the event fires properly
                if (victim == shooter) {
                    if (selfDamageSource != null) {
                        victim.damage(damage, selfDamageSource);
                    }
                    if (DEBUG) {
                        Main.applog.log(Level.INFO, "[Grenade Debug] Applied self-damage (will set explosion cause on death)");
                    }
                } else {
                    victim.damage(damage, damageSource);
                }
                
                if (DEBUG) {
                    Main.applog.log(Level.INFO, "[Grenade Debug] After damage - Health: " + victim.getHealth());
                    Main.applog.log(Level.INFO, "[Grenade Debug] Damage event fired for " + victim.getName());
                }
            } else {
                if (DEBUG) {
                    Main.applog.log(Level.WARNING, "[Grenade Debug] DamageSource is null for " + victim.getName());
                }
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player victim)) {
            return;
        }
        if (e.getCause().toString().contains("EXPLOSION")) {
            if (DEBUG) {
                Main.applog.log(Level.INFO, "[Grenade Debug] EntityDamageEvent MONITOR: " + victim.getName() + " taking " + e.getDamage() + " damage, cancelled=" + e.isCancelled());
            }
        }
    }

    public void stop() {
        reloading.clear();
    }
}
