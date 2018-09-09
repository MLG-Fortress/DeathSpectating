package com.robomwm.deathspectating.listeners;

import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import com.robomwm.deathspectating.DeathSpectating;

/**
 * Created on 2/15/2017.
 * @author RoboMWM
 */
public class DeathListener implements Listener
{
    private DeathSpectating plugin;

    public DeathListener(DeathSpectating deathSpectating)
    {
        plugin = deathSpectating;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    void onPlayerDies(PlayerDeathEvent event)
    {
        Player player = event.getEntity();

        if (plugin.isSpectating(player))
            return;

        if (!plugin.getConfigManager().canSpectate(player, player.getLastDamageCause().getCause()))
            return;

        /*Put player in death spectating mode*/
        if (plugin.startDeathSpectating(player, event))
        {
            //Cancel event so player doesn't actually die
            event.setCancelled(true);

            //Play the "hit" sound (since we canceled the event, the hit sound will not play)
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
        }
    }
}
