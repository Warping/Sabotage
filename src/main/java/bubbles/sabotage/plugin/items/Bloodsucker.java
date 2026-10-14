package bubbles.sabotage.plugin.items;

import bubbles.sabotage.plugin.SabotageBootstrap;
import bubbles.sabotage.plugin.items.customitem.CustomItem;
import bubbles.sabotage.plugin.util.Health;
import bubbles.sabotage.plugin.util.Text;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

public class Bloodsucker extends CustomItem {

    // 8 hearts at Health 0 (the kit's starting hearts), plus 2 hearts (4 HP) per Health level.
    private static final double BASE_HEALTH = 16.0;
    private static final double HEALTH_PER_LEVEL = 4.0;

    public Bloodsucker() {

        super();
        //Set variable item by changing Material.BLAZE_ROD to some other Material

        ItemStack item = new ItemStack(Material.IRON_SWORD);
        ItemMeta im = item.getItemMeta();
        if (im == null) {
            throw new IllegalStateException("Could not create item meta for " + Material.IRON_SWORD);
        }

        // Change the item meta and item details below
        im.addEnchant(Enchantment.SHARPNESS, 1, true);
        im.displayName(Text.of(ChatColor.LIGHT_PURPLE + "Blood Infused Sword"));

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GOLD + "Killing a player adds 2 hearts to your maximum health!");
        lore.add(ChatColor.GOLD + "Hearts gained are stored in its Health enchantment (max Health VI)!");
        im.lore(Text.of(lore));


        // End of changes

        item.setItemMeta(im);
        setItem(item);

    }

    private static Enchantment healthEnchantment() {
        NamespacedKey key = NamespacedKey.fromString(SabotageBootstrap.HEALTH_KEY);
        if (key == null) {
            throw new IllegalStateException("Invalid enchantment key: " + SabotageBootstrap.HEALTH_KEY);
        }
        Enchantment enchantment = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT).get(key);
        if (enchantment == null) {
            throw new IllegalStateException("Enchantment is not registered: " + SabotageBootstrap.HEALTH_KEY);
        }
        return enchantment;
    }

    // Returns the inventory slot of this custom sword, or -1 if the player doesn't have one.
    private int findSwordSlot(PlayerInventory inv) {
        ItemStack[] contents = inv.getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            if (isCustomItem(contents[slot])) return slot;
        }
        return -1;
    }

    private void updateHealth(Player p) {
        int slot = findSwordSlot(p.getInventory());
        if (slot == -1) return;
        ItemStack sword = p.getInventory().getItem(slot);
        ItemMeta meta = sword == null ? null : sword.getItemMeta();
        if (meta == null) return;
        int level = meta.getEnchantLevel(healthEnchantment());
        Health.setMax(p, BASE_HEALTH + HEALTH_PER_LEVEL * level);
    }

    @Override
    protected void onKill(PlayerDeathEvent e, Player attacker, Player victim) {
        PlayerInventory inv = attacker.getInventory();
        int slot = findSwordSlot(inv);
        if (slot == -1) return;

        ItemStack sword = inv.getItem(slot);
        ItemMeta meta = sword == null ? null : sword.getItemMeta();
        if (sword != null && meta != null) {
            Enchantment health = healthEnchantment();
            int level = meta.getEnchantLevel(health);
            if (level < SabotageBootstrap.HEALTH_MAX_LEVEL) {
                meta.addEnchant(health, level + 1, true);
                sword.setItemMeta(meta);
                inv.setItem(slot, sword);
            }
        }

        updateHealth(attacker);
        attacker.addPotionEffect(PotionEffectType.REGENERATION.createEffect(20 * 4, 1));
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
