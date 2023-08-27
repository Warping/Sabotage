package bubbles.sabotage.plugin.game;

import bubbles.sabotage.plugin.Bomb;
import bubbles.sabotage.plugin.BombBoard;
import bubbles.sabotage.plugin.Kit;
import bubbles.sabotage.plugin.Main;
import bubbles.sabotage.plugin.counter.Counter;
import bubbles.sabotage.plugin.fileIO.ReadWrite;
import bubbles.sabotage.plugin.groups.SabKits;
import bubbles.sabotage.plugin.groups.SabTeams;
import bubbles.sabotage.plugin.items.customitem.CustomItem;
import bubbles.sabotage.plugin.listeners.GameListener;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.*;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scoreboard.Team;

import java.util.*;
import java.util.logging.Level;

import static bubbles.sabotage.plugin.Main.applog;

@SerializableAs("Game")
public class Game implements ConfigurationSerializable {
	
	private Main plugin = Main.getPlugin(Main.class);
	
	private String name; //Name of Game
	private World world; //World where game takes place
	private HashMap<Player, String> playerKits = new HashMap<Player, String>(); //Players and their selected kit
	private HashMap<Player, Boolean> playerStatus = new HashMap<Player, Boolean>();
	private ArrayList<Bomb> bombs = new ArrayList<Bomb>(); //Bombs in game
	private ArrayList<String> teamNames = new ArrayList<String>(); //Team names
	private SabTeams teams = new SabTeams(); //Teams available
	private SabKits kits = new SabKits(plugin); //Kits available
	private ArrayList<Location> teamLocs = new ArrayList<Location>(); //Team Locations
	private boolean active = false; //Game has started?
	private boolean autoBalance = true; //AutoBalance toggle
	@SuppressWarnings("unused")
	private GameListener listen;

	private final Level LOG_LEVEL = Level.INFO;
	
	public Game(String name, String world) {
		
		this.name = name;
		this.world = plugin.getServer().getWorld(world);
	}
	
	@SuppressWarnings("unchecked")
	public Game(Map<String, Object> map) {
		this.plugin = Main.getPlugin(Main.class);
		this.name = (String) map.get("Name");
		String _world = (String) map.get("World");
		this.world = plugin.getServer().getWorld(_world);
		this.teamNames = (ArrayList<String>) map.get("Teams");
		this.teamLocs  = (ArrayList<Location>) map.get("Team Spawns");
	}

	@Override
	public Map<String, Object> serialize() {
		Map<String, Object> map = new HashMap<>();
		map.put("Name", name);
		map.put("World", world.getName());
		map.put("Teams", teams.getTeamNames());
		map.put("Team Spawns", teams.getSpawnArray());
		return map;
	}
	
	public void save() {
		ReadWrite.saveSabKits(this);
		ReadWrite.saveSabBombs(this);
		ReadWrite.saveSabGame(this);
	}
	
	public void setup() {
		applog.log(LOG_LEVEL, "Updating Gamerules...");
		world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
		world.setGameRule(GameRule.DO_MOB_LOOT, false);
		world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
		world.setGameRule(GameRule.DO_FIRE_TICK, false);
		world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
		world.setGameRule(GameRule.DO_TILE_DROPS, false);
		world.setDifficulty(Difficulty.HARD);
		applog.log(LOG_LEVEL,"Establishing Listener...");
		listen = new GameListener(this);
		applog.log(LOG_LEVEL,"Game " + name + " has finished setup!");
		for (Player p : plugin.getServer().getOnlinePlayers()) {
			setPlayerStatus(p, false); //spectator
		}
		BombBoard.setup(this);
		BombBoard.display();
		plugin.updateScoreboard();
		applog.log(LOG_LEVEL,"Bomb Board loaded!");
		save();
		applog.log(LOG_LEVEL,"Game " + name + " has been saved!");
		stop();
	}
	
