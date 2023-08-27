package bubbles.sabotage.plugin.fileIO;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import bubbles.sabotage.plugin.Bomb;
import bubbles.sabotage.plugin.PlayerData;
import bubbles.sabotage.plugin.game.Game;
import bubbles.sabotage.plugin.groups.SabKits;

public class ReadWrite {

	private static void saveConfig(Game game, FileConfiguration config, String filename) {
        try {
            config.save("plugins/Sabotage/" + game.getName() + "/" + filename + ".yml");
        } catch (IOException e) {
        	System.out.println("failed!");
            e.printStackTrace();
        }
	}
	
	public static void saveSabKits(Game game) {
		FileConfiguration config = new YamlConfiguration();
		config.set("Sabotage Kits", game.getKits());
		saveConfig(game, config, "kits");
	}
	
	public static SabKits getSabKits(String game) {
		FileConfiguration config = YamlConfiguration.loadConfiguration(new File("plugins/Sabotage/" + game + "/kits.yml"));
		return (SabKits) config.get("Sabotage Kits");
	}
	
	public static void saveSabBombs(Game game) {
		FileConfiguration config = new YamlConfiguration();
		config.set("Sabotage Bombs", game.getBombs());
		saveConfig(game, config, "bombs");
	}
	
	@SuppressWarnings("unchecked")
	public static ArrayList<Bomb> getSabBombs(String game) {
		FileConfiguration config = YamlConfiguration.loadConfiguration(new File("plugins/Sabotage/" + game + "/bombs.yml"));
		return (ArrayList<Bomb>) config.get("Sabotage Bombs");
	}
	
	public static void saveSabGame(Game game) {
		FileConfiguration config = new YamlConfiguration();
		config.set("Sabotage Game", game);
		saveConfig(game, config, game.getName());
	}
	
	public static Game getSabGame(String game) {
		FileConfiguration config = YamlConfiguration.loadConfiguration(new File("plugins/Sabotage/" + game + "/" + game + ".yml"));
		return (Game) config.get("Sabotage Game");
	}
	
	public static void saveSabPlayerData(Game game) {
		FileConfiguration config = new YamlConfiguration();
		config.set("Sabotage Players", game);
		saveConfig(game, config, "playerData");
	}
	
	public static ArrayList<PlayerData> getSabPlayerData(String game) {
		FileConfiguration config = YamlConfiguration.loadConfiguration(new File("plugins/Sabotage/" + game + "/" + "playerData.yml"));
		
		return null;
	}
	
}
