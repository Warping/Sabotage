package bubbles.sabotage.plugin.items;

import bubbles.sabotage.plugin.Commands;
import bubbles.sabotage.plugin.items.customitem.CustomItem;
import bubbles.sabotage.plugin.util.Text;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class Teleporter extends CustomItem {

    private static final int MAX_TELEPORTERS_PER_PLAYER = 2;
    private static final long PLACEMENT_DELAY = 2000L;
    private static final long TELEPORT_DELAY = 2000L;
    private static final long ACTIVATION_DELAY = 2000L;
    private final long RELOAD_DELAY = 100L;
    private final boolean DEBUG = false;
    
    HashMap<Location, Player> teleporterLocations = new HashMap<>();
    HashMap<Player, List<Location>> playerTeleporters = new HashMap<>();
    HashMap<Player, Long> lastPlacementTime = new HashMap<>();
    HashMap<Player, Long> lastTeleportTime = new HashMap<>();
    HashMap<Player, Long> lastActivationTime = new HashMap<>();

    public Teleporter() {
        super();

        ItemStack item = new ItemStack(Material.STONE_PICKAXE);
        ItemMeta im = item.getItemMeta();

        im.displayName(Text.of(ChatColor.BLUE + "Teleporter Placement Tool"));

        List<String> lore = new ArrayList<>();
        lore.add(org.bukkit.ChatColor.AQUA + "Right Click to Place Teleporter.");
        lore.add(org.bukkit.ChatColor.AQUA + "Left Click to Pick Up Teleporter.");
        lore.add(org.bukkit.ChatColor.AQUA + "Max 2 per player. Right-click to teleport.");
        im.lore(Text.of(lore));
        item.setItemMeta(im);
        setItem(item);
    }

    private boolean isForbiddenPlacementBlock(Material material) {
        if (material == Material.SPAWNER) {
            return true;
        }
        
        String name = material.name();
        
        if (name.endsWith("_PRESSURE_PLATE")) {
            return true;
        }
        
        if (name.endsWith("_BUTTON")) {
            return true;
        }
        
        return false;
    }

    private void placeTeleporter(Player p, Block block) {
        Location loc = block.getLocation();
        
        if (isForbiddenPlacementBlock(block.getType())) {
            p.sendMessage(ChatColor.RED + "Cannot place teleporter on that block.");
            return;
        }
        
        if (!contains(p, new ItemStack(Material.SPAWNER), 1)) {
            p.sendMessage(ChatColor.RED + "You have no teleporters.");
            return;
        }
        
        if (!playerTeleporters.containsKey(p)) {
            playerTeleporters.put(p, new ArrayList<>());
        }
        
        long currentTime = System.currentTimeMillis();
        if (lastPlacementTime.containsKey(p)) {
            long timeSinceLastPlacement = currentTime - lastPlacementTime.get(p);
            if (timeSinceLastPlacement < PLACEMENT_DELAY) {
                long timeRemaining = (PLACEMENT_DELAY - timeSinceLastPlacement) / 1000;
                p.sendMessage(ChatColor.RED + "Wait " + timeRemaining + " second(s) before placing another teleporter.");
                return;
            }
        }
        
        if (playerTeleporters.get(p).size() >= MAX_TELEPORTERS_PER_PLAYER) {
            p.sendMessage(ChatColor.RED + "You have reached the maximum number of teleporters (2).");
            return;
        }
        
        Location teleporterLoc = loc.clone().add(0, 1, 0);
        
        if (teleporterLocations.containsKey(teleporterLoc)) {
            p.sendMessage(ChatColor.RED + "A teleporter is already placed here.");
            return;
        }
        
        if (loc.clone().add(0, 1, 0).getBlock().getType() != Material.AIR ||
            loc.clone().add(0, 2, 0).getBlock().getType() != Material.AIR) {
            return;
        }
        
        if (!block.getType().isOccluding()) {
            return;
        }
        
        consume(p, new ItemStack(Material.SPAWNER), 1);
        teleporterLocations.put(teleporterLoc, p);
        playerTeleporters.get(p).add(teleporterLoc);
        lastPlacementTime.put(p, currentTime);
        
        if (playerTeleporters.get(p).size() == MAX_TELEPORTERS_PER_PLAYER) {
            lastActivationTime.put(p, currentTime);
            p.sendMessage(ChatColor.YELLOW + "Both teleporters placed! Activation in 2 seconds...");
        }
        
        teleporterLoc.getBlock().setType(Material.SPAWNER);
        
        p.sendMessage(ChatColor.GREEN + "Teleporter placed. (" + playerTeleporters.get(p).size() + "/" + MAX_TELEPORTERS_PER_PLAYER + ")");
        p.getWorld().playSound(teleporterLoc, Sound.BLOCK_BEACON_ACTIVATE, 2.0F, 1.5F);
    }

    private void removeTeleporter(Player p, Location loc) {
        if (!teleporterLocations.containsKey(loc)) return;
        Player owner = teleporterLocations.get(loc);
        if (!owner.equals(p)) return;
        
        loc.getBlock().setType(Material.AIR);
        teleporterLocations.remove(loc);
        
        if (playerTeleporters.containsKey(p)) {
            playerTeleporters.get(p).remove(loc);
        }
        
        give(p, new ItemStack(Material.SPAWNER), 1);
        p.getWorld().playSound(loc, Sound.BLOCK_BEACON_DEACTIVATE, 2.0F, 1.5F);
        p.sendMessage(ChatColor.GREEN + "Teleporter removed.");
    }

    private void useTeleporter(Player p, Location loc) {
        if (DEBUG) System.out.println("[TELEPORTER DEBUG] useTeleporter called for player " + p.getName() + " at location " + loc);
        
        if (!teleporterLocations.containsKey(loc)) {
            if (DEBUG) System.out.println("[TELEPORTER DEBUG] Location not in teleporterLocations");
            return;
        }
        Player owner = teleporterLocations.get(loc);
        
        if (!getGame().getTeams().onSameTeam(owner, p)) {
            if (DEBUG) System.out.println("[TELEPORTER DEBUG] Different teams");
            p.sendMessage(ChatColor.RED + "This teleporter belongs to an enemy team member.");
            return;
        }
        
        if (!playerTeleporters.containsKey(owner) || playerTeleporters.get(owner).size() < 2) {
            if (DEBUG) System.out.println("[TELEPORTER DEBUG] Both teleporters must be placed - Owner: " + owner.getName() + ", Size: " + (playerTeleporters.containsKey(owner) ? playerTeleporters.get(owner).size() : 0));
            p.sendMessage(ChatColor.RED + "Both teleporters must be placed to use them.");
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        if (lastActivationTime.containsKey(owner)) {
            long timeSinceActivation = currentTime - lastActivationTime.get(owner);
            if (timeSinceActivation < ACTIVATION_DELAY) {
                long timeRemaining = (ACTIVATION_DELAY - timeSinceActivation) / 1000;
                if (DEBUG) System.out.println("[TELEPORTER DEBUG] Activation delay - remaining: " + timeRemaining);
                p.sendMessage(ChatColor.RED + "Teleporters activating in " + timeRemaining + " second(s)...");
                return;
            }
        }
        
        if (lastTeleportTime.containsKey(p)) {
            long timeSinceLastTeleport = currentTime - lastTeleportTime.get(p);
            if (timeSinceLastTeleport < TELEPORT_DELAY) {
                long timeRemaining = (TELEPORT_DELAY - timeSinceLastTeleport) / 1000;
                if (DEBUG) System.out.println("[TELEPORTER DEBUG] Teleport cooldown - remaining: " + timeRemaining);
                p.sendMessage(ChatColor.RED + "Wait " + timeRemaining + " second(s) before teleporting again.");
                return;
            }
        }
        
        List<Location> destinations = playerTeleporters.get(owner);
        Location teleportFromLoc = null;
        Location teleportToLoc = null;
        
        for (Location destLoc : destinations) {
            if (destLoc.getBlockX() == loc.getBlockX() && 
                destLoc.getBlockY() == loc.getBlockY() && 
                destLoc.getBlockZ() == loc.getBlockZ()) {
                teleportFromLoc = destLoc;
            } else {
                teleportToLoc = destLoc;
            }
        }
        
        if (teleportToLoc == null) return;
        
        Location teleportLoc = teleportToLoc.clone().add(0, 1, 0);
        
        p.getWorld().spawnParticle(org.bukkit.Particle.FLAME, teleportFromLoc.clone().add(0.5, 1.5, 0.5), 50, 0.3, 0.8, 0.3, 0.2);
        p.teleport(teleportLoc);
        p.getWorld().spawnParticle(org.bukkit.Particle.FLAME, teleportLoc.clone().add(0.5, 0.5, 0.5), 50, 0.3, 0.8, 0.3, 0.2);
        
        lastTeleportTime.put(p, currentTime);
        
        p.getWorld().playSound(teleportFromLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0F, 0.7F);
        p.getWorld().playSound(teleportLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0F, 0.7F);
        p.sendMessage(ChatColor.GREEN + "Teleported!");
    }

    @EventHandler
    public void onTeleporterBreak(BlockBreakEvent e) {
        Block block = e.getBlock();
        Location loc = block.getLocation();
        
        if (!teleporterLocations.containsKey(loc)) {
            return;
        }
        
        Player breaker = e.getPlayer();
        Player owner = teleporterLocations.get(loc);
        
        if (getGame().getTeams().onSameTeam(breaker, owner)) {
            e.setCancelled(true);
            breaker.sendMessage(ChatColor.RED + "You cannot break your teammate's teleporter.");
            return;
        }
        
        e.setCancelled(true);
        loc.getBlock().setType(Material.AIR);
        teleporterLocations.remove(loc);
        
        if (playerTeleporters.containsKey(owner)) {
            playerTeleporters.get(owner).remove(loc);
        }
        
        breaker.sendMessage(ChatColor.GREEN + "You destroyed " + owner.getName() + "'s teleporter!");
        owner.sendMessage(ChatColor.RED + breaker.getName() + " destroyed your teleporter!");
        
        breaker.getWorld().playSound(loc, Sound.BLOCK_BEACON_DEACTIVATE, 10.0F, 1.0F);
        
        getPlugin().getServer().getScheduler().runTaskLater(getPlugin(), () -> {
            give(owner, new ItemStack(Material.SPAWNER), 1);
            owner.sendMessage(ChatColor.YELLOW + "Your teleporter has been returned to your inventory.");
        }, 200L);
    }

    @Override
    protected void onRightClickBlock(PlayerInteractEvent e, boolean mainHand) {
        if (DEBUG) System.out.println("[TELEPORTER DEBUG] onRightClickBlock fired for " + e.getPlayer().getName());
        Block block = e.getClickedBlock();
        Location blockLoc = block.getLocation();
        
        if (block.getType() == Material.SPAWNER && teleporterLocations.containsKey(blockLoc)) {
            if (DEBUG) System.out.println("[TELEPORTER DEBUG] onRightClickBlock -> useTeleporter (direct spawner)");
            e.setCancelled(true);
            useTeleporter(e.getPlayer(), blockLoc);
        } else {
            Location blockAbove = blockLoc.clone().add(0, 1, 0);
            if (teleporterLocations.containsKey(blockAbove)) {
                if (DEBUG) System.out.println("[TELEPORTER DEBUG] onRightClickBlock -> useTeleporter (block above)");
                e.setCancelled(true);
                useTeleporter(e.getPlayer(), blockAbove);
            } else {
                if (DEBUG) System.out.println("[TELEPORTER DEBUG] onRightClickBlock -> placeTeleporter");
                placeTeleporter(e.getPlayer(), block);
            }
        }
    }

    @EventHandler
    public void onSpawnerRightClick(PlayerInteractEvent e) {
        if (DEBUG) System.out.println("[TELEPORTER DEBUG] onSpawnerRightClick fired for " + e.getPlayer().getName() + " (cancelled=" + e.isCancelled() + ", hand=" + e.getHand() + ")");
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.isCancelled()) {
            if (DEBUG) System.out.println("[TELEPORTER DEBUG] onSpawnerRightClick -> early return (action or cancelled)");
            return;
        }
        
        if (e.getHand() == org.bukkit.inventory.EquipmentSlot.OFF_HAND) {
            if (DEBUG) System.out.println("[TELEPORTER DEBUG] onSpawnerRightClick -> early return (off-hand)");
            return;
        }
        
        Block block = e.getClickedBlock();
        if (block == null || block.getType() != Material.SPAWNER) {
            if (DEBUG) System.out.println("[TELEPORTER DEBUG] onSpawnerRightClick -> early return (not spawner)");
            return;
        }
        
        if (isCustomItem(e.getPlayer().getInventory().getItemInMainHand())) {
            if (DEBUG) System.out.println("[TELEPORTER DEBUG] onSpawnerRightClick -> early return (holding teleporter item)");
            return;
        }
        
        Location blockLoc = block.getLocation();
        Location blockAbove = blockLoc.clone().add(0, 1, 0);
        
        if (teleporterLocations.containsKey(blockLoc)) {
            if (DEBUG) System.out.println("[TELEPORTER DEBUG] onSpawnerRightClick -> useTeleporter (direct spawner)");
            e.setCancelled(true);
            useTeleporter(e.getPlayer(), blockLoc);
        } else if (teleporterLocations.containsKey(blockAbove)) {
            if (DEBUG) System.out.println("[TELEPORTER DEBUG] onSpawnerRightClick -> useTeleporter (block above)");
            e.setCancelled(true);
            useTeleporter(e.getPlayer(), blockAbove);
        }
    }

    @Override
    protected void onLeftClickBlock(PlayerInteractEvent e) {
        removeTeleporter(e.getPlayer(), e.getClickedBlock().getLocation());
    }

    private void removeAllTeleporters(Player p) {
        if (!playerTeleporters.containsKey(p)) return;
        
        Set<Location> toRemove = new HashSet<>();
        for (Location loc : playerTeleporters.get(p)) {
            if (teleporterLocations.containsKey(loc)) {
                toRemove.add(loc);
            }
        }
        
        for (Location loc : toRemove) {
            if (Commands.isDebugMode()) System.out.println("[TELEPORTER DEBUG] Removing teleporter at " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
            loc.getBlock().setType(Material.AIR);
            teleporterLocations.remove(loc);
        }
        
        playerTeleporters.get(p).clear();
        if (Commands.isDebugMode()) System.out.println("[TELEPORTER DEBUG] Teleporters remaining on map: " + teleporterLocations.size());
    }

    @EventHandler
    @Override
    protected void onDeath(PlayerDeathEvent e) {
        Player p = e.getPlayer();
        if (Commands.isDebugMode()) System.out.println("[TELEPORTER DEBUG] Player " + p.getName() + " died!");
        if (Commands.isDebugMode()) System.out.println("[TELEPORTER DEBUG] Total teleporters on map: " + teleporterLocations.size());
        
        removeAllTeleporters(p);
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        if (Commands.isDebugMode()) System.out.println("[TELEPORTER DEBUG] Player " + p.getName() + " disconnected!");
        if (Commands.isDebugMode()) System.out.println("[TELEPORTER DEBUG] Total teleporters on map: " + teleporterLocations.size());
        
        removeAllTeleporters(p);
    }
}
