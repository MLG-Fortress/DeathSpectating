package to.us.tf.DeathSpectating.tasks;

import org.bukkit.Location;
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
    private String unformattedTitle;
    private String unformattedSubTitle;
    private int score;
    private boolean preventMovement = true;
    private Location deathLocation;

    public SpectateTask(Player player, long ticks, @Nullable Entity killer, DeathSpectating deathSpectating)
    {
        this.player = player;
        this.ticks = ticks;
        this.instance = deathSpectating;
        this.killer = killer;
        //Point down by default https://bukkit.org/threads/vectors.152310/#post-1703396
        //if (killer == null)
        //    vector = player.getLocation().subtract(player.getLocation().add(0, 1, 0).toVector()).toVector();

        this.unformattedTitle = deathSpectating.getConfigManager().getDeathTitle("titles");
        this.unformattedSubTitle = deathSpectating.getConfigManager().getDeathTitle("subtitles");
        this.score = player.getTotalExperience();
        this.deathLocation = player.getLocation();
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

    public String getUnformattedTitle()
    {
        return unformattedTitle;
    }

    public void setUnformattedTitle(String unformattedTitle)
    {
        if (unformattedTitle == null)
            unformattedTitle = "";
        this.unformattedTitle = unformattedTitle;
    }

    public String getUnformattedSubTitle()
    {
        return unformattedSubTitle;
    }

    public void setUnformattedSubTitle(String unformattedSubTitle)
    {
        if (unformattedSubTitle == null)
            unformattedSubTitle = "";
        this.unformattedSubTitle = unformattedSubTitle;
    }

    public boolean hasPreventMovement()
    {
        return preventMovement;
    }

    public void setPreventMovement(boolean preventMovement)
    {
        this.preventMovement = preventMovement;
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

        if (ticks % 10 == 0 && !(unformattedTitle.isEmpty() && unformattedSubTitle.isEmpty()))
        {
            int seconds = (int)ticks / 20;
            String title = instance.getConfigManager().formatter(unformattedTitle, seconds, score);
            String subTitle = instance.getConfigManager().formatter(unformattedSubTitle, seconds, score);
            player.sendTitle(title, subTitle, 0, 20, 20); //Could use paper's more robust Title API
        }

        if (ticks < 1)
        {
            instance.respawnPlayer(player);
            player.sendTitle(" ", " ", 0, 1, 0); //resetTitle only seems to reset title, not both title and subtitle
            player.resetTitle();
            this.cancel();
            return;
        }

        //Track killer
        if (preventMovement && killer != null && killer.isValid() && !killer.isDead() && killer.getWorld() == player.getWorld())
        {
            vector = killer.getLocation().toVector().subtract(deathLocation.toVector());
            player.teleport(deathLocation.setDirection(vector));
            player.setFlySpeed(0f);
            player.setSpectatorTarget(null);
        }
        else if (preventMovement)
        {
            if (player.getLocation().distanceSquared(deathLocation) > 0.5)
                player.teleport(deathLocation.setDirection(player.getLocation().getDirection()));
            player.setFlySpeed(0f);
            player.setSpectatorTarget(null);
        }

        ticks--;
    }
}
