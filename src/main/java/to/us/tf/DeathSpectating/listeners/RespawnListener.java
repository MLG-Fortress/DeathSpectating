package to.us.tf.DeathSpectating.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import to.us.tf.DeathSpectating.DeathSpectating;

/**
 * Created on 4/10/2017.
 *
 * Primarily to repair cases when other plugins interfere with DeathSpectating (usually minigames)
 *
 * @author RoboMWM
 */
public class RespawnListener implements Listener
{
    DeathSpectating instance;

    public RespawnListener(DeathSpectating deathSpectating)
    {
        instance = deathSpectating;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
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
