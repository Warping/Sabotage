package bubbles.sabotage.plugin.util;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;

public final class Health {

	private Health() {}

	public static void setMax(LivingEntity entity, double maxHealth) {
		AttributeInstance attribute = entity.getAttribute(Attribute.MAX_HEALTH);
		if (attribute == null) return;
		attribute.setBaseValue(maxHealth);
		if (entity.getHealth() > maxHealth) entity.setHealth(maxHealth);
	}

	public static double getMax(LivingEntity entity) {
		AttributeInstance attribute = entity.getAttribute(Attribute.MAX_HEALTH);
		return attribute == null ? 0 : attribute.getValue();
	}

}
