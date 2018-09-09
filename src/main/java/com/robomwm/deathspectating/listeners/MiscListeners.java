package com.robomwm.deathspectating.listeners;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import com.robomwm.deathspectating.ConfigManager;
import com.robomwm.deathspectating.DeathSpectating;

/**
 * Created on 2/15/2017.
 * @author RoboMWM
 * Primarily to stop death spectators from doing stuff
 * If you wish to override, simply check if the event is canceled and the player has "DEAD" metadata before doing w/e you want to do
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

        //Only allow "death spectating" teleports to occur
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.PLUGIN && event.getPlayer().hasMetadata("DS_TP"))
        {
            event.getPlayer().removeMetadata("DS_TP", instance);
            return;
        }

        event.setCancelled(true);
    }

    //Some plugins trigger abilities if the player sneaks, even in spectating gamemode
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    void onPlayerTryToSneakWhenDead(PlayerToggleSneakEvent event)
    {
        if (instance.isSpectating(event.getPlayer()) && event.isSneaking())
            event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW) //Set to LOW to allow plugins to override without sending denial message
    void onPlayerTryToRunCommandWhenDead(PlayerCommandPreprocessEvent event)
    {
        if (instance.isSpectating(event.getPlayer()) && !configManager.isAllowedToUseAnyCommand(event.getPlayer()))
        {
            String command = event.getMessage().split(" ")[0]; //Got a more efficient/better way? Let me know/PR it!
            command = command.substring(1); //Remove slash
            if (!configManager.isWhitelistedCommand(command))
            {
                event.setCancelled(true);
                if (!configManager.getCommandDeniedMessage().isEmpty())
                    event.getPlayer().sendMessage(configManager.getCommandDeniedMessage());
            }
        }
    }


    //Players cannot quit and remain death spectating; instantly respawn players that quit while death spectating
    @EventHandler(priority = EventPriority.LOW) //Set to low to allow plugins to operate on ragequitters
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

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private void onPlayerSwingHand(PlayerAnimationEvent event)
    {
        if (instance.isSpectating(event.getPlayer()))
            event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    private void onPlayerTryToInteractWhenDead(PlayerInteractEvent event)
    {
        if (instance.isSpectating(event.getPlayer()))
            event.setCancelled(true);
    }

    //This is unnecessary and may remove, but will add it here for now just in case
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    private void onPlayerPickupWhenDead(EntityPickupItemEvent event)
    {
        if (event.getEntityType() != EntityType.PLAYER)
            return;
        Player player = (Player)event.getEntity();
        if (instance.isSpectating(player))
        {
            instance.getLogger().warning("Somehow " + player.getName() + " picked up an item while dead. We stopped this but we thought this wasn't possible. Let RoboMWM know!");
            event.setCancelled(true);
        }
    }
}
