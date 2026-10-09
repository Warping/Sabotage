package bubbles.sabotage.plugin;

import bubbles.sabotage.plugin.fileIO.KitIO;
import bubbles.sabotage.plugin.fileIO.ReadWrite;
import bubbles.sabotage.plugin.game.Game;
import bubbles.sabotage.plugin.groups.SabKits;
import bubbles.sabotage.plugin.groups.SabTeams;
import bubbles.sabotage.plugin.items.KitSelect;
import bubbles.sabotage.plugin.items.customitem.CustomItem;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;

public class Commands implements CommandExecutor {
	
	private Game game;
	private SabTeams teams;
	private SabKits kits;
	private static boolean debugMode = false;
	
	public Commands(Game game) {
		this.game = game;
		this.teams = game.getTeams();
		this.kits = game.getKits();
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		Player p = (Player) sender;
		if (cmd.getName().equals("sab")) {
			if (args.length==0) {
				sendHelp(p);
				return true;
			}
			switch (args[0].toLowerCase()) {
			case "save":
				p.sendMessage("Saving...");
				game.save();
				p.sendMessage("Saved!");
				break;
			case "reload":
				reloadAll(p);
				break;
			case "stop":
				game.stop();
				break;
				
			case "start":
				game.start();
				break;
			case "balance":
				if (game.isAutoBalance()) {
					game.setAutoBalance(false);
					p.sendMessage("Team balancing disabled!");
				} else {
					game.setAutoBalance(true);
					p.sendMessage("Team balancing enabled!");
				}
				break;
				
			case "edit":
				game.getPlugin().getEditMode().toggle(p);
				break;
			
			case "debug":
				debugMode = !debugMode;
				p.sendMessage(ChatColor.GOLD + "Debug mode " + (debugMode ? ChatColor.GREEN + "enabled!" : ChatColor.RED + "disabled!"));
				break;
			
			case "editmodedbg":
				game.getPlugin().getEditMode().setDebugMode(!game.getPlugin().getEditMode().isDebugMode());
				p.sendMessage(ChatColor.GOLD + "Edit mode debug " + (game.getPlugin().getEditMode().isDebugMode() ? ChatColor.GREEN + "enabled!" : ChatColor.RED + "disabled!"));
				break;
				
			case "item":
				if (args.length==4) {
					if (game.getPlugin().getServer().getPlayer(args[1])==null) {
						p.sendMessage(ChatColor.RED + "Player not found!");
						return true;
					}
					Player receiver = game.getPlugin().getServer().getPlayer(args[1]);
					try {
						Integer.parseInt(args[3]);
					} catch (NumberFormatException e) {
						p.sendMessage("/sab item [player] [itemname] [count]");
						return true;
					}
					giveItem(p, receiver, args[2], Integer.parseInt(args[3]));
				} else {
					p.sendMessage("/sab item [player] [itemname] [count]");
				}
				break;
				
			case "team":
				if (args.length==3 && args[1].equalsIgnoreCase("add")) {
					addTeam(args[2], p);
				} else if (args.length==3 && args[1].equalsIgnoreCase("setspawn")) {
					setTeamSpawn(args[2], p);
				} else {
					sendTeamHelp(p);
				}
				break;
			case "kit":
				if (args.length==4 && args[1].equalsIgnoreCase("add")) {
					try {
						Integer.parseInt(args[3]);
					} catch (NumberFormatException e) {
						p.sendMessage(ChatColor.RED + "Price must be a whole number!");
						sendKitHelp(p);
						return true;
					}
					addKit(args[2], Integer.parseInt(args[3]), p);
				} else if (args.length==3 && args[1].equalsIgnoreCase("remove")) {
					removeKit(args[2], p);
				} else if (args.length==3 && args[1].equalsIgnoreCase("load")) {
					if (Kit.load(p, kits.getKit(args[2]))) {
						p.sendMessage(ChatColor.GREEN + "Kit " + args[2] + " loaded!");
					} else if (!kits.getKits().isEmpty()) {
						p.sendMessage(ChatColor.RED + "Valid Kits are " + kits);
					} else {
						p.sendMessage(ChatColor.RED + "No Kits Available!");
					}
				} else {
					sendKitHelp(p);
				}
				break;
			case "bomb":
				if (args.length==4 && args[1].equalsIgnoreCase("add")) {
					try {
						Integer.parseInt(args[3]);
					} catch (NumberFormatException e) {
						p.sendMessage(ChatColor.RED + "Timer must be a whole number of seconds!");
						sendBombHelp(p);
						return true;
					}
					addBomb(args[2], Integer.parseInt(args[3]), p);
				} else if (args.length==3 && args[1].equalsIgnoreCase("remove")) {
					removeBomb(args[2], p);
				} else if (args.length>=3 && args[1].equalsIgnoreCase("tp")) {
					teleportToBomb(args[2], args.length>=4 ? args[3] : "arm", p);
				} else {
					sendBombHelp(p);
				}
				break;
			case "help":
				sendHelp(p);
				break;
			default:
				sendHelp(p);
				break;
			}
		} else if (cmd.getName().equals("team")) {
			if (args.length!=0) {
				joinTeam(args[0], p);
			} else {
				return false;
			}
		}
		else if (cmd.getName().equals("kit")) {
			if (args.length!=0) {
				selectKit(args[0], p);
			} else {
				selectKit("", p);
			}
		}
		return true;
	}

