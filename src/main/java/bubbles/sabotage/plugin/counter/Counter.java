package bubbles.sabotage.plugin.counter;

import bubbles.sabotage.plugin.Main;
import org.bukkit.scheduler.BukkitTask;

public class Counter implements Runnable {

	private BukkitTask task;
	private Main plugin = Main.getPlugin(Main.class);

    public Counter(long length) {
    	task = plugin.getServer().getScheduler().runTaskTimer(plugin, this, 0L, length);
    }

    public void cancel() {
        task.cancel();
    }

	@Override
	public void run() {
		
	}

}
