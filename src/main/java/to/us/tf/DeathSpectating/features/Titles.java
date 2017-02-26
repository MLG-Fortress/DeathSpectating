package to.us.tf.DeathSpectating.features;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitRunnable;
import to.us.tf.DeathSpectating.ConfigManager;
import to.us.tf.DeathSpectating.DeathSpectating;
import to.us.tf.DeathSpectating.events.DeathSpectatingEvent;
import to.us.tf.DeathSpectating.tasks.SpectateTask;

import java.util.HashMap;
import java.util.Map;

/**
 * Decided to incorporate it straight into this plugin, since CB does have a basic title API
 * Created on 2/26/2017.
 * @author RoboMWM
 */
public class Titles implements Listener
{
    DeathSpectating instance;

    private Map<Player, Integer> scores = new HashMap<>();
    ConfigManager config;

    public Titles(DeathSpectating deathSpectating, ConfigManager config)
    {
        instance = deathSpectating;
        this.config = config;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    void onDeathStoreScore(PlayerDeathEvent event)
    {
        scores.put(event.getEntity(), event.getEntity().getTotalExperience());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    void onQuitEvent(PlayerQuitEvent event)
    {
        //Cleanup just in case
        scores.remove(event.getPlayer());
    }

    @EventHandler
    void onSpectate(DeathSpectatingEvent event)
    {
        new BukkitRunnable()
        {
            SpectateTask spectateTask = event.getSpectateTask();
            String unformattedTitle = config.getDeathTitle("title");
            String unformattedSubTitle = config.getDeathTitle("subtitle");
            int score = scores.remove(spectateTask.getPlayer());

            @Override
            public void run()
            {
                if (spectateTask.getTicks() < 2 || !spectateTask.getPlayer().hasMetadata("DEAD"))
                {
                    this.cancel();
                    return;
                }
                int seconds = (int)spectateTask.getTicks() / 20;
                String title = config.formatter(unformattedTitle, seconds, score);
                String subTitle = config.formatter(unformattedSubTitle, seconds, score);
                spectateTask.getPlayer().sendTitle(title, subTitle, 0, 20, 2); //Could use paper's more robust Title API
            }
        }.runTaskTimer(instance, 2L, 10L);
    }

    @EventHandler
    void onRespawn(PlayerRespawnEvent event)
    {
        event.getPlayer().sendTitle(" ", " ", 1, 1, 1); //resetTitle only seems to reset title, not both title and subtitle
        event.getPlayer().resetTitle();
    }
}
