package to.us.tf.DeathSpectating;

import org.bukkit.Bukkit;

/**
 * Created on 2/27/2017.
 *
 * @author RoboMWM
 */
public class CompatUtil
{
    private static Integer serverVersion = null;
    private static int currentVersion = 11;
    public static boolean isNewer()
    {
        return getVersion() > currentVersion;
    }

    public static int getVersion()
    {
        if (serverVersion != null)
            return serverVersion;
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
            return -1;
        }
        serverVersion = versionNumber;
        return versionNumber;
    }

    public static boolean isOlder(int version)
    {
        return getVersion() < version;
    }
}
