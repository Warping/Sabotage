package bubbles.sabotage.plugin.items;

import bubbles.sabotage.plugin.items.customitem.CustomItem;
import bubbles.sabotage.plugin.util.Text;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

// An iron sword that inflicts Poison 1 on any player it hits for 5 seconds
public class Poisoner extends CustomItem {

	private static final int POISON_DURATION = 5 * 20; // 5 seconds in ticks
	private static final int POISON_LEVEL = 1;

	public Poisoner() {
		super();

		ItemStack item = new ItemStack(Material.IRON_SWORD);
		ItemMeta im = item.getItemMeta();

		im.displayName(Text.of(ChatColor.GREEN + "Poisoner"));
		im.lore(Text.of(buildLore()));

		item.setItemMeta(im);
		setItem(item);
	}

	private List<String> buildLore() {
		List<String> lore = new ArrayList<>();
		lore.add(ChatColor.DARK_GREEN + "Inflicts Poison I on hit!");
		lore.add(ChatColor.GRAY + "Duration: 5 seconds");
		return lore;
	}

	@EventHandler
	public void onPoisonerAttack(EntityDamageByEntityEvent e) {
		if (!(e.getEntity() instanceof Player) || !(e.getDamager() instanceof Player)) {
			return;
		}
		
		Player victim = (Player) e.getEntity();
		Player attacker = (Player) e.getDamager();
		
		ItemStack mainHand = attacker.getInventory().getItemInMainHand();
		ItemStack offHand = attacker.getInventory().getItemInOffHand();
		
		if (!isCustomItem(mainHand) && !isCustomItem(offHand)) {
			return;
		}
		
		if (getGame().getTeams().onSameTeam(attacker, victim)) {
			return;
		}
		
		victim.addPotionEffect(new PotionEffect(PotionEffectType.POISON, POISON_DURATION, POISON_LEVEL - 1));
	}
}
