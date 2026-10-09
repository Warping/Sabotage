package bubbles.sabotage.plugin.items;

import bubbles.sabotage.plugin.items.customitem.CustomItem;
import bubbles.sabotage.plugin.util.Text;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

// A diamond axe that permanently gains a level of Sharpness (up to the vanilla max) for every
// kill scored while wielding it. Each physical copy tracks its own Sharpness level independently,
// via the enchant stored on that specific ItemStack - recognition as "this custom item" relies on
// the CustomItem persistent-data tag (see CustomItem#isCustomItem), not on the item staying
// identical, so it keeps working as the axe's enchants/lore change.
public class BaccasBounty extends CustomItem {

    private static final int MAX_SHARPNESS = Enchantment.SHARPNESS.getMaxLevel();
    private static final String[] ROMAN_NUMERALS = {"-", "I", "II", "III", "IV", "V"};

    public BaccasBounty() {
        super();

        ItemStack item = new ItemStack(Material.DIAMOND_AXE);
        ItemMeta im = item.getItemMeta();

        im.displayName(Text.of(ChatColor.RED + "Bacca's Bounty"));
        im.lore(Text.of(buildLore(0)));

        item.setItemMeta(im);
        setItem(item);
    }

    private List<String> buildLore(int sharpnessLevel) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GOLD + "Gains a level of Sharpness for every kill!");
        lore.add(ChatColor.GRAY + "Current Bonus: " + ChatColor.RED + "Sharpness " + ROMAN_NUMERALS[sharpnessLevel]);
        return lore;
    }

    @Override
    protected void onKill(PlayerDeathEvent e, Player attacker, Player victim) {
        PlayerInventory inv = attacker.getInventory();

        ItemStack mainHand = inv.getItemInMainHand();
        if (isCustomItem(mainHand)) {
            levelUp(mainHand);
            inv.setItemInMainHand(mainHand);
            return;
        }

        ItemStack offHand = inv.getItemInOffHand();
        if (isCustomItem(offHand)) {
            levelUp(offHand);
            inv.setItemInOffHand(offHand);
        }
    }

    private void levelUp(ItemStack axe) {
        ItemMeta meta = axe.getItemMeta();
        int currentLevel = meta.getEnchantLevel(Enchantment.SHARPNESS);
        if (currentLevel >= MAX_SHARPNESS) {
            return;
        }
        int newLevel = currentLevel + 1;
        meta.addEnchant(Enchantment.SHARPNESS, newLevel, true);
        meta.lore(Text.of(buildLore(newLevel)));
        axe.setItemMeta(meta);
    }
}
