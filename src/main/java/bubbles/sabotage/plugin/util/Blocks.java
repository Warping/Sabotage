package bubbles.sabotage.plugin.util;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class Blocks {
    public boolean isBlockBetween(Location loc1, Location loc2) {
        World world = loc1.getWorld();
        if (world == null || !world.equals(loc2.getWorld())) return false;

        // Calculate the direction and distance from loc1 to loc2
        Vector direction = loc2.toVector().subtract(loc1.toVector());
        double distance = direction.length();

        // If locations are the same or too close, no block can be between them
        if (distance < 0.001) return false;

        // Perform the ray trace looking for blocks (ignoring air and fluids)
        RayTraceResult result = world.rayTraceBlocks(loc1, direction.normalize(), distance);

        // If the result is not null, a block was hit along the path
        return result != null && result.getHitBlock() != null;
    }
}
