package bubbles.sabotage.plugin;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.data.EnchantmentRegistryEntry;
import io.papermc.paper.registry.event.RegistryEvents;
import io.papermc.paper.registry.keys.tags.ItemTypeTagKeys;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.EquipmentSlotGroup;

// Runs before Main#onEnable so custom enchantments exist in the registry when items are built.
public class SabotageBootstrap implements PluginBootstrap {

    public static final String HEALTH_KEY = "sabotage:health";
    public static final int HEALTH_MAX_LEVEL = 6;
    public static final String FROST_BITE_KEY = "sabotage:frost_bite";

    @Override
    public void bootstrap(BootstrapContext context) {
        context.getLifecycleManager().registerEventHandler(RegistryEvents.ENCHANTMENT.compose(), event -> {
            event.registry().register(TypedKey.create(RegistryKey.ENCHANTMENT, HEALTH_KEY), b -> b
                    .description(Component.text("Health"))
                    .supportedItems(event.getOrCreateTag(ItemTypeTagKeys.SWORDS))
                    .weight(1)
                    .maxLevel(HEALTH_MAX_LEVEL)
                    .minimumCost(EnchantmentRegistryEntry.EnchantmentCost.of(1, 0))
                    .maximumCost(EnchantmentRegistryEntry.EnchantmentCost.of(1, 0))
                    .anvilCost(1)
                    .activeSlots(EquipmentSlotGroup.ANY));
            event.registry().register(TypedKey.create(RegistryKey.ENCHANTMENT, FROST_BITE_KEY), b -> b
                    .description(Component.text("Frost Bite"))
                    .supportedItems(event.getOrCreateTag(ItemTypeTagKeys.SWORDS))
                    .weight(1)
                    .maxLevel(1)
                    .minimumCost(EnchantmentRegistryEntry.EnchantmentCost.of(1, 0))
                    .maximumCost(EnchantmentRegistryEntry.EnchantmentCost.of(1, 0))
                    .anvilCost(1)
                    .activeSlots(EquipmentSlotGroup.ANY));
        });
    }
}
