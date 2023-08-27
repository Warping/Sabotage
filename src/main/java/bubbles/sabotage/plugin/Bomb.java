package bubbles.sabotage.plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import bubbles.sabotage.plugin.counter.Counter;
import bubbles.sabotage.plugin.groups.SabTeams;
import bubbles.sabotage.plugin.listeners.BombListener;
import net.md_5.bungee.api.ChatColor;

import static bubbles.sabotage.plugin.Main.applog;

@SerializableAs("Bomb")
public class Bomb implements ConfigurationSerializable {
	
	private boolean isExploded; //Checks if bomb has blown up
	private boolean isArmed; //Bomb armed status
	private boolean isCooldown; //Check if cooldown active
	private String _owner; //Owner passed as String for Serialization
	private Team owner; //Owner of bomb (null means anyone can arm and disarm)
	private Team armedBy; // Team which armed the bomb
	private Location armLoc; //Location where bomb shall be armed
	private Location disarmLoc; //Location of where bomb shall be disarmed
	private World world;
	private SabTeams teams; //Teams which can interact with this bomb
	private Double disarmStatus; //Percent until bomb is disarmed
	private HashMap<Team, Double> teamStatus = new HashMap<Team, Double>(); //Percent until team bomb is armed
	private HashMap<Team, Integer> teamTimer = new HashMap<Team, Integer>(); //Timer until bomb explodes for team
	private BombListener listen; //Listens for arm/disarm events as well as break events
	private Main plugin = Main.getPlugin(Main.class); //Loads current plugin for world access/server messages etc.
	private int startTime; //Start time for each team
	private final Level LOG_LEVEL = Level.FINE;
	private final Level LOG_LEVEL_2 = Level.FINER;

	//Create Bomb
	public Bomb(String owner, Location armLoc, Location disarmLoc, int startTime) {
		this._owner = owner;
		this.armLoc = armLoc;
		this.world = armLoc.getWorld();
		this.disarmLoc = disarmLoc;
		this.startTime = startTime;
	}
	
	public static Bomb deserialize(Map<String, Object> map) {
		String owner = (String) map.get("Owner");
		Location armLoc = (Location) map.get("Arm Location");
		Location disarmLoc = (Location) map.get("Disarm Location");
		int startTime = (int) map.get("Timer");
		return new Bomb(owner, armLoc, disarmLoc, startTime);
	}
	
	@Override
	public Map<String, Object> serialize() {
		Map<String, Object> map = new HashMap<>();
		if (this.owner==null) {
			map.put("Owner", "NONE");
		} else {
			map.put("Owner", this.owner.getName());
		}
		map.put("Arm Location", armLoc);
		map.put("Disarm Location", disarmLoc);
		map.put("Timer", startTime);
		return map;
	}
	
	public void setTeams(SabTeams teams) {
		this.teams = teams;
		setOwner();
		reset();
		listen = new BombListener(this);
	}
	
	private void setOwner() {
		if (this._owner.equalsIgnoreCase("none")) {
			this.owner = null;
		} else {
			this.owner = teams.getTeam(_owner);
		}
	}
	
	public void resetTimerAll() {
		for (Team team : teams.getTeams()) {
			resetTimer(team);
		}
	}
	
	public void resetTimer(Team team) {
		teamTimer.put(team, startTime);
	}
	
	public void countDown(Team team) {
		new Counter(10L) {
			
			@Override
			public void run() {
				if (isArmed) {
					if (disarmLoc.getBlock().getType().equals(Material.TNT)) {
						if (getOwner()==null) {
							disarmLoc.getBlock().setType(teams.getBlock(team));
						} else {
							disarmLoc.getBlock().setType(teams.getBlock(getOwner()));
						}
						teamTimer.put(team, teamTimer.get(team) - 1);
						if (teamTimer.get(team)%30==0 || teamTimer.get(team)<=10) {
							for (Player p : world.getPlayers()) {
								if (getOwner()==null) {
									if (!getArmedBy().getEntries().contains(p.getName())) {
										p.playSound(p.getLocation(), Sound.ENTITY_BLAZE_DEATH, 0.3F, 2F);
									}
									p.sendMessage(SabTeams.getDisplayName(team) + ChatColor.GOLD + " team's bomb explodes in " + teamTimer.get(team) + " seconds!");
								} else if (getOwner().getEntries().contains(p.getName())){
									p.playSound(p.getLocation(), Sound.ENTITY_BLAZE_DEATH, 0.3F, 2F);
									p.sendMessage(ChatColor.GOLD + "Your bomb explodes in " + teamTimer.get(team) + " seconds!");
								}
							}
						}
					} else {
						disarmLoc.getBlock().setType(Material.TNT);
						world.playSound(disarmLoc, Sound.ENTITY_CREEPER_DEATH, 0.5F, 1F);
					}
					if (teamTimer.get(team)==0) {
						applog.log(LOG_LEVEL, "Bomb has exploded for team " + team.getName() + "!");
						explode();
						cancel();
					}
				} else {
					applog.log(LOG_LEVEL,"Bomb disarmed! Timer stopped!");
					cancel();
				}
			}
		};
	}
	
	public void reset() {
		disarmLoc.getBlock().setType(Material.AIR);
		armLoc.getBlock().setType(Material.TNT);
		resetArmStatusAll();
		resetDisarmStatus();
		resetTimerAll();
		isCooldown = false;
		isArmed = false;
		isExploded = false;
	}
	
	public void explode() {
		disarmLoc.getBlock().setType(Material.BLACK_WOOL);
		world.playSound(disarmLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.3F, 0.5F);
		if (getOwner()==null) {
			plugin.getServer().broadcastMessage(SabTeams.getDisplayName(armedBy) + " team has blown up a bomb!");
		} else {
			plugin.getServer().broadcastMessage(SabTeams.getDisplayName(armedBy) + " team has blown up a " + SabTeams.getDisplayName(getOwner()) + " bomb!");
		}
		isExploded = true;
	}
	