	public void giveMenuItems(Player player) {
		//player.getInventory().addItem(new ItemStack(Material.FEATHER));
		plugin.getItems().get("kit").give(player, 1);
		if (!active) {
			//player.getInventory().addItem(new ItemStack(Material.NETHER_STAR));
			plugin.getItems().get("team").give(player, 1);
		}
	}
	
	public void start() {
		if (active==true) {
			return;
		}
		active = true;
		for (Bomb bomb : bombs) {
			bomb.reset();
		}
		if (autoBalance) {
			teamBalance();
		}
		for (Player p : plugin.getServer().getOnlinePlayers()) {
			respawn(p);
		}
		BombBoard.update();
		applog.log(LOG_LEVEL,ChatColor.GREEN + "Sabotage has started!");
		
	}
	
	public void stop() {
		active = false;
		playerKits = new HashMap<Player, String>();
		for (Player p : plugin.getServer().getOnlinePlayers()) {
			spectator(p);
			showPlayer(p);
		}
		for (Bomb bomb : bombs) {
			bomb.reset();
		}
		BombBoard.display();
		BombBoard.stop();
		for (CustomItem item : plugin.getItems().values()) {
			item.stop();
		}
		applog.log(LOG_LEVEL,ChatColor.GREEN + "Sabotage has ended!");
		
	}
	
	public void clear(Player p) {
		p.getInventory().clear();
		p.updateInventory();
		for (PotionEffect potion : p.getActivePotionEffects()) {
			p.removePotionEffect(potion.getType());
		}
		//p.setHealth(p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getDefaultValue());
		p.setMaxHealth(20);
		p.setHealth(20);
		p.setFoodLevel(20);
		p.setExp(0);
		p.setLevel(0);
		p.setArrowsInBody(0);
		Bukkit.getScheduler().runTask(plugin, new Runnable() {
			@Override
			public void run() {
				p.setFireTicks(0);
			}
		});		
	}
	
	public void respawn(Player player) {
		player.setAllowFlight(false);
		if (active) {
			if (playerKits.get(player)==null) {
				setSelectedKit(player, kits.getKits().get(0).getName());	
			}
			Kit.load(player, kits.getKit(playerKits.get(player)));
			if (teams.getSpawnLoc(player)!=null) {
				player.teleport(teams.getSpawnLoc(player));
			}
			setPlayerStatus(player, true);
		}
		showPlayer(player);
		player.setInvulnerable(false);
	}
	
	public void spectator(Player player) {
		clear(player);
		giveMenuItems(player);
		setPlayerStatus(player, false);
		player.setInvulnerable(true);
		player.setAllowFlight(true);
		if (active) {
			hidePlayer(player);
		}
	}
	
	public void deathCounter(Player player, int seconds) {
		new Counter(20L) {
			
			int currentTime = seconds;
			
			@Override
			public void run() {
				if (currentTime > 0 && active) {
					player.setLevel(currentTime);
				} else {
					respawn(player);
					cancel();
				}
				currentTime--;
				
			}
		};
	}
	
	public void teamBalance() {
		int teamsNum = teams.getTeams().size();
		int playersSize = plugin.getServer().getOnlinePlayers().size();
		int neededSize = playersSize / teamsNum;
		if (playersSize % teamsNum != 0) {
			neededSize++;
		}
		Queue<String> noTeamPlayers = new LinkedList<>();
		Set<String> teamPlayers = teams.getTeamPlayers();
		for (Player p : plugin.getServer().getOnlinePlayers()) {
			if (!teamPlayers.contains(p.getName())) {
				noTeamPlayers.add(p.getName());
			}
		}
		int teamSize = 0;
		for (int i = 0; i < plugin.getServer().getOnlinePlayers().size(); i++) {
			for (Team team : teams.getTeams()) {
				teamSize = team.getEntries().size();
				if (teamSize > neededSize) {
					String[] entries = team.getEntries().toArray(new String[team.getEntries().size()]);
					String playerString = entries[entries.length - 1];
					if (plugin.getServer().getPlayer(playerString)!=null) {
						Player p = plugin.getServer().getPlayer(playerString);
						teams.removePlayer(p);
						noTeamPlayers.add(p.getName());
					} else {
						teams.removeOfflinePlayer(playerString);
					}
					teamSize = team.getEntries().size();
				}
				if (teamSize < neededSize && noTeamPlayers.size()>0) {
					Player p = plugin.getServer().getPlayer(noTeamPlayers.remove());
					teams.addPlayer(p, team.getName());
				}
			}
		}
	}
	