	private void sendHelp(Player p) {
		p.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Sabotage Admin Commands:");
		p.sendMessage(ChatColor.YELLOW + "/sab start" + ChatColor.GRAY + " - Starts the game");
		p.sendMessage(ChatColor.YELLOW + "/sab stop" + ChatColor.GRAY + " - Stops the game and returns everyone to spectator");
		p.sendMessage(ChatColor.YELLOW + "/sab save" + ChatColor.GRAY + " - Saves teams, kits, and bombs to disk");
		p.sendMessage(ChatColor.YELLOW + "/sab reload" + ChatColor.GRAY + " - Reloads all kits, teams, and bombs from .yml files");
		p.sendMessage(ChatColor.YELLOW + "/sab balance" + ChatColor.GRAY + " - Toggles automatic team balancing");
		p.sendMessage(ChatColor.YELLOW + "/sab edit" + ChatColor.GRAY + " - Toggles Creative map-editing mode (saves/restores your loadout)");
		p.sendMessage(ChatColor.YELLOW + "/sab debug" + ChatColor.GRAY + " - Toggles debug output filtering");
		p.sendMessage(ChatColor.YELLOW + "/sab item [player] [item] [count]" + ChatColor.GRAY + " - Gives a custom item to a player");
		p.sendMessage(ChatColor.YELLOW + "/sab team" + ChatColor.GRAY + " - Manage teams (run for details)");
		p.sendMessage(ChatColor.YELLOW + "/sab kit" + ChatColor.GRAY + " - Manage kits (run for details)");
		p.sendMessage(ChatColor.YELLOW + "/sab bomb" + ChatColor.GRAY + " - Manage bombs (run for details)");
	}

	private void sendTeamHelp(Player p) {
		p.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Team Commands:");
		p.sendMessage(ChatColor.YELLOW + "/sab team add <color>" + ChatColor.GRAY + " - Adds a team, spawn set to your location");
		p.sendMessage(ChatColor.YELLOW + "/sab team setspawn <color>" + ChatColor.GRAY + " - Sets an existing team's spawn to your location");
		if (teams.getTeams().isEmpty()) {
			p.sendMessage(ChatColor.RED + "No teams exist yet!");
		} else {
			p.sendMessage(ChatColor.GRAY + "Current teams: " + teams);
		}
	}

	private void sendKitHelp(Player p) {
		p.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Kit Commands:");
		p.sendMessage(ChatColor.YELLOW + "/sab kit add <name> <price>" + ChatColor.GRAY + " - Saves your hotbar/armor/offhand as a new kit");
		p.sendMessage(ChatColor.YELLOW + "/sab kit remove <name>" + ChatColor.GRAY + " - Deletes a kit");
		p.sendMessage(ChatColor.YELLOW + "/sab kit load <name>" + ChatColor.GRAY + " - Equips a kit on yourself");
		if (kits.getKits().isEmpty()) {
			p.sendMessage(ChatColor.RED + "No kits exist yet!");
		} else {
			p.sendMessage(ChatColor.GRAY + "Current kits: " + kits);
		}
	}

	private void sendBombHelp(Player p) {
		p.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Bomb Commands:");
		p.sendMessage(ChatColor.YELLOW + "/sab bomb add <owner|none> <seconds>" + ChatColor.GRAY + " - Adds a bomb at your location");
		p.sendMessage(ChatColor.YELLOW + "/sab bomb remove <#>" + ChatColor.GRAY + " - Removes a bomb by its number below");
		p.sendMessage(ChatColor.YELLOW + "/sab bomb tp <#> [arm/disarm]" + ChatColor.GRAY + " - Teleports to a bomb's arm or disarm spot");
		listBombs(p);
	}

