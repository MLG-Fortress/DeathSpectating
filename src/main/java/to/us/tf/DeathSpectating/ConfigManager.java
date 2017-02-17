package to.us.tf.DeathSpectating;

import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created on 2/16/2017.
 *
 * @author RoboMWM
 */
public class ConfigManager
{
    private DeathSpectating instance;
    private FileConfiguration config;
    private long respawnTicks = 160L; //8 seconds
    private Set<World> whitelistedWorlds;
    private Set<String> whitelistedCommands = new HashSet<>();
    private boolean usePermissionForSpectating = false;

    ConfigManager(DeathSpectating deathSpectating)
    {
        instance = deathSpectating;
        config = instance.getConfig();
        config.addDefault("respawnTimeInSeconds", 8);
        config.addDefault("usePermissionForSpectating", false);
        config.addDefault("useWorldWhitelist", false);
        List<String> whitelist = new ArrayList<>();
        for (World world : instance.getServer().getWorlds())
            whitelist.add(world.getName());
        config.addDefault("worldWhitelist", whitelist);
        List<String> cmdWhitelist = new ArrayList<>(Arrays.asList("me", "m", "msg", "message", "t", "tell", "w", "whisper", "list"));
        config.addDefault("commandWhitelist", cmdWhitelist);
        config.options().copyDefaults(true);
        instance.saveConfig();
        respawnTicks = (long)(config.getDouble("respawnTimeInSeconds") / 20D);
        usePermissionForSpectating = config.getBoolean("usePermissionForSpectating");
        if (config.getBoolean("useWorldWhitelist"))
        {
            whitelistedWorlds = new HashSet<>();
            for (String worldName : config.getStringList("worldWhitelist"))
            {
                World world = instance.getServer().getWorld(worldName);
                if (world == null)
                    instance.getLogger().warning("World " + worldName + " does not exist. We advise removing this from the worldWhitelist in the config for DeathSpectating");
                else
                    whitelistedWorlds.add(world);
            }
        }
        for (String command : config.getStringList("commandWhitelist"))
        {
            whitelistedCommands.add(command);
        }
    }

    public long getRespawnTicks()
    {
        return respawnTicks;
    }

    public boolean canSpectatorTeleport(Player player)
    {
        return player.hasPermission("deathspectating.teleport");
    }

    public boolean isWhitelistedCommand(String command)
    {
        return whitelistedCommands.contains(command);
    }

    public boolean canSpectate(Player player)
    {
        return isWhitelistedWorld(player.getWorld()) && hasPermissionToSpectate(player);
    }

    /*Private methods, for now*/

    private boolean isWhitelistedWorld(World world)
    {
        return whitelistedWorlds == null || whitelistedWorlds.contains(world);
    }

    private boolean hasPermissionToSpectate(Player player)
    {
        return !usePermissionForSpectating || player.hasPermission("deathspectating.spectate");
    }

}
