package to.us.tf.DeathSpectating.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import to.us.tf.DeathSpectating.tasks.SpectateTask;

/**
 * @author RoboMWM
 * Created on 2/15/2017
 * Primarily for addons to display extra information like respawning time to player
 */
public class DeathSpectatingEvent extends Event
{
    // Custom Event Requirements
    private static final HandlerList handlers = new HandlerList();
    public static HandlerList getHandlerList() {
        return handlers;
    }
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    SpectateTask spectateTask;

    public DeathSpectatingEvent(SpectateTask task)
    {
        this.spectateTask = task;
    }

    public SpectateTask getSpectateTask()
    {
        return spectateTask;
    }
}
