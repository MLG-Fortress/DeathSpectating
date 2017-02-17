package us.to.tf.DeathSpectating;

import org.bukkit.GameMode;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

/**
 * Created on 2/15/2017.
 * @author RoboMWM
 * Primarily to stop death spectators from doing stuff
 * May alter in the future to allow things such as spectating other people, etc.
 */
public class MiscListeners implements Listener
{
    DeathSpectating instance;
    ConfigManager configManager;

    public MiscListeners(DeathSpectating deathSpectating)
    {
        instance = deathSpectating;
        configManager = instance.getConfigManager();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    void onPlayerTryToTeleportWhenDead(PlayerTeleportEvent event)
    {
        if (!instance.isSpectating(event.getPlayer()))
            return;
        if (configManager.canSpectatorTeleport(event.getPlayer()))
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

    //Stops some abilities from being used
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    void onPlayerTryToSneakWhenDead (PlayerToggleSneakEvent event)
    {
        if (instance.isSpectating(event.getPlayer()) && event.isSneaking())
            event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    void onPlayerTryToRunCommandWhenDead(PlayerCommandPreprocessEvent event)
    {
        if (instance.isSpectating(event.getPlayer()))
        {
            String command = event.getMessage().split(" ")[0]; //Got a more efficient/better way? Let me know/PR it!
            command = command.substring(1, command.length()); //Remove slash
            if (configManager.isWhitelistedCommand(command))
                return;
        }
            event.setCancelled(true);
    }

    /**
     * Players cannot quit and remain death spectating
     */
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
        Player player = (Player)event.getEntity();
        if (player.getGameMode() == GameMode.SPECTATOR && instance.isSpectating(player))
        {
            event.setCancelled(true);
        }
    }

}
