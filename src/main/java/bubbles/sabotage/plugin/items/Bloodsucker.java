package bubbles.sabotage.plugin.items;

import bubbles.sabotage.plugin.items.customitem.CustomItem;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

public class Bloodsucker extends CustomItem {

    final double MAX_HEALTH = 40.0;

    public Bloodsucker() {

        super();
        //Set variable item by changing Material.BLAZE_ROD to some other Material

        ItemStack item = new ItemStack(Material.IRON_SWORD);
        ItemMeta im = item.getItemMeta();

        // Change the item meta and item details below
        im.addEnchant(Enchantment.DAMAGE_ALL, 1, true);
        im.setDisplayName(ChatColor.LIGHT_PURPLE + "Blood Infused Sword");

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GOLD + "Killing a player adds 2 hearts to your maximum health!");
        lore.add(ChatColor.GOLD + "The amount of redstone in your inventory is how many hearts you have!");
        im.setLore(lore);


        // End of changes

        item.setItemMeta(im);
        setItem(item);

    }

    private void updateHealth(Player p) {
        int redstoneAmount = 0;
        for (ItemStack item : p.getInventory().getContents()) {
            if (item != null && item.getType() == Material.REDSTONE) {
                redstoneAmount += item.getAmount();
            }
        }
        if (redstoneAmount == 0) p.setMaxHealth(20);
        else p.setMaxHealth(Math.min(redstoneAmount * 2.0, MAX_HEALTH));

    }

    @Override
    protected void onKill(PlayerDeathEvent e, Player attacker, Player victim) {
        if (contains(attacker, getItem(), 1)) {
            if (!contains(attacker, new ItemStack(Material.REDSTONE), (int)(MAX_HEALTH / 2))) give(attacker, new ItemStack(Material.REDSTONE), 2);
            updateHealth(attacker);
            attacker.addPotionEffect(PotionEffectType.REGENERATION.createEffect(20 * 4, 1));
        }
    }

    @EventHandler
    private void onTeleport(PlayerTeleportEvent e) {
        if (e.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
            return;
        }
        Player p = e.getPlayer();
        if (contains(p, getItem(), 1)) {
            updateHealth(p);
        }
    }


}
