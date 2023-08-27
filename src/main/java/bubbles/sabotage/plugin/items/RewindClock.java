package bubbles.sabotage.plugin.items;

import bubbles.sabotage.plugin.counter.Counter;
import bubbles.sabotage.plugin.items.customitem.CustomItem;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class RewindClock extends CustomItem {

    private final HashMap<Player, Queue<Location>> recentPos = new HashMap<>();
    private final int MAX_REWIND_TIME = 12;

    public RewindClock() {
        super();

        //Set variable item by changing Material.BLAZE_ROD to some other Material

        ItemStack item = new ItemStack(Material.CLOCK);
        ItemMeta im = item.getItemMeta();

        // Change the item meta and item details below

        im.setDisplayName(ChatColor.AQUA + "Rewind Clock");

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GOLD + "Goes back in time by Right Clicking");
        im.setLore(lore);

        // End of changes

        item.setItemMeta(im);
        setItem(item);

        //Extra Info
        new Counter(20L) {
            @Override
            public void run() {
                for (Player p : getGame().getWorld().getPlayers()) {
                    if (!contains(p, item, 1)) {
                        recentPos.remove(p);
                        continue;
                    }
                    recentPos.putIfAbsent(p, new LinkedList<>());
                    Queue<Location> queue = recentPos.get(p);
                    if (queue.size() >= MAX_REWIND_TIME) {
                        queue.poll();
                    }
                    queue.add(p.getLocation());
                    recentPos.put(p,queue);
                    if (queue.size() == MAX_REWIND_TIME - 1) {
                        getPlugin().getServer().getScheduler().scheduleSyncDelayedTask(getPlugin(), () -> {
                            p.sendMessage(ChatColor.GREEN + "You can now Rewind!");
                            p.getWorld().playSound(p, Sound.BLOCK_NOTE_BLOCK_HARP, 10, 2);
                        }, 20L);
                    }
                }
            }
        };
    }

    private void rewind(Player player) {
        if(recentPos.get(player)==null) return;
        if(recentPos.get(player).size() < MAX_REWIND_TIME) {
            player.sendMessage(ChatColor.RED + "Cannot go back this early! Wait " + (MAX_REWIND_TIME - recentPos.get(player).size()) + " seconds");
            player.getWorld().playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 10, 1);
            return;
        }
        player.getWorld().spawnParticle(Particle.SPELL_INSTANT, player.getLocation(), 100, 0.1, 0.5, 0.1, 1);
        player.getWorld().playSound(player,Sound.ENTITY_ENDERMAN_TELEPORT, 10, 1);
        player.teleport(recentPos.get(player).poll());
        player.getWorld().playSound(player,Sound.ENTITY_ENDERMAN_TELEPORT, 10, 1);
        player.getWorld().spawnParticle(Particle.SPELL_INSTANT, player.getLocation(), 100, 0.1, 0.5, 0.1, 1);
        recentPos.remove(player);
    }

    @Override
    protected void onRightClickPlayer(PlayerInteractAtEntityEvent e, boolean mainHand) {
        rewind(e.getPlayer());
    }

    @Override
    protected void onRightClickAir(PlayerInteractEvent e, boolean mainHand) {
        rewind(e.getPlayer());
    }

    @Override
    protected void onRightClickBlock(PlayerInteractEvent e, boolean mainHand) {
        rewind(e.getPlayer());
    }

    @Override
    public void stop() {
        recentPos.clear();
    }
}