	public boolean addTeam(String teamName, Location spawn) {
		boolean success = teams.addTeam(teamName, spawn);
		if (bombs.size()!=0 && success) {
			for (Bomb bomb : bombs) {
				bomb.setTeams(teams);
			}
		}
		return success;
	}
	
	public void hidePlayer(Player p) {
		for (Player player : plugin.getServer().getOnlinePlayers()) {
			player.hidePlayer(plugin, p);
		}
	}
	
	public void showPlayer(Player p) {
		for (Player player : plugin.getServer().getOnlinePlayers()) {
			player.showPlayer(plugin, p);
		}
	}
	
	public void addBomb(String owner, int armX, int armY, int armZ, int disarmX, int disarmY, int disarmZ, int time) {
		Bomb bomb = new Bomb(owner, new Location(world, armX, armY, armZ), new Location(world, disarmX, disarmY, disarmZ), time);
		bomb.setTeams(teams);
		bombs.add(bomb);
	}
	
	public void addBomb(String owner, int armX, int armY, int armZ, int time) {
		Bomb bomb = new Bomb(owner, new Location(world, armX, armY, armZ), new Location(world, armX, armY, armZ), time);
		bomb.setTeams(teams);
		bombs.add(bomb);
	}
	
	public void addBovmb(int armX, int armY, int armZ, int time) {
		Bomb bomb = new Bomb("NONE", new Location(world, armX, armY, armZ), new Location(world, armX, armY, armZ), time);
		bomb.setTeams(teams);
		bombs.add(bomb);
	}
	
	public void addBomb(Bomb bomb) {
		bomb.setTeams(teams);
		bombs.add(bomb);
	}

	public Main getPlugin() {
		return plugin;
	}

	public void setPlugin(Main plugin) {
		this.plugin = plugin;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public World getWorld() {
		return world;
	}

	public void setWorld(World world) {
		this.world = world;
	}

	public ArrayList<Bomb> getBombs() {
		return bombs;
	}

	public SabTeams getTeams() {
		return teams;
	}

	public void setTeams(SabTeams teams) {
		this.teams = teams;
	}

	public SabKits getKits() {
		return kits;
	}

	public void setKits(SabKits kits) {
		this.kits = kits;
	}

	public ArrayList<String> getTeamNames() {
		return teamNames;
	}
	
	public ArrayList<Location> getTeamSpawns() {
		return teamLocs;
	}

	public void setTeamNames(ArrayList<String> teamNames) {
		this.teamNames = teamNames;
	}
	
	public void setSelectedKit(Player p, String kit) {
		this.playerKits.put(p, kit);
	}
	
	public void respawnPlayer(Player p) {
		Kit.load(p, kits.getKit(playerKits.get(p)));
		
	}

	public boolean isActive() {
		return active;
	}

	public boolean isAutoBalance() {
		return autoBalance;
	}

	public void setAutoBalance(boolean autoBalance) {
		this.autoBalance = autoBalance;
	}

	public HashMap<Player, String> getPlayerKits() {
		return playerKits;
	}
	
	public void setPlayerStatus(Player player, boolean spectator) {
		playerStatus.put(player, spectator);
	}
	
	public boolean getPlayerStatus(Player player) {
		return playerStatus.get(player);
	}
	

}
