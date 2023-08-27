package bubbles.sabotage.plugin.items;

import bubbles.sabotage.plugin.GUI;
import bubbles.sabotage.plugin.items.customitem.CustomItem;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class Landmine extends CustomItem {

    private final float POWER = 1.2F;
    private final long FUSE_DELAY = 25L;
    private final long RELOAD_DELAY = 100L;
    HashMap<Location, Player> landmineLocations = new HashMap<>();
    HashMap<Player, Material> landmineSkins = new HashMap<>();
    GUI landMineSkinGUI = new GUI(getPlugin(), "Mine Appearance");

    public Landmine() {
        super();

        //Set variable item by changing Material.BLAZE_ROD to some other Material

        ItemStack item = new ItemStack(Material.SHEARS);
        ItemMeta im = item.getItemMeta();

        // Change the item meta and item details below

        im.setDisplayName(ChatColor.RED + "Landmine Arming Tool");

        List<String> lore = new ArrayList<>();
        lore.add(org.bukkit.ChatColor.GOLD + "Right Click to Place Mine.");
        lore.add(org.bukkit.ChatColor.GOLD + "Sneak Right Click to Change Mine Appearance.");
        lore.add(org.bukkit.ChatColor.GOLD + "Left Click to Pick Up Mine.");
        im.setLore(lore);
        item.setItemMeta(im);
        setItem(item);

        loadMineSkins();
    }

    private void loadMineSkins() {
        landMineSkinGUI.addSlot(new ItemStack(Material.ACACIA_PRESSURE_PLATE),"Acacia");
        landMineSkinGUI.addSlot(new ItemStack(Material.BIRCH_PRESSURE_PLATE),"Birch");
        landMineSkinGUI.addSlot(new ItemStack(Material.BAMBOO_PRESSURE_PLATE),"Bamboo");
        landMineSkinGUI.addSlot(new ItemStack(Material.CRIMSON_PRESSURE_PLATE),"Crimson");
        landMineSkinGUI.addSlot(new ItemStack(Material.CHERRY_PRESSURE_PLATE),"Cherry");
        landMineSkinGUI.addSlot(new ItemStack(Material.DARK_OAK_PRESSURE_PLATE),"Dark Oak");
        landMineSkinGUI.addSlot(new ItemStack(Material.JUNGLE_PRESSURE_PLATE),"Jungle");
        landMineSkinGUI.addSlot(new ItemStack(Material.OAK_PRESSURE_PLATE),"Oak");
        landMineSkinGUI.addSlot(new ItemStack(Material.MANGROVE_PRESSURE_PLATE),"Mangrove");
        landMineSkinGUI.addSlot(new ItemStack(Material.POLISHED_BLACKSTONE_PRESSURE_PLATE),"Polished Blackstone");
        landMineSkinGUI.addSlot(new ItemStack(Material.SPRUCE_PRESSURE_PLATE),"Spruce");
        landMineSkinGUI.addSlot(new ItemStack(Material.WARPED_PRESSURE_PLATE),"Warped");
        landMineSkinGUI.addSlot(new ItemStack(Material.HEAVY_WEIGHTED_PRESSURE_PLATE),"Heavy Weighted");
        landMineSkinGUI.addSlot(new ItemStack(Material.LIGHT_WEIGHTED_PRESSURE_PLATE),"Light Weighted");
        landMineSkinGUI.addSlot(new ItemStack(Material.STONE_PRESSURE_PLATE),"Stone");

    }

    private void placeLandmine(Player p, Block block) {
        Location loc = block.getLocation();
        if (!contains(p, new ItemStack(Material.HEAVY_WEIGHTED_PRESSURE_PLATE), 1)) {
            p.sendMessage(ChatColor.RED + "You have no landmines.");
            return;
        }
        if (loc.clone().add(0, 1, 0).getBlock().getType() != Material.AIR ||
            loc.clone().add(0, 2, 0).getBlock().getType() != Material.AIR) {
            return;
        }
        if (!block.getType().isOccluding()) {
            return;
        }
        consume(p, new ItemStack(Material.HEAVY_WEIGHTED_PRESSURE_PLATE), 1);
        Location landmineLoc = loc.clone().add(0, 1, 0);
        landmineLocations.put(landmineLoc, p);
        if (landmineSkins.containsKey(p)) {
            landmineLoc.getBlock().setType(landmineSkins.get(p));
        } else {
            landmineSkins.put(p, Material.HEAVY_WEIGHTED_PRESSURE_PLATE);
            landmineLoc.getBlock().setType(Material.HEAVY_WEIGHTED_PRESSURE_PLATE);
        }
        p.sendMessage(ChatColor.GREEN + "Landmine placed.");
        p.getWorld().playSound(landmineLoc, Sound.BLOCK_BAMBOO_WOOD_PRESSURE_PLATE_CLICK_ON, 10.0F, 0.8F);

    }

    private void removeLandmine(Player p, Location loc) {
        if (!landmineLocations.containsKey(loc)) return;
        Player owner = landmineLocations.get(loc);
        if (!owner.equals(p)) return;
        loc.getBlock().setType(Material.AIR);
        landmineLocations.remove(loc);
        give(p, new ItemStack(Material.HEAVY_WEIGHTED_PRESSURE_PLATE), 1);
        p.getWorld().playSound(loc, Sound.BLOCK_BAMBOO_WOOD_PRESSURE_PLATE_CLICK_OFF, 10.0F, 0.7F);
    }

    private boolean changeLandmineSkin(Player p) {
        if (p.isSneaking()) {
            landMineSkinGUI.open(p);
            return true;
        }
        return false;
    }

    @EventHandler
    public void onLandMineStep(PlayerInteractEvent e) {
        if (!e.getAction().equals(Action.PHYSICAL)) {
            return;
        }
        Player victim = e.getPlayer();
        Location loc = e.getClickedBlock().getLocation();
        if (landmineLocations.containsKey(loc)) {
            Player attacker = landmineLocations.get(loc);
            if (getGame().getTeams().onSameTeam(attacker, victim)) {
                e.setCancelled(true);
                return;
            }
            attacker.sendMessage(ChatColor.GREEN + "Your landmine was triggered by " + victim.getName() + "!");
            attacker.getWorld().playSound(attacker, Sound.BLOCK_NOTE_BLOCK_PLING, 10.0F, 1F);
            landmineLocations.remove(loc);
            getPlugin().getServer().getScheduler().scheduleSyncDelayedTask(getPlugin(), () -> {
                e.getClickedBlock().setType(Material.AIR);
                victim.getWorld().playSound(loc, Sound.ENTITY_CREEPER_PRIMED, 10F, 1.2F);
            }, 1);
            getPlugin().getServer().getScheduler().scheduleSyncDelayedTask(getPlugin(), () ->
                attacker.getWorld().createExplosion(loc, POWER, false, false), FUSE_DELAY);
            getPlugin().getServer().getScheduler().scheduleSyncDelayedTask(getPlugin(), () ->
                give(attacker, new ItemStack(Material.HEAVY_WEIGHTED_PRESSURE_PLATE), 1), RELOAD_DELAY);
        }

    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        if (e.getCurrentItem()!=null) {
            if (e.getInventory().equals(landMineSkinGUI.getInv())) {
                e.setCancelled(true);
                Material mat = e.getCurrentItem().getType();
                landmineSkins.put(p, mat);
                p.sendMessage(ChatColor.GREEN + "Landmine skin set to " + mat.name() + ".");
                p.getWorld().playSound(p, Sound.BLOCK_NOTE_BLOCK_PLING, 10.0F, 1F);
                landMineSkinGUI.closeInventory(e.getWhoClicked());
            }
        }
    }

    @Override
    protected void onRightClickBlock(PlayerInteractEvent e, boolean mainHand) {
        if (!changeLandmineSkin(e.getPlayer())) placeLandmine(e.getPlayer(), e.getClickedBlock());
    }

    @Override
    protected void onRightClickAir(PlayerInteractEvent e, boolean mainHand) {
        if (!changeLandmineSkin(e.getPlayer())) return;
    }

    @Override
    protected void onRightClickPlayer(PlayerInteractAtEntityEvent e, boolean mainHand) {
        if (!changeLandmineSkin(e.getPlayer())) return;
    }

    @Override
    protected void onLeftClickBlock(PlayerInteractEvent e) {
        removeLandmine(e.getPlayer(), e.getClickedBlock().getLocation());
    }

    @Override
    protected void onDeath(PlayerDeathEvent e) {
        Player p = e.getPlayer();
        Set<Location> toRemove = new HashSet<>();
        for (Location loc : landmineLocations.keySet()) {
            if (landmineLocations.get(loc).equals(p)) {
                loc.getBlock().setType(Material.AIR);
                toRemove.add(loc);
            }
        }
        for (Location loc : toRemove) {
            landmineLocations.remove(loc);
        }
    }

    @Override
    public void stop() {
        for (Location loc : landmineLocations.keySet()) {
            loc.getBlock().setType(Material.AIR);
        }
        landmineLocations.clear();
    }
}