	public void resetArmStatusAll() {
		for (Team team : teams.getTeams()) {
			resetArmStatus(team);
		}
	}
	
	public void resetArmStatus(Team team) {
		teamStatus.put(team, 0.0);
	}
	
	public void resetDisarmStatus() {
		disarmStatus = 0.0;
	}
	
	public Team getOwner() {
		return owner;
	}

	public boolean isExploded() {
		return isExploded;
	}

	public SabTeams getSabTeams() {
		return teams;
	}
	
	public BombListener getListen() {
		return listen;
	}
	
	public Main getPlugin() {
		return plugin;
	}

	public boolean isArmed() {
		return isArmed;
	}

	public Team getArmedBy() {
		return armedBy;
	}

	public Location getArmLoc() {
		return armLoc;
	}

	public Location getDisarmLoc() {
		return disarmLoc;
	}

	public double getDisarmStatus() {
		return disarmStatus;
	}
	
	public double getArmStatus(Team team) {
		return teamStatus.get(team);
	}

	public int getTeamTimer(Team team) {
		return this.teamTimer.get(team);
	}

	public void armBomb(Team armedBy) {
		this.armedBy = armedBy;
		isArmed = true;
		armLoc.getBlock().setType(Material.AIR);
		disarmLoc.getBlock().setType(Material.TNT);
		for (Player p : plugin.getServer().getOnlinePlayers()) {
			plugin.getServer().getWorld("world").playSound(p.getLocation(), Sound.ENTITY_BLAZE_DEATH, (float) 0.3, (float) .5);
		}
		if (this.owner == null) {
			countDown(armedBy);
		} else {
			countDown(owner);
		}
	}
	
	public void disarmBomb(Team disarmedBy) {
		this.armedBy = null;
		isArmed = false;
		disarmLoc.getBlock().setType(Material.AIR);
		armLoc.getBlock().setType(Material.OBSIDIAN);
		isCooldown = true;
		plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable () {
			@Override
			public void run() {
				isCooldown = false;
				armLoc.getBlock().setType(Material.TNT);
			}
		}, 160L);
	}
	
	private void armingCheck(Team team, double oldStatus, Player player) {
		plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
			
			@Override
			public void run() {
				double newStatus = teamStatus.get(team);
				if (newStatus==oldStatus) {
					resetArmStatus(team);
					//plugin.updateScoreboard();
				}
				if ((float) oldStatus == player.getExp()) {
					player.setExp(0F);
				}
				String log = Double.toString(newStatus - oldStatus);
				applog.log(LOG_LEVEL_2, log);
				double otherArmStatus = 0;
				for (Team other : teamStatus.keySet()) {
					if (other!=team) {
						otherArmStatus += teamStatus.get(other);
					}
				}
				if (newStatus - oldStatus >= .06 && otherArmStatus > 0.11) {
					for (Team other : teamStatus.keySet()) {
						resetArmStatus(other);
					}
					player.sendMessage(ChatColor.RED + "Overloading Bomb!");
				}
			}
		}, 20L);
	}
	
	private void disarmingCheck(double oldStatus, Player player) {
		plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
			
			@Override
			public void run() {
				double newStatus = disarmStatus;
				if (newStatus==oldStatus) {
					resetDisarmStatus();
					plugin.updateScoreboard();
				}
				if ((float) oldStatus == player.getExp()) {
					player.setExp(0F);
				}
			}
		}, 20L);
	}
	
	private void sound() {
		if (isArmed) {
			plugin.getServer().getWorld("world").playSound(disarmLoc, Sound.BLOCK_LAVA_EXTINGUISH, (float) 0.3, (float) .7);
		} else {
			plugin.getServer().getWorld("world").playSound(armLoc, Sound.BLOCK_LAVA_EXTINGUISH, (float) 0.3, (float) .7);
		}
	}

	public void disarming(Player player, Team team) {
		sound();
		double val = disarmStatus;
		val += 0.025;
		val = Math.round(val * 100.0) / 100.0;
		disarmStatus = val;
		player.setExp(disarmStatus.floatValue());
		if (disarmStatus>=1.0) {
			disarmBomb(team);
			resetArmStatusAll();
			player.setExp(0F);
		}
		//plugin.updateScoreboard();
		disarmingCheck(disarmStatus, player);
		
	}

	public void arming(Player player, Team team) {
		if (isCooldown) {
			return;
		}
		sound();
		double val = teamStatus.get(team);
		val += 0.025;
		val = Math.round(val * 100.0) / 100.0;
		teamStatus.put(team, val);
		player.setExp(teamStatus.get(team).floatValue());
		if (teamStatus.get(team)>=1.0) {
			armBomb(team);
			resetArmStatusAll();
			player.setExp(0F);
		}
		//plugin.updateScoreboard();
		armingCheck(team, teamStatus.get(team), player);
	}
	
	public void denyArm(Player p) {
		p.sendMessage(ChatColor.RED + "You cannot arm your own team's bomb!");
	}
	
	public void denyDisarm(Player p) {
		if (owner==null) {
			p.sendMessage(ChatColor.RED + "You cannot disarm a bomb you armed!");
		} else {
			p.sendMessage(ChatColor.RED + "You cannot disarm " + owner.getColor() + owner.getName() + ChatColor.RED + " team's bomb!");
		}
	}
	
	public void explodedMsg(Player p) {
		p.sendMessage(ChatColor.RED + "This bomb has already exploded!");
	}
}
