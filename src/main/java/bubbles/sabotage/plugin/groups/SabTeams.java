package bubbles.sabotage.plugin.groups;

import bubbles.sabotage.plugin.GUI;
import bubbles.sabotage.plugin.Main;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class SabTeams {
	
	private Main plugin = Main.getPlugin(Main.class);
	private ArrayList<Team> teams = new ArrayList<Team>();
	private HashMap<Team, Location> spawnLoc = new HashMap<Team, Location>();
	private GUI teamGUI;
	private Scoreboard board;
	
	public SabTeams(String[] teamNames, ArrayList<Location> spawns) {
		board = plugin.getScoreboard();
		ItemStack[] items = new ItemStack[teamNames.length];
		String[] cmds = new String[teamNames.length];
		for (int i = 0; i < teamNames.length; i++) {
			if (teamNames[i]!=null) {
				addTeam(teamNames[i], spawns.get(i));
			}
		}
		teamGUI = new GUI(plugin, ChatColor.GREEN + "Teams", items, cmds);
	}
	
	public SabTeams() {
		board = plugin.getScoreboard();
		teamGUI = new GUI(plugin, "Teams");
	}
	
	public boolean addTeam(String teamName, Location spawn) {
		for (Team t : teams) {
			if (t.getName().equalsIgnoreCase(teamName)) {
				return false;
			}
		}
		teamGUI.addSlot();
		Team team = board.registerNewTeam(teamName);
		teams.add(team);
		updateOptions(team);
		ItemStack is = convertItem(teamName);
		ItemMeta im = is.getItemMeta();
		im.setDisplayName(getDisplayName(team) + team.getColor() + " Team");
		is.setItemMeta(im);
		teamGUI.addSlot(is, "/team " + teamName.toLowerCase());
		spawnLoc.put(team, spawn);
		return true;
	}
	
	private void updateOptions(Team team) {
		team.setCanSeeFriendlyInvisibles(true);
		if (team.getName().toLowerCase().contains("red")) {
			team.setColor(ChatColor.RED);
		} else if (team.getName().toLowerCase().contains("blue")) {
			team.setColor(ChatColor.BLUE);
		} else if (team.getName().toLowerCase().contains("green")) {
			team.setColor(ChatColor.DARK_GREEN);
		} else if (team.getName().toLowerCase().contains("purple")) {
			team.setColor(ChatColor.DARK_PURPLE);
		} else if (team.getName().toLowerCase().contains("pink")) {
			team.setColor(ChatColor.LIGHT_PURPLE);
		} else if (team.getName().toLowerCase().contains("orange")) {
			team.setColor(ChatColor.GOLD);
		} else if (team.getName().toLowerCase().contains("aqua")) {
			team.setColor(ChatColor.AQUA);
		}
		team.setAllowFriendlyFire(false);
	}
	
	public Main getPlugin() {
		return plugin;
	}


	private static ItemStack convertItem(String name) {
		ItemStack item = new ItemStack(Material.AIR);
		if (name.toLowerCase().contains("red")) {
			item = new ItemStack(Material.RED_WOOL);
		} else if (name.toLowerCase().contains("blue")) {
			item = new ItemStack(Material.BLUE_WOOL);
		} else if (name.toLowerCase().contains("green")) {
			item = new ItemStack(Material.LIME_WOOL);
		} else if (name.toLowerCase().contains("purple")) {
			item = new ItemStack(Material.PURPLE_WOOL);
		} else if (name.toLowerCase().contains("pink")) {
			item = new ItemStack(Material.PINK_WOOL);
		} else if (name.toLowerCase().contains("orange")) {
			item = new ItemStack(Material.ORANGE_WOOL);
		} else if (name.toLowerCase().contains("aqua")) {
			item = new ItemStack(Material.CYAN_WOOL);
		}
		return item;
	}
	
	public static String getDisplayName(Team team) {
		String name = team.getName();
		String second = name.substring(1, name.length());
		String first = name.substring(0, 1).toUpperCase();
		name = first + second;
		return team.getColor() + name + ChatColor.GOLD;
	}

	public static Color getDyeColor(Team team) {
		switch (team.getColor()) {
			case BLUE:
				return Color.fromRGB(0x5555FF);
			case RED:
				return Color.fromRGB(0xFF5555);
			case DARK_GREEN:
				return Color.fromRGB(0x55FF55);
			case DARK_PURPLE:
				return Color.fromRGB(0xAA00AA);
			case GOLD:
				return Color.fromRGB(0xFFAA00);
			case LIGHT_PURPLE:
				return Color.fromRGB(0xFF55FF);
			case AQUA:
				return Color.fromRGB(0x55FFFF);
			default:
				break;
		}

		return null;
	}
	
	public Material getBlock(Team team) {
		return convertItem(team.getName().toString()).getType();
	}
	
	public GUI getTeamGUI() {
		return teamGUI;
	}

	public ArrayList<Team> getTeams() {
		return teams;
	}
	
	public ArrayList<String> getTeamNames() {
		ArrayList<String> names = new ArrayList<String>();
		for (Team team : teams) {
			names.add(team.getName());
		}
		return names;
	}
	
	public ArrayList<Location> getSpawnArray() {
		ArrayList<Location> locs = new ArrayList<Location>();
		for (Team team : teams) {
			locs.add(spawnLoc.get(team));
		}
		return locs;
	}
	
	public HashMap<Team, Location> getSpawnHash() {
		return spawnLoc;
	}

	public Scoreboard getScoreboard() {
		return board;
	}
	
	public void unregisterAll() {
		for (Team team : teams) {
			team.unregister();
		}
	}
	
	public Team getTeam(String teamName) {
		for (Team team : teams) {
			if (team.getName().equals(teamName)) {
				return team;
			}
		}
		return null;
	}
	
	public Team getTeamOfPlayer(String player) {
		for (Team t : teams) {
			if (t.getEntries().contains(player)) {
				return t;
			}
		}
		return null;
	}
	
	public Team getTeamOfPlayer(Player player) {
		for (Team t : teams) {
			if (t.getEntries().contains(player.getName())) {
				return t;
			}
		}
		return null;
	}
	
	public Set<String> getTeamPlayers() {
		Set<String> teamPlayers = new HashSet<String>();
		for (Team t : teams) {
			teamPlayers.addAll(t.getEntries());
		}
		return teamPlayers;
	}
	
	public void removePlayer(Player p) {
		for (Team t : teams) {
			t.removeEntry(p.getName());
		}
	}
	
	public void removeOfflinePlayer(String p) {
		for (Team t : teams) {
			t.removeEntry(p);
		}
	}
	
	public void addPlayer(Player p, String team) {
		if (getTeam(team)!=null) {
			removePlayer(p);
			getTeam(team).addEntry(p.getName());
			plugin.updateScoreboard();
		}
	}
	
	public void setSpawnLoc(Team team, Location loc) {
		this.spawnLoc.put(team, loc);
	}
	
	public Location getSpawnLoc(Team team) {
		return spawnLoc.get(team);
	}
	
	public Location getSpawnLoc(Player player) {
		if (getTeamOfPlayer(player)!=null) {
			return spawnLoc.get(getTeamOfPlayer(player));
		}
		return null;
	}

	
	@Override
	public String toString() {
		String str = "";
		if (teams.size()==0) {
			return "No Teams!";
		} else if (teams.size()==1) {
			return teams.get(0).getDisplayName();
		} else if (teams.size()==2) {
			return teams.get(0).getDisplayName() + " and " + teams.get(1).getDisplayName();
		}
		for (int i = 0; i < teams.size() - 1; i++) {
			str += getDisplayName(teams.get(i)) + ", ";
		}
		str += "and " + getDisplayName(teams.get(teams.size() - 1));
		return str;
	}
	
	public boolean onSameTeam(Player p1, Player p2) {
		for (Team t : teams) {
			if (t.getEntries().contains(p1.getName()) && t.getEntries().contains(p2.getName())) {
				return true;
			}
		}
		return false;
	}
}
