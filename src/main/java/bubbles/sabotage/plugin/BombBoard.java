package bubbles.sabotage.plugin;

import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import bubbles.sabotage.plugin.counter.Counter;
import bubbles.sabotage.plugin.game.Game;
import bubbles.sabotage.plugin.groups.SabTeams;
import net.md_5.bungee.api.ChatColor;

public class BombBoard {
	
	private static Main plugin = Main.getPlugin(Main.class);
	
	private static Game game;
	private static Scoreboard board;
	private static String boardName = ChatColor.RED + "" + ChatColor.BOLD + "Bomb Timer";
	private static Objective objBomb;
	private static Counter count = new Counter(10L);
	private static int maxLines = 15;
	private static int lines = 15;
	
	
	public static void setup(Game _game) {
		game = _game;
		board = plugin.getScoreboard();
		objBomb = board.registerNewObjective("bombboard", "dummy", boardName);
		objBomb.setDisplaySlot(DisplaySlot.SIDEBAR);
	}
	
	public static void display() {
		lines = maxLines;
		objBomb.unregister();
		objBomb = board.registerNewObjective("bombboard", "dummy", boardName);
		objBomb.setDisplaySlot(DisplaySlot.SIDEBAR);
		int bomb_index = 1;
		String tag;
		tag = ChatColor.RED + "" + ChatColor.BOLD + "=================";
		addLine(tag);
		for (Bomb bomb : game.getBombs()) {
			int nameLength;
			String name;
			if (bomb.getOwner() != null) {
				name = SabTeams.getDisplayName(bomb.getOwner()) + " Bomb";
				nameLength = bomb.getOwner().getName().length() + 5;
			} else {
				name = ChatColor.WHITE + "All " + ChatColor.GOLD + "Bomb ";
				nameLength = 8;
				bomb_index++;
			}
			String suffix;
			int spaces;
			if (bomb.isExploded()) {
				suffix = ChatColor.GRAY + "Exploded";
				spaces = 25 - 3 - nameLength - 8;
			} else if (bomb.isArmed()) {
				String timer;
				if (bomb.getOwner() != null) {
					timer = Integer.toString(bomb.getTeamTimer(bomb.getOwner()));
				} else {
					timer = Integer.toString(bomb.getTeamTimer(bomb.getArmedBy()));
				}
				suffix = " " + SabTeams.getDisplayName(bomb.getArmedBy()) + " " + ChatColor.AQUA + timer;
				spaces = 25 - 3 - nameLength - bomb.getArmedBy().getName().length() - timer.length();
			} else {
				suffix = ChatColor.AQUA + "Unarmed";
				spaces = 25 - 3 - nameLength - 7;
			}
			tag = name + " " + space(spaces) + suffix;
			addLine(tag);
			int lowestTime = 120;
			if (bomb.getOwner() == null) {
				for (Team team : bomb.getSabTeams().getTeams()) {
					if (bomb.getTeamTimer(team) < lowestTime)
						lowestTime = bomb.getTeamTimer(team);
				}
				String prefix;
				for (Team team : bomb.getSabTeams().getTeams()) {
					prefix = "    ";
					if (bomb.getTeamTimer(team) == lowestTime)
						prefix = " ★ ";
					int size = 25 - 3 - team.getName().length() - Integer.toString(bomb.getTeamTimer(team)).length();
					if (team.getName().length() < 5) {
						size++;
					}
					tag = prefix + SabTeams.getDisplayName(team) + space(size) + ChatColor.YELLOW + bomb.getTeamTimer(team);
					addLine(tag);
				}
			} else {
				String prefix = "    ";
				int size = 25 - 3 - bomb.getOwner().getName().length() - Integer.toString(bomb.getTeamTimer(bomb.getOwner())).length();
				if (bomb.getOwner().getName().length() < 5) {
					size++;
				}
				tag = prefix + SabTeams.getDisplayName(bomb.getOwner()) + space(size) + ChatColor.YELLOW + bomb.getTeamTimer(bomb.getOwner());
				addLine(tag);
			}
		}
		tag = ChatColor.RED + "" + ChatColor.BOLD + "=================";
		addLine(tag);
	}
	
	private static String space(int size) {
		String ret = "";
		for (int i = 0; i < size; i++) {
			ret += " ";
		}
		return ret;
	}
	
	public static void addLine(String str) {
		if (lines < 1) {
			return;
		}
		if (lines > 0 && lines < 10) {
			str += ChatColor.getByChar(Integer.toString(lines).toCharArray()[0]);
		} if (lines == 10) {
			str += ChatColor.getByChar('a');
		} if (lines == 11) {
			str += ChatColor.getByChar('b');
		} if (lines == 12) {
			str += ChatColor.getByChar('c');
		} if (lines == 13) {
			str += ChatColor.getByChar('d');
		} if (lines == 14) {
			str += ChatColor.getByChar('e');
		} if (lines == 15) {
			str += ChatColor.getByChar('f');
		}
		Score score = objBomb.getScore(str);
		score.setScore(lines);
		lines--;
	}
	
	public static void update() {
		count = new Counter(10L) {
			public void run() {
				display();
			}
		};
	}
	
	public static void stop() {
		count.cancel();
	}
	
	

}
