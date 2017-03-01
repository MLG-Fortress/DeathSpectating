package to.us.tf.DeathSpectating.listeners;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.PlayerInventory;
import to.us.tf.DeathSpectating.CompatUtil;
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

        //Ignore if player will survive this damage
        if (player.getHealth() > event.getFinalDamage())
            return;

        //Ignore if player is holding a totem of undying
        PlayerInventory inventory = player.getInventory();
        try
        {
            if (inventory.getItemInMainHand().getType() == Material.TOTEM || inventory.getItemInOffHand().getType() == Material.TOTEM)
                return;
        }
        catch (NoSuchFieldError | NoSuchMethodError e) //TOTEM (not in 1.10 and below) //getItemInMainHand, etc. (not in 1.8 and below)
        {
            if (CompatUtil.isNewer()) throw e;
        }

        //Ignore if this is probably the result of the Essentials suicide command
        //Essentials will perform Player#setHealth(0), which does not fire a damage event, but does kill the player. This will lead to a double death message.
        if ((event.getCause() == EntityDamageEvent.DamageCause.CUSTOM || event.getCause() == EntityDamageEvent.DamageCause.SUICIDE)
                && event.getDamage() == Short.MAX_VALUE)
            return;

        player.setLastDamageCause(event);
        //TODO: fire EntityResurrectEvent

        /*Put player in death spectating mode*/
        if (instance.startDeathSpectating(player))
            //Cancel event so player doesn't actually die
            event.setCancelled(true);
    }
}
