package bubbles.sabotage.plugin.items;

import bubbles.sabotage.plugin.counter.Counter;
import bubbles.sabotage.plugin.items.customitem.CustomItem;
import bubbles.sabotage.plugin.util.Text;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.*;
import java.util.logging.Level;
import static bubbles.sabotage.plugin.Main.applog;

public class RewindClock extends CustomItem {

    private final HashMap<Player, Queue<Location>> recentPos = new HashMap<>();
    private final int MAX_REWIND_TIME = 20;

    public RewindClock() {
        super();

        //Set variable item by changing Material.BLAZE_ROD to some other Material

        ItemStack item = new ItemStack(Material.CLOCK);
        ItemMeta im = item.getItemMeta();

        // Change the item meta and item details below

        im.displayName(Text.of(ChatColor.AQUA + "Rewind Clock"));

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GOLD + "Goes back in time by Right Clicking");
        im.lore(Text.of(lore));

        // End of changes

        item.setItemMeta(im);
        setItem(item);

        //Extra Info
        new Counter(20L) {
            @Override
            public void run() {
                for (Player p : getGame().getWorld().getPlayers()) {
                    if (!contains(p, getItem(), 1)) {
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
                        getPlugin().getServer().getScheduler().runTaskLater(getPlugin(), () -> {
                            p.sendMessage(ChatColor.GREEN + "You can now Rewind!");
                            @NonNull Sound blockNoteBlockHarp = Sound.BLOCK_NOTE_BLOCK_HARP;
                            if (blockNoteBlockHarp != null) {
                                p.getWorld().playSound(p, blockNoteBlockHarp, 0.5F, 2);
                            } else {
                                applog.log(Level.WARNING, "Sound BLOCK_NOTE_BLOCK_HARP is null!");
                            }
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
            @NonNull Sound blockNoteBlockBass = Sound.BLOCK_NOTE_BLOCK_BASS;
            if (blockNoteBlockBass != null) {
                player.getWorld().playSound(player, blockNoteBlockBass, 0.5F, 1);
            } else {
                applog.log(Level.WARNING, "Sound BLOCK_NOTE_BLOCK_BASS is null!");
            }
            return;
        }
        player.getWorld().spawnParticle(Particle.CRIT, player.getLocation(), 100, 0.1, 0.5, 0.1, 1);
        @NonNull Sound entityEndermanTeleport = Sound.ENTITY_ENDERMAN_TELEPORT;
        if (entityEndermanTeleport != null) {
            player.getWorld().playSound(player,entityEndermanTeleport, 0.5F, 1);
        } else {
            applog.log(Level.WARNING, "Sound ENTITY_ENDERMAN_TELEPORT is null!");
        }
        player.teleport(recentPos.get(player).poll());
        @NonNull Sound entityEndermanTeleport2 = Sound.ENTITY_ENDERMAN_TELEPORT;
        if (entityEndermanTeleport2 != null) {
            player.getWorld().playSound(player,entityEndermanTeleport2, 0.5F, 1);
        } else {
            applog.log(Level.WARNING, "Sound ENTITY_ENDERMAN_TELEPORT is null!");
        }
        player.getWorld().spawnParticle(Particle.CRIT, player.getLocation(), 100, 0.1, 0.5, 0.1, 1);
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
