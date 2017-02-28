package to.us.tf.DeathSpectating;

import org.bukkit.Bukkit;

/**
 * Created on 2/27/2017.
 *
 * @author RoboMWM
 */
public class CompatUtil
{
    public static boolean isNewer()
    {
        String version = Bukkit.getBukkitVersion();
        version = version.substring(2);
        version = version.substring(0, version.indexOf("."));
        int versionNumber;
        try
        {
            versionNumber = Integer.valueOf(version);
        }
        catch (Exception e)
        {
            Bukkit.getLogger().warning("[DeathSpectating] Was not able to determine bukkit version.");
            return false;
        }
        return versionNumber > 11;
    }
}
