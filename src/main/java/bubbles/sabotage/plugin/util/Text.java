package bubbles.sabotage.plugin.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.List;
import java.util.stream.Collectors;

// Bridges legacy '§'-coded strings (built with ChatColor) to Adventure Components
public final class Text {

	private Text() {}

	public static Component of(String legacy) {
		return LegacyComponentSerializer.legacySection().deserialize(legacy);
	}

	public static List<Component> of(List<String> legacyLines) {
		return legacyLines.stream().map(Text::of).collect(Collectors.toList());
	}

}