	private void listBombs(Player p) {
		ArrayList<Bomb> bombs = game.getBombs();
		if (bombs.isEmpty()) {
			p.sendMessage(ChatColor.RED + "No bombs exist yet!");
			return;
		}
		for (int i = 0; i < bombs.size(); i++) {
			Bomb bomb = bombs.get(i);
			String owner = bomb.getOwner()!=null ? SabTeams.getDisplayName(bomb.getOwner()) : ChatColor.GRAY + "Any team";
			Location loc = bomb.getArmLoc();
			p.sendMessage(ChatColor.YELLOW + "" + (i + 1) + ". " + ChatColor.RESET + owner + ChatColor.GRAY
					+ " @ " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
		}
	}

	private void giveItem(Player sender, Player player, String name, int count) {
		if (game.getPlugin().getItems().get(name)!=null) {
			CustomItem item = game.getPlugin().getItems().get(name);
			sender.sendMessage("Giving " + count + " of " + name + " to " + player.getName() +"!");
			item.give(player, count);
		} else {
			sender.sendMessage("Item " + name + " does not exist!");
			String items = "Try";
			for (String itemName : game.getPlugin().getItems().keySet()) {
				items += " " + itemName;
			}
			sender.sendMessage(items);
		}
	}
	
	private void addKit(String name, int price, Player p) {
		Kit kit = Kit.fromPlayer(name, price, p);
		kits.addKit(kit);
		game.save();
		p.sendMessage(ChatColor.GOLD + "Added kit " + kit.getDisplayName());
		p.sendMessage(ChatColor.GRAY + "Edit its icon, description, and items in plugins/Sabotage/"
				+ game.getName() + "/kits/" + kit.getName() + ".yml");
	}
	
	private void removeKit(String name, Player p) {
		Kit remKit = null;
		for (int i = 0; i < kits.getKits().size(); i++) {
			if (kits.getKits().get(i).getName().equalsIgnoreCase(name.trim())) {
				remKit = kits.getKits().get(i);
				break;
			}
		}
		if (remKit!=null) {
			kits.removeKit(remKit);
			KitIO.deleteKit(remKit.getName(), game.getName());
			game.save();
			p.sendMessage(ChatColor.GOLD + "Removed kit " + remKit.getDisplayName());
		} else {
			p.sendMessage(ChatColor.RED + "Kit " + name + " does not exist!");
		}
	}
	
	private void addTeam(String name, Player p) {
		if (game.addTeam(name, p.getLocation())) {
			p.sendMessage(ChatColor.GOLD + "Added team " + SabTeams.getDisplayName(game.getTeams().getTeam(name)) + "!");
			game.save();
		} else {
			p.sendMessage(ChatColor.RED + "Team " + SabTeams.getDisplayName(game.getTeams().getTeam(name)) + " already exists!");
		}
	}

	private void setTeamSpawn(String name, Player p) {
		Team team = teams.getTeam(name.trim().toLowerCase());
		if (team==null) {
			p.sendMessage(ChatColor.RED + "Team " + name + " does not exist!");
			p.sendMessage(ChatColor.RED + "Valid Teams are " + teams);
			return;
		}
		teams.setSpawnLoc(team, p.getLocation());
		game.save();
		p.sendMessage(ChatColor.GOLD + "Set spawn for " + SabTeams.getDisplayName(team) + ChatColor.GOLD + " to your location!");
	}

	private void addBomb(String owner, int time, Player p) {
		if (!owner.equalsIgnoreCase("none") && teams.getTeam(owner.trim().toLowerCase())==null) {
			p.sendMessage(ChatColor.RED + "Team " + owner + " does not exist! Use 'none' for a bomb any team can arm.");
			p.sendMessage(ChatColor.RED + "Valid Teams are " + teams);
			return;
		}
		Location loc = p.getLocation();
		game.addBomb(owner, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), time);
		game.save();
		p.sendMessage(ChatColor.GOLD + "Added a " + time + "s bomb at your location!");
	}

