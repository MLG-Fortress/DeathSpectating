package us.to.tf.DeathSpectating.tasks;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import us.to.tf.DeathSpectating.DeathSpectating;

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
        if (!instance.isSpectating(player))
        {
            this.cancel();
            return;
        }

        if (ticks < 2)
        {
            instance.respawnPlayer(player);
            this.cancel();
            return;
        }

        //Track killer
        if (!player.isDead() && killer != null && killer.getWorld() == player.getWorld())
        {
            vector = killer.getLocation().toVector().subtract(player.getLocation().toVector());
            player.teleport(player.getLocation().setDirection(vector));
        }

        ticks--;
    }
}
