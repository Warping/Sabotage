package bubbles.sabotage.plugin;

import bubbles.sabotage.plugin.fileIO.ReadWrite;
import bubbles.sabotage.plugin.game.Game;
import bubbles.sabotage.plugin.groups.SabKits;
import bubbles.sabotage.plugin.items.*;
import bubbles.sabotage.plugin.items.customitem.CustomItem;
import bubbles.sabotage.plugin.listeners.KitIconListener;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main extends JavaPlugin {
	
	private Commands cmds;
	private Scoreboard board;
	private Game game;
	private EditMode editMode = new EditMode();
	private HashMap<String, CustomItem> items = new HashMap<String, CustomItem>();
	private final Level LOG_LEVEL = Level.INFO;
	public static Logger applog;
	
	@Override
	public void onEnable() {
		applog = getLogger().getParent();

		ConfigurationSerialization.registerClass(Bomb.class, "Bomb");
		ConfigurationSerialization.registerClass(Game.class, "Game");
		board = getServer().getScoreboardManager().getNewScoreboard();
		
		load("Sab");
		loadCustomItems();
		getServer().getPluginManager().registerEvents(new KitIconListener(), this);
		loadKits("Sab");
		game.setup();

		cmds = new Commands(game);
		registerSabCommand("sab", "Controls various sabotage gamemode functions.", "sabotage.sab", "/sab [start/stop/save/balance/edit/item/team/kit/bomb/help]");
		registerSabCommand("team", "Changes Team", null, "/team [color]");
		registerSabCommand("kit", "Changes Kit", null, "/kit [kitname]");
		applog.log(LOG_LEVEL,"Commands Loaded!");
		
	}
	
	@Override
	public void onDisable() {
		if (game != null) {
			game.stop();
			game.getTeams().unregisterAll();
		}
	}

	// Paper plugins can't declare commands in paper-plugin.yml or use getCommand(), so each
	// command is registered in code and forwarded to Commands#handle.
	private void registerSabCommand(String name, String description, String permission, String usage) {
		registerCommand(name, description, new BasicCommand() {
			@Override
			public void execute(CommandSourceStack stack, String[] args) {
				if (!cmds.handle(stack.getSender(), name, args)) {
					stack.getSender().sendMessage("Usage: " + usage);
				}
			}

			@Override
			public String permission() {
				return permission;
			}
		});
	}
	
	private void loadCustomItems() {
		items.put("healing", new HealingWand());
		items.put("grappler", new GrapplingHook());
		items.put("trash", new Trash());
		items.put("rifle", new Rifle());
		items.put("steak", new Steak());
		items.put("kit", new KitSelect());
		items.put("team", new TeamSelect());
		items.put("grenade", new Grenade());
		items.put("rewind", new RewindClock());
		items.put("flare", new Flare());
		items.put("landmine", new Landmine());
		items.put("teleporter", new Teleporter());
		items.put("bloodsucker", new Bloodsucker());
		items.put("baccasbounty", new BaccasBounty());
		items.put("poisoner", new Poisoner());
		items.put("warper", new Warper());
		items.put("barbed", new BarbedSlasher());
		for (String name : items.keySet()) {
			applog.log(LOG_LEVEL,"Item Loaded: " + name + " : " + items.get(name).getItem().getType());
		}
	}
	
	public HashMap<String, CustomItem> getItems() {
		return items;
	}

	public void load(String gameName) {
		
		if (game!=null) {
			game.getTeams().unregisterAll();
		}
		
		if (ReadWrite.getSabGame(gameName)!=null) {
			applog.log(LOG_LEVEL,"Loading " + gameName + "!");
			game = ReadWrite.getSabGame(gameName);
			for (int i = 0; i < game.getTeamNames().size(); i++) {
				game.addTeam(game.getTeamNames().get(i), game.getTeamSpawns().get(i));
				applog.log(LOG_LEVEL,"Team " + game.getTeamNames().get(i) + " has been loaded! (Spawn: " + game.getTeamSpawns().get(i) + ")");
			}
		} else {
			this.getLogger().log(LOG_LEVEL,"Creating " + gameName + "...");
			game = new Game(gameName, "world");
		}
		
		// No teams yet (brand new game, or an existing save with none added) - default to
		// red/blue/green/purple so the team GUI isn't empty out of the box.
		if (game.getTeams().getTeams().isEmpty()) {
			applog.log(LOG_LEVEL,"No teams found - creating default teams (red, blue, green, purple)...");
			for (String color : new String[] {"red", "blue", "green", "purple"}) {
				game.addTeam(color, game.getWorld().getSpawnLocation());
			}
		}
		
		if (ReadWrite.getSabBombs(gameName)!=null) {
			applog.log(LOG_LEVEL,"Loading Bombs!");
			ArrayList<Bomb> bombs = ReadWrite.getSabBombs(gameName);
			for (Bomb bomb : bombs) {
				game.addBomb(bomb);
				if (bomb.getOwner()!=null) {
					applog.log(LOG_LEVEL,"Bomb for Team " + bomb.getOwner().getName() + " loaded!");
				} else {
					applog.log(LOG_LEVEL,"Bomb for No Team loaded!");
				}
			}
		} else {
			applog.log(LOG_LEVEL,"Creating Bombs!");
			game.addBomb("red", 0, 3, 0, 0, 1, 0, 120);
		}
		
		//game.setup();
	}

	// Kits reference custom items by key, so this must run after loadCustomItems() has
	// registered them (see onEnable()).
	private void loadKits(String gameName) {
		if (ReadWrite.getSabKits(gameName)!=null) {
			applog.log(LOG_LEVEL,"Loading Kits!");
			SabKits kits = ReadWrite.getSabKits(gameName);
			game.setKits(kits);
			applog.log(LOG_LEVEL,"Kits Loaded: " + kits);
		} else {
			applog.log(LOG_LEVEL,"Creating Kits!");
		}
	}

	public Scoreboard getScoreboard() {
		return board;
	}
	
	public Commands getCmds() {
		return cmds;
	}

	public void updateScoreboard() {
		for (Player p : getServer().getOnlinePlayers()) {
			p.setScoreboard(board);
		}
	}

	// Replaces the shared scoreboard with a fresh one. Anything that cached the old board
	// (teams, bomb board) must be rebuilt against it, then updateScoreboard() applies it.
	public void resetScoreboard() {
		board = getServer().getScoreboardManager().getNewScoreboard();
	}

	public Game getGame() {
		return game;
	}
	public HashMap<String, CustomItem> getCustomItems() {
		return items;
	}

	public EditMode getEditMode() {
		return editMode;
	}
}
