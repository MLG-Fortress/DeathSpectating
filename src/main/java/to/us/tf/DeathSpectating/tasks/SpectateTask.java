package to.us.tf.DeathSpectating.tasks;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import to.us.tf.DeathSpectating.DeathSpectating;

import javax.annotation.Nullable;

/**
 * Created on 2/15/2017.
 * @author RoboMWM
 */

public class SpectateTask extends BukkitRunnable
{
    private DeathSpectating instance;
    private Player player;
    private Entity killer;
    private long ticks;
    private Vector vector;

    public SpectateTask(Player player, long ticks, @Nullable Entity killer, DeathSpectating deathSpectating)
    {
        this.player = player;
        this.ticks = ticks;
        this.instance = deathSpectating;
        this.killer = killer;
        //Point down by default https://bukkit.org/threads/vectors.152310/#post-1703396
        //if (killer == null)
        //    vector = player.getLocation().subtract(player.getLocation().add(0, 1, 0).toVector()).toVector();
    }

    public Player getPlayer()
    {
        return player;
    }

    public void setTicks(long ticks)
    {
        this.ticks = ticks;
    }

    public long getTicks()
    {
        return ticks;
    }

    public void setKiller(Entity killer)
    {
        this.killer = killer;
    }

    public Entity getKiller()
    {
        return killer;
    }

    public void run()
    {
        if (player.isDead() && instance.isSpectating(player)) //A plugin (e.g. Essentials) did Player#setHealth(0)
            instance.setSpectating(player, false, null);

        if (!instance.isSpectating(player))
        {
            this.cancel();
            return;
        }

        if (ticks < 1)
        {
            instance.respawnPlayer(player);
            this.cancel();
            return;
        }

        //Track killer
        if (killer != null && killer.isValid() && !killer.isDead() && killer.getWorld() == player.getWorld())
        {
            vector = killer.getLocation().toVector().subtract(player.getLocation().toVector());
            player.teleport(player.getLocation().setDirection(vector));
        }

        //player.setSpectatorTarget(player);
        player.setFlySpeed(0f); //does this even work for spectators?

        ticks--;
    }
}
