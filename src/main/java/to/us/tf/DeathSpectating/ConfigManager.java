package to.us.tf.DeathSpectating;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

import javax.annotation.Nonnull;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

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
    private Map<String, String> messages = new HashMap<>();
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
        List<String> dCBL = new ArrayList<>(Arrays.asList("SUFFOCATION"));
        config.addDefault("damageCauseBlacklist", dCBL);
        config.addDefault("useWorldWhitelist", false);
        List<String> whitelist = new ArrayList<>();
        for (World world : instance.getServer().getWorlds())
            whitelist.add(world.getName());
        config.addDefault("worldWhitelist", whitelist);
        config.addDefault("commandWhitelist", new ArrayList<>(Arrays.asList("me", "m", "msg", "message", "t", "tell", "w", "whisper", "list", "help", "?", "info", "report")));

        config.options().copyDefaults(true);
        instance.saveConfig();

        respawnTicks = (long)(config.getDouble("respawnTimeInSeconds") * 20L);
        usePermissionForSpectating = config.getBoolean("usePermissionForSpectating");
        if (config.getBoolean("useDamageCauseBlacklist"))
        {
            for (String causeString : config.getStringList("damageCauseBlacklist"))
            {
                try
                {
                    blacklistedDamageCauses.add(EntityDamageEvent.DamageCause.valueOf(causeString.toUpperCase()));
                }
                catch (IllegalArgumentException e)
                {
                    instance.getLogger().warning(causeString + " is not a valid DamageCause. See documentation for a list of valid DamageCauses");
                }

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

        //Messages
        ConfigurationSection messageSection = config.getConfigurationSection("messages");
        if (messageSection == null)
            messageSection = config.createSection("messages");

        if (messageSection.getString("spectating") == null)
            messageSection.set("spectating", "&cYou died! Respawning in {0} seconds.");
        messages.put("spectating", formatter(messageSection.getString("spectating"), (respawnTicks / 20L)));
        if (messageSection.getString("deniedCommand") == null)
            messageSection.set("deniedCommand", "&cYou are not allowed to use that command while death spectating.");
        messages.put("deniedCommand", formatter(messageSection.getString("deniedCommand")));

        //Title messages
        ConfigurationSection titleSection = config.getConfigurationSection("titleMessages");
        if (titleSection == null)
            titleSection = config.createSection("titleMessages");

        if (titleSection.getStringList("titles") == null)
            titleSection.set("title", new ArrayList<>(Arrays.asList("&cYou died!", "&cGame over!")));
        if (titleSection.getStringList("subtitles") == null)
            titleSection.set("subtitle", new ArrayList<>(Arrays.asList("Respawning in {0}", "Score: &e{1}", "Score: &e{1}&f, Respawning in {0}")));

        instance.saveConfig();
    }

    public String getDeathTitle(@Nonnull String titleType)
    {
        List<String> hi = config.getConfigurationSection("titleMessages").getStringList(titleType);
        return hi.get(ThreadLocalRandom.current().nextInt(hi.size()));
    }

    public long getRespawnTicks()
    {
        return respawnTicks;
    }

    public boolean isWhitelistedCommand(String command)
    {
        return whitelistedCommands.contains(command);
    }

    public boolean isAllowedToUseAnyCommand(Player player)
    {
        return player.hasPermission("deathspectating.commands");
    }

    public String getCommandDeniedMessage()
    {
        return messages.get("deniedCommand");
    }

    public String getYouDiedMessage()
    {
        return messages.get("spectating");
    }

    public boolean canSpectate(Player player, EntityDamageEvent.DamageCause damageCause)
    {
        return damageCause != null && isWhitelistedWorld(player.getWorld()) && !blacklistedDamageCauses.contains(damageCause) && hasPermissionToSpectate(player);
    }

    /*Utility methods*/
    public String formatter(String stringToFormat, Object... formatees)
    {
        return formatter(MessageFormat.format(stringToFormat, formatees));
    }

    public String formatter(String stringToFormat)
    {
        return ChatColor.translateAlternateColorCodes('&', stringToFormat);
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
