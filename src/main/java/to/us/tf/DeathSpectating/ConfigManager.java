package to.us.tf.DeathSpectating;

import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

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
    private Set<World> whitelistedWorlds = new HashSet<>();
    private Set<String> whitelistedCommands = new HashSet<>();
    private Set<EntityDamageEvent.DamageCause> blacklistedDamageCauses = new HashSet<>();
    private boolean usePermissionForSpectating = false;

    ConfigManager(DeathSpectating deathSpectating)
    {
        instance = deathSpectating;
        config = instance.getConfig();
        config.addDefault("respawnTimeInSeconds", 8);
        config.addDefault("usePermissionForSpectating", false);
        config.addDefault("useDamageCauseBlacklist", true);
        List<String> dCBL = new ArrayList<>(Arrays.asList("SUFFOCATION", "SUICIDE"));
        config.addDefault("damageCauseBlacklist", dCBL);
        config.addDefault("useWorldWhitelist", false);
        List<String> whitelist = new ArrayList<>();
        for (World world : instance.getServer().getWorlds())
            whitelist.add(world.getName());
        config.addDefault("worldWhitelist", whitelist);
        List<String> cmdWhitelist = new ArrayList<>(Arrays.asList("me", "m", "msg", "message", "t", "tell", "w", "whisper", "list", "help", "?", "info", "report"));
        config.addDefault("commandWhitelist", cmdWhitelist);

        config.options().copyDefaults(true);
        instance.saveConfig();

        respawnTicks = (long)(config.getDouble("respawnTimeInSeconds") * 20L);
        usePermissionForSpectating = config.getBoolean("usePermissionForSpectating");
        if (config.getBoolean("useDamageCauseBlacklist"))
        {
            for (String causeString : config.getStringList("damageCauseBlacklist"))
            {
                EntityDamageEvent.DamageCause damageCause = EntityDamageEvent.DamageCause.valueOf(causeString.toUpperCase());
                if (damageCause == null)
                    instance.getLogger().warning(causeString + " is not a valid DamageCause.");
                else
                    blacklistedDamageCauses.add(damageCause);
            }
        }
        if (config.getBoolean("useWorldWhitelist"))
        {
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

    public boolean canSpectate(Player player, EntityDamageEvent.DamageCause damageCause)
    {
        return damageCause != null && isWhitelistedWorld(player.getWorld()) && !blacklistedDamageCauses.contains(damageCause);
    }

    /*Private methods, for now*/

    private boolean isWhitelistedWorld(World world)
    {
        return whitelistedWorlds.isEmpty() || whitelistedWorlds.contains(world);
    }

    private boolean hasPermissionToSpectate(Player player)
    {
        return !usePermissionForSpectating || player.hasPermission("deathspectating.spectate");
    }

}
