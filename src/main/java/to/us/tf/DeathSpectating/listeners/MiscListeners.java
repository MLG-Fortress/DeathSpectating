package to.us.tf.DeathSpectating.listeners;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import to.us.tf.DeathSpectating.ConfigManager;
import to.us.tf.DeathSpectating.DeathSpectating;

/**
 * Created on 2/15/2017.
 * @author RoboMWM
 * Primarily to stop death spectators from doing stuff
 */
public class MiscListeners implements Listener
{
    DeathSpectating instance;
    ConfigManager configManager;

    public MiscListeners(DeathSpectating deathSpectating)
    {
        instance = deathSpectating;
        configManager = instance.getConfigManager();
        instance.registerListener(this);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    void onPlayerTryToTeleportWhenDead(PlayerTeleportEvent event)
    {
        if (!instance.isSpectating(event.getPlayer()))
            return;
        try
        {
            //Only allow "death spectating" teleports to occur
            if (event.getCause() == PlayerTeleportEvent.TeleportCause.PLUGIN && event.getFrom().distanceSquared(event.getTo()) == 0)
                return;
        }
        catch (IllegalArgumentException e) //If trying to teleport to another world, yes of course stop that
        {}
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    void onPlayerTryToInteractWhenDead(PlayerInteractEvent event)
    {
        if (instance.isSpectating(event.getPlayer()))
            event.setCancelled(true);
    }

    //Some plugins trigger abilities if the player sneaks, even in spectating gamemode
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    void onPlayerTryToSneakWhenDead (PlayerToggleSneakEvent event)
    {
        if (instance.isSpectating(event.getPlayer()) && event.isSneaking())
            event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    void onPlayerTryToRunCommandWhenDead(PlayerCommandPreprocessEvent event)
    {
        if (instance.isSpectating(event.getPlayer()) && !configManager.isAllowedToUseAnyCommand(event.getPlayer()))
        {
            String command = event.getMessage().split(" ")[0]; //Got a more efficient/better way? Let me know/PR it!
            command = command.substring(1, command.length()); //Remove slash
            if (!configManager.isWhitelistedCommand(command))
            {
                event.setCancelled(true);
                if (!configManager.getCommandDeniedMessage().isEmpty())
                    event.getPlayer().sendMessage(configManager.getCommandDeniedMessage());
            }
        }
    }


    //Players cannot quit and remain death spectating; instantly respawn players that quit while death spectating
    @EventHandler(priority = EventPriority.LOWEST)
    void onPlayerQuitWhileSpectatingOrDead(PlayerQuitEvent event)
    {
        instance.respawnPlayer(event.getPlayer());
    }

    //Prevent spectators from taking damage (e.g. void)
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    void onPlayerTakeDamageWhileSpectating(EntityDamageEvent event)
    {
        if (event.getEntityType() != EntityType.PLAYER)
            return;
        if (instance.isSpectating((Player)event.getEntity()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    void onPlayerRegainingHealthWhileSpectating(EntityRegainHealthEvent event)
    {
        if (event.getEntityType() != EntityType.PLAYER)
            return;
        if (instance.isSpectating((Player)event.getEntity()))
            event.setCancelled(true);
    }

}
