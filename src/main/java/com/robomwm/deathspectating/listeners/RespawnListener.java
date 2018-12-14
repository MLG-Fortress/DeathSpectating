package com.robomwm.deathspectating.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import com.robomwm.deathspectating.DeathSpectating;

/**
 * Created on 4/10/2017.
 *
 * Primarily to repair cases when other plugins interfere with DeathSpectating
 * (usually minigames firing player.spigot().respawn() and teleporting/setting stuff on player in death event?)
 *
 * TODO: I don't think this is needed anymore...?
 *
 * @author RoboMWM
 */
public class RespawnListener implements Listener
{
    private DeathSpectating instance;

    public RespawnListener(DeathSpectating deathSpectating)
    {
        instance = deathSpectating;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    void onPlayerRespawns(PlayerRespawnEvent event)
    {
        Player player = event.getPlayer();

        //If not previously death spectating, or if we are doing the respawning, ignore
        if (!player.hasMetadata("DEAD") || player.hasMetadata("DS_RESPAWN"))
            return;

        //Otherwise, some other plugin screwed around (e.g. most minigame plugins).
        player.removeMetadata("DEAD", instance);
        player.setFlySpeed(0.1f);
    }
}
