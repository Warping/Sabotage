package bubbles.sabotage.plugin;

import bubbles.sabotage.plugin.game.Game;
import bubbles.sabotage.plugin.groups.SabKits;
import bubbles.sabotage.plugin.groups.SabTeams;
import bubbles.sabotage.plugin.items.KitSelect;
import bubbles.sabotage.plugin.items.customitem.CustomItem;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Commands implements CommandExecutor {
	
	private Game game;
	private SabTeams teams;
	private SabKits kits;
	
	public Commands(Game game) {
		this.game = game;
		this.teams = game.getTeams();
		this.kits = game.getKits();
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		Player p = (Player) sender;
		if (cmd.getName().equals("sab")) {
			if (args.length!=0) {
				switch (args[0]) {
				case "save":
					p.sendMessage("Saving...");
					game.save();
					p.sendMessage("Saved!");
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
					if (args.length==3) {
						if (args[1].equals("add")) {
							addTeam(args[2], p);
						} else {
							p.sendMessage("/sab team add [color]");
						}
					} else {
						p.sendMessage("/sab team add [color]");
					}
					break;
				case "kit":
					if (args.length>=3) {
						if (args[1].equals("add") && args.length==4) {
							try {
								Integer.parseInt(args[3]);
							} catch (NumberFormatException e) {
								p.sendMessage("/sab kit [add/remove] [kitname] [price]");
								return true;
							}
							addKit(args[2], Integer.parseInt(args[3]), p);
						} else if (args[1].equals("remove") && args.length==3) {
							removeKit(args[2], p);
						} else if (args[1].equals("load") && args.length==3) {
							if (Kit.load(p, kits.getKit(args[2]))) {
								p.sendMessage(ChatColor.GREEN + "Kit " + args[2] + " loaded!");
							} else {
								if (kits!=null) {
									p.sendMessage(ChatColor.RED + "Valid Kits are " + kits);
								} else {
									p.sendMessage(ChatColor.RED + "No Kits Available!");
								}
							}
						}
					} else {
						p.sendMessage("/sab kit [add/remove/load] [kitname] [price]");
					}
					break;
				default:
					return false;
				}
			} else {
				return false;
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
		Kit kit = new Kit(name, price, p);
		kits.addKit(kit);
		game.save();
		p.sendMessage(ChatColor.GOLD + "Added kit " + kit.getDisplayName());
	}
	
	private void removeKit(String name, Player p) {
		Kit remKit = null;
		for (int i = 0; i < kits.getKits().size(); i++) {
			if (kits.getKits().get(i).getName().equalsIgnoreCase(name.trim())) {
				remKit = kits.getKits().get(i);
			}
			if (remKit!=null) {
				kits.removeKit(remKit);
				game.save();
				p.sendMessage(ChatColor.GOLD + "Removed kit " + remKit.getDisplayName());
			}
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

}
