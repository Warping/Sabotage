package bubbles.sabotage.plugin.counter;

import bubbles.sabotage.plugin.Main;

public class Counter implements Runnable {

	private int taskId;
	private Main plugin = Main.getPlugin(Main.class);

    public Counter(long length) {
    	taskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, this, 0L, length);
    }

    public void cancel() {
        plugin.getServer().getScheduler().cancelTask(taskId);
    }

	@Override
	public void run() {
		
	}

}
