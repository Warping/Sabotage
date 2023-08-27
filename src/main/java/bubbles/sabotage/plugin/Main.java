package bubbles.sabotage.plugin;

import bubbles.sabotage.plugin.fileIO.ReadWrite;
import bubbles.sabotage.plugin.game.Game;
import bubbles.sabotage.plugin.groups.SabKits;
import bubbles.sabotage.plugin.items.*;
import bubbles.sabotage.plugin.items.customitem.CustomItem;
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
	private HashMap<String, CustomItem> items = new HashMap<String, CustomItem>();
	private final Level LOG_LEVEL = Level.INFO;
	public static Logger applog;
	
	@Override
	public void onEnable() {
		applog = getLogger().getParent();

		ConfigurationSerialization.registerClass(SabKits.class, "SabKits");
		ConfigurationSerialization.registerClass(Kit.class, "Kit");
		ConfigurationSerialization.registerClass(Bomb.class, "Bomb");
		ConfigurationSerialization.registerClass(Game.class, "Game");
		board = getServer().getScoreboardManager().getNewScoreboard();
		
		load("Sab");
		loadCustomItems();
		game.setup();

		cmds = new Commands(game);
		getCommand("sab").setExecutor(cmds);
		getCommand("team").setExecutor(cmds);
		getCommand("kit").setExecutor(cmds);
		applog.log(LOG_LEVEL,"Commands Loaded!");
		
	}
	
	@Override
	public void onDisable() {
		game.getTeams().unregisterAll();
	}
	
	private void loadCustomItems() {
		items.put("healing", new HealingWand());
		items.put("grappler", new GrapplingHook());
		items.put("trash", new Trash());
		items.put("sniper", new Sniper());
		items.put("steak", new Steak());
		items.put("kit", new KitSelect());
		items.put("team", new TeamSelect());
		items.put("grenade", new Grenade());
		items.put("rewind", new RewindClock());
		items.put("flare", new Flare());
		items.put("landmine", new Landmine());
		items.put("bloodsucker", new Bloodsucker());
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
		
		if (ReadWrite.getSabKits(gameName)!=null) {
			applog.log(LOG_LEVEL,"Loading Kits!");
			SabKits kits = ReadWrite.getSabKits(gameName);
			game.setKits(kits);
			applog.log(LOG_LEVEL,"Kits Loaded: " + kits);
		} else {
			applog.log(LOG_LEVEL,"Creating Kits!");
		}
		
		//game.setup();
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

	public Game getGame() {
		return game;
	}
	public HashMap<String, CustomItem> getCustomItems() {
		return items;
	}
}
