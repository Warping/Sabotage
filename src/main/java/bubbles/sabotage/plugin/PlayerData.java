package bubbles.sabotage.plugin;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;

@SerializableAs("PlayerData")
public class PlayerData implements ConfigurationSerializable{
	
	private String nametag;
	private String team;
	private String kit;
	private int kills;
	private int deaths;

	public PlayerData(String nametag, String team, String kit, int kills, int deaths) {
		this.nametag = nametag;
		this.team = team;
		this.kit = kit;
		this.kills = kills;
		this.deaths = deaths;
	}
	
	public PlayerData(Map<String, Object> config) {
		this.nametag = (String) config.get("NameTag");
		this.team = (String) config.get("Team");
		this.kit = (String) config.get("Kit");
		this.kills = (int) config.get("Kills");
		this.deaths = (int) config.get("Deaths");
	}
	
	@Override
	public Map<String, Object> serialize() {
		Map<String, Object> map = new HashMap<>();
		map.put("NameTag", nametag);
		map.put("Team", team);
		map.put("Kit", kit);
		map.put("Kills", kills);
		map.put("Deaths", deaths);
		return map;
	}

	public String getNametag() {
		return nametag;
	}

	public void setNametag(String nametag) {
		this.nametag = nametag;
	}

	public String getTeam() {
		return team;
	}

	public void setTeam(String team) {
		this.team = team;
	}

	public String getKit() {
		return kit;
	}

	public void setKit(String kit) {
		this.kit = kit;
	}

	public int getKills() {
		return kills;
	}

	public void setKills(int kills) {
		this.kills = kills;
	}

	public int getDeaths() {
		return deaths;
	}

	public void setDeaths(int deaths) {
		this.deaths = deaths;
	}

	
	

}
