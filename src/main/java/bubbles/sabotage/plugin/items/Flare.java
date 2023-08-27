package bubbles.sabotage.plugin.items;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

import bubbles.sabotage.plugin.counter.Counter;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import bubbles.sabotage.plugin.items.customitem.CustomItem;
import org.bukkit.material.MaterialData;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import static bubbles.sabotage.plugin.Main.applog;

public class Flare extends CustomItem {

    private final long FLARE_DELAY = 80L;
    HashMap<FallingBlock, Player> crates = new HashMap<>();
    HashMap<Block, Player> activeCrates = new HashMap<>();
    ArrayList<Item> flares = new ArrayList<>();

    public Flare() {
        super();

        //Set variable item by changing Material.BLAZE_ROD to some other Material

        ItemStack item = new ItemStack(Material.REDSTONE_TORCH);
        ItemMeta im = item.getItemMeta();

        // Change the item meta and item details below

        im.setDisplayName(ChatColor.AQUA + "Flare");

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GOLD + "Throw Flare with right click to spawn supply crate!");
        im.setLore(lore);


        // End of changes

        item.setItemMeta(im);
        setItem(item);

    }

    private void spawnFlare(Player player) {
        consume(player, 1);
        Item flare = player.getWorld().dropItem(player.getEyeLocation(), new ItemStack(Material.REDSTONE_TORCH));
        Vector direction = player.getLocation().getDirection().multiply(0.4);
        flare.setVelocity(direction);

        // Prevent the item from being picked up again and despawn it after 3.5 seconds
        flare.setPickupDelay(1000);
        flares.add(flare);
        Counter particleTrial = new Counter(1L) {
            @Override
            public void run() {
                player.getWorld().spawnParticle(Particle.FLAME, flare.getLocation().clone().add(0,0.4,0), 5, 0, 0, 0, 0);
            }
        };
        getPlugin().getServer().getScheduler().scheduleSyncDelayedTask(getPlugin(), () -> {
            particleTrial.cancel();
            if (!flares.contains(flare)) return;
            Location flareLocation = flare.getLocation();
            flares.remove(flare);
            flare.remove();
            spawnCrate(player, flareLocation);
        }, FLARE_DELAY);
    }

    private void spawnCrate(Player player, Location flareLocation) {
        int x = flareLocation.getBlockX();
        int y = flareLocation.getBlockY();
        int z = flareLocation.getBlockZ();
        boolean canSpawn = true;
        if (canSpawn) {
            for (int i = y; i < y + 60; i++) {
                Block block = player.getWorld().getBlockAt(x, i, z);
                if (block.getType() != Material.AIR) {
                    player.sendMessage(ChatColor.RED + "Invalid Flare Location!");
                    give(player, 1);
                    canSpawn = false;
                    break;
                }
            }
        }
        if (canSpawn) {
            for (Item flare : flares) {
                int xflare = flare.getLocation().getBlockX();
                int yflare = flare.getLocation().getBlockY();
                int zflare = flare.getLocation().getBlockZ();
                if (new Location(player.getWorld(), xflare, yflare, zflare).distance(new Location(player.getWorld(), x, y, z)) < 1) {
                    player.sendMessage(ChatColor.RED + "Invalid Flare Location!");
                    give(player, 1);
                    canSpawn = false;
                    break;
                }
            }
        }
        if (canSpawn) {
            FallingBlock crate = player.getWorld().spawnFallingBlock(new Location(player.getWorld(), x + 0.5, y + 60, z + 0.5), Material.CHEST, (byte) 0);
            crates.put(crate, player);
            player.sendMessage(ChatColor.AQUA + "Supply Crate Inbound...");
            player.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1, 2);
        }
    }

    private void giveLoot(Player p) {
        int random = (((int) (Math.random() * 100)) % 2) + 1;
        give(p, Steak.getStaticItem(), random);
        random = (((int) (Math.random() * 100)) % 2) + 1;
        p.getInventory().addItem(new ItemStack(Material.ENDER_PEARL, random));
        random = (int) (Math.random() * 100);
        if (random <= 20) {
            p.getInventory().addItem(new ItemStack(Material.DIAMOND_CHESTPLATE, 1));
        }
    }

    private void deleteAllCrates() {
        for (FallingBlock crate : crates.keySet()) {
            crate.remove();
        }
        crates.clear();
        for (Block crate : activeCrates.keySet()) {
            crate.setType(Material.AIR);
        }
        activeCrates.clear();
        for (Item flare : flares) {
            flare.remove();
        }
        flares.clear();
    }

    @EventHandler
    public void onFallingBlockLand(final EntityChangeBlockEvent event){
        if(event.getEntity() instanceof FallingBlock){
            FallingBlock crate = (FallingBlock) event.getEntity();
            if(event.getBlock().getType() == Material.AIR){
                new BukkitRunnable(){
                    @Override
                    public void run(){
                        applog.log(Level.INFO, "Crate Landed!");
                        if (crates.get(crate) == null) return;
                        if (crates.get(crate) instanceof Player) {
                            Player p = crates.remove(crate);
                            Block activateCrate = event.getBlock();
                            activeCrates.put(activateCrate, p);
                            p.sendMessage(ChatColor.GREEN + "Supply Crate Landed!");
                            p.getWorld().playSound(p.getLocation(), Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1, 2);
                        }
                    }
                }.runTaskLater(getPlugin(), 1L);
            }
        }
    }

    @EventHandler
    public void onRightClickCrate(PlayerInteractEvent e) {
        if (e.getClickedBlock() == null) return;
        if (e.getClickedBlock().getType() == Material.CHEST) {
            if (activeCrates.get(e.getClickedBlock()) == null) return;
            if (activeCrates.get(e.getClickedBlock()) instanceof Player) {
                if (!e.getPlayer().equals(activeCrates.get(e.getClickedBlock()))) {
                    e.getPlayer().sendMessage(ChatColor.RED + "This is not your crate!");
                    return;
                }
                Player p = activeCrates.remove(e.getClickedBlock());
                e.getClickedBlock().setType(Material.AIR);
                giveLoot(p);
                p.sendMessage(ChatColor.GREEN + "Supply Crate Looted!");
            }
        }
    }

    @Override
    protected void onRightClickPlayer(PlayerInteractAtEntityEvent e, boolean mainHand) {
        spawnFlare(e.getPlayer());
    }

    @Override
    protected void onRightClickAir(PlayerInteractEvent e, boolean mainHand) {
        spawnFlare(e.getPlayer());
    }

    @Override
    protected void onRightClickBlock(PlayerInteractEvent e, boolean mainHand) {
        spawnFlare(e.getPlayer());
    }

    @Override
    public void stop() {
        deleteAllCrates();
    }
}
