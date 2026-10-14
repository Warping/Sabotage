package bubbles.sabotage.plugin.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.scoreboard.Team;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Bridges legacy '§'-coded strings (built with ChatColor) to Adventure Components
public final class Text {

	private static final Map<@NonNull TextColor, @NonNull ChatColor> LEGACY_COLORS;

	static {
		Map<@NonNull TextColor, @NonNull ChatColor> map = new HashMap<>();
		
		map.put(NamedTextColor.BLACK, ChatColor.BLACK);
		map.put(NamedTextColor.DARK_BLUE, ChatColor.DARK_BLUE);
		map.put(NamedTextColor.DARK_GREEN, ChatColor.DARK_GREEN);
		map.put(NamedTextColor.DARK_AQUA, ChatColor.DARK_AQUA);
		map.put(NamedTextColor.DARK_RED, ChatColor.DARK_RED);
		map.put(NamedTextColor.DARK_PURPLE, ChatColor.DARK_PURPLE);
		map.put(NamedTextColor.GOLD, ChatColor.GOLD);
		map.put(NamedTextColor.GRAY, ChatColor.GRAY);
		map.put(NamedTextColor.DARK_GRAY, ChatColor.DARK_GRAY);
		map.put(NamedTextColor.BLUE, ChatColor.BLUE);
		map.put(NamedTextColor.GREEN, ChatColor.GREEN);
		map.put(NamedTextColor.AQUA, ChatColor.AQUA);
		map.put(NamedTextColor.RED, ChatColor.RED);
		map.put(NamedTextColor.LIGHT_PURPLE, ChatColor.LIGHT_PURPLE);
		map.put(NamedTextColor.YELLOW, ChatColor.YELLOW);
		map.put(NamedTextColor.WHITE, ChatColor.WHITE);
		
		LEGACY_COLORS = Map.copyOf(map); 
	}

	private Text() {}

	public static Component of(String legacy) {
		if (legacy == null) {
			return Component.empty();
		}
		return LegacyComponentSerializer.legacySection().deserialize(legacy);
	}

	public static List<Component> of(List<String> legacyLines) {
		return legacyLines.stream().map(Text::of).collect(Collectors.toList());
	}

	public static String plain(Component component) {
		if (component == null) {
			return "";
		}
		return PlainTextComponentSerializer.plainText().serialize(component);
	}

	public static ChatColor chatColor(Team team) {
		return team.hasColor() ? chatColor(team.color()) : ChatColor.RESET;
	}

	public static ChatColor chatColor(TextColor color) {
		ChatColor legacy = LEGACY_COLORS.get(color);
		return legacy != null ? legacy : ChatColor.of(color.asHexString());
	}

}