	private void removeBomb(String indexArg, Player p) {
		int index;
		try {
			index = Integer.parseInt(indexArg) - 1;
		} catch (NumberFormatException e) {
			p.sendMessage(ChatColor.RED + "Bomb number must be a number! Use /sab bomb to list bombs.");
			return;
		}
		ArrayList<Bomb> bombs = game.getBombs();
		if (index < 0 || index >= bombs.size()) {
			p.sendMessage(ChatColor.RED + "No bomb #" + indexArg + "! Use /sab bomb to list bombs.");
			return;
		}
		
		// Get bomb before removing to access its locations
		Bomb bomb = bombs.get(index);
		
		// Clear TNT blocks if they exist
		if (bomb.getArmLoc().getBlock().getType().equals(Material.TNT)) {
			bomb.getArmLoc().getBlock().setType(Material.AIR);
		}
		if (bomb.getDisarmLoc().getBlock().getType().equals(Material.TNT)) {
			bomb.getDisarmLoc().getBlock().setType(Material.AIR);
		}
		
		if (game.removeBomb(index)) {
			game.save();
			p.sendMessage(ChatColor.GOLD + "Removed bomb #" + (index + 1));
		}
	}

	private void teleportToBomb(String indexArg, String part, Player p) {
		int index;
		try {
			index = Integer.parseInt(indexArg) - 1;
		} catch (NumberFormatException e) {
			p.sendMessage(ChatColor.RED + "Bomb number must be a number! Use /sab bomb to list bombs.");
			return;
		}
		ArrayList<Bomb> bombs = game.getBombs();
		if (index < 0 || index >= bombs.size()) {
			p.sendMessage(ChatColor.RED + "No bomb #" + indexArg + "! Use /sab bomb to list bombs.");
			return;
		}
		Bomb bomb = bombs.get(index);
		boolean disarm = part.equalsIgnoreCase("disarm");
		p.teleport(disarm ? bomb.getDisarmLoc() : bomb.getArmLoc());
		p.sendMessage(ChatColor.GOLD + "Teleported to bomb #" + (index + 1) + "'s " + (disarm ? "disarm" : "arm") + " location!");
	}
	
	public void joinTeam(String name, Player p) {
		if (game.isActive()) {
			p.sendMessage(ChatColor.RED + "Game has already begun!");
			return;
		}
		if (teams.getTeam(name)!=null) {
			teams.addPlayer(p, name);
			p.sendMessage(ChatColor.GOLD + "You are now on the " + SabTeams.getDisplayName(teams.getTeam(name)) + " team!");
		} else {
			p.sendMessage(ChatColor.RED + "Invalid team!");
			p.sendMessage(ChatColor.RED + "Valid Teams are " + teams);
		}
	}
	
	public void selectKit(String kit, Player p) {
		KitSelect kitSelect = (KitSelect) game.getPlugin().getCustomItems().get("kit");
		if (kits.getKits().isEmpty()) p.sendMessage(ChatColor.RED + "No Kits Available!");
		if (kit.equals("")) kitSelect.getGUI().open(p);
		if (kits.getKit(kit)!=null) {
			p.sendMessage(ChatColor.GOLD + "You are now kit " + kits.getKit(kit).getDisplayName() + "!");
			game.setSelectedKit(p, kit);
		} else {
			p.sendMessage(ChatColor.RED + "Kit " + kit + " does not exist!");
			p.sendMessage(ChatColor.RED + "Valid Kits are " + kits);
		}
	}

	private void reloadAll(Player p) {
		p.sendMessage(ChatColor.GOLD + "Reloading kits, teams, and bombs...");
		
		// Reload kits
		SabKits newKits = KitIO.loadKits(game.getName());
		game.setKits(newKits);
		this.kits = newKits;
		
		// Reload teams - unregister old ones and create fresh instance
		game.getTeams().unregisterAll();
		Game savedGame = ReadWrite.getSabGame(game.getName());
		if (savedGame != null && !savedGame.getTeamNames().isEmpty()) {
			SabTeams newTeams = new SabTeams();
			for (int i = 0; i < savedGame.getTeamNames().size(); i++) {
				newTeams.addTeam(savedGame.getTeamNames().get(i), savedGame.getTeamSpawns().get(i));
			}
			game.setTeams(newTeams);
			this.teams = newTeams;
		}
		
		// Reload bombs
		ArrayList<Bomb> newBombs = ReadWrite.getSabBombs(game.getName());
		if (newBombs != null) {
			for (Bomb bomb : newBombs) {
				bomb.setTeams(teams);
			}
			game.getBombs().clear();
			game.getBombs().addAll(newBombs);
		}
		
		// Update bomb board
		BombBoard.update();
		
		p.sendMessage(ChatColor.GREEN + "Reload complete!");
		p.sendMessage(ChatColor.GRAY + "Kits: " + kits.getKits().size() + ", Teams: " + teams.getTeams().size() + ", Bombs: " + game.getBombs().size());
	}

	public static boolean isDebugMode() {
		return debugMode;
	}

}

