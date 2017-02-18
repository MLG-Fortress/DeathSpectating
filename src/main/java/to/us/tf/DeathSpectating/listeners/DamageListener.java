package to.us.tf.DeathSpectating.listeners;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import to.us.tf.DeathSpectating.DeathSpectating;

/**
 * Created on 2/15/2017.
 * @author RoboMWM
 */
public class DamageListener implements Listener
{
    DeathSpectating instance;

    public DamageListener(DeathSpectating deathSpectating)
    {
        instance = deathSpectating;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    void onPlayerBasicallyWouldBeDead(EntityDamageEvent event)
    {
        if (event.getEntityType() != EntityType.PLAYER)
            return;

        Player player = (Player)event.getEntity();

        if (!instance.getConfigManager().canSpectate(player, event.getCause()))
            return;

        //Check if player would be dead or not because of this
        if (player.getHealth() > event.getFinalDamage())
            return;

        //Ignore if this is probably the result of the Essentials suicide command
        //Essentials will perform Player#setHealth(0), which does not fire a damage event, but does kill the player. This will lead to a double death message.
        if ((event.getCause() == EntityDamageEvent.DamageCause.CUSTOM || event.getCause() == EntityDamageEvent.DamageCause.SUICIDE)
                && event.getDamage() == Short.MAX_VALUE)
            return;

        player.setLastDamageCause(event);

        /*Put player in death spectating mode*/
        if (instance.startDeathSpectating(player))
            //Cancel event so player doesn't actually die
            event.setCancelled(true);
    }
}
