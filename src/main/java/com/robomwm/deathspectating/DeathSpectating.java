package com.robomwm.deathspectating;

import com.robomwm.deathspectating.events.DeathSpectatingEvent;
import com.robomwm.deathspectating.listeners.DeathListener;
import com.robomwm.deathspectating.listeners.MiscListeners;
import com.robomwm.deathspectating.tasks.SpectateTask;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.Statistic;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

/**
 * @author RoboMWM
 * Created on 2/15/2017.
 */
public class DeathSpectating extends JavaPlugin implements Listener
{
    private ConfigManager configManager;

    public void onEnable()
    {
        configManager = new ConfigManager(this);
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new DeathListener(this), this);
        getServer().getPluginManager().registerEvents(new MiscListeners(this), this);
    }

    public ConfigManager getConfigManager()
    {
        return configManager;
    }

    public boolean isSpectating(Player player)
    {
        return player.getGameMode() == GameMode.SPECTATOR && player.hasMetadata("DEAD");
    }

    /**
     * Tags/untags a player as spectating ("DEAD")
     * Also automatically sets/resets spectating attributes
     * @param player
     * @param spectate
     * */
    public boolean setSpectating(Player player, boolean spectate, GameMode gameMode)
    {
        if (spectate)
        {
            player.setGameMode(GameMode.SPECTATOR);
            if (player.getGameMode() != GameMode.SPECTATOR)
            {
                getLogger().warning("Another plugin prevented the player from entering the spectator gamemode!");
                return false;
            }
            player.setMetadata("DEAD", new FixedMetadataValue(this, gameMode));
            player.setFlySpeed(0.0f);
        }
        else
        {
            if (gameMode == null && player.hasMetadata("DEAD"))
                gameMode = (GameMode)player.getMetadata("DEAD").get(0).value();
            player.removeMetadata("DEAD", this);
            if (gameMode == null)
                gameMode = getServer().getDefaultGameMode();
            player.setGameMode(gameMode);
            player.setFlySpeed(0.1f);
        }
        return true;
    }

    public boolean respawnPlayer(Player player)
    {
        if (!isSpectating(player) || player.isDead())
            return false;

        //TODO: Non-Vanilla behavior - can't determine whether to tell player their "bed is missing or obstructed" (Issue #12)
        //Reason: No non-CB/NMS way to determine if player has set a bed spawn before or not.
        //There is respawnLocation or similar method inside of CraftPlayer, including whether "bed spawn" is forced or not

        Location spawnLocation = player.getBedSpawnLocation();
        boolean bedSpawn = true;

        //http://i.minidigger.me/2017/02/idea64_16_17-53-36.png
        //i.e., bedLocation will be null if obstructed
        //CB sets respawnLocation as per below if this is the case
        if (spawnLocation == null)
        {
            spawnLocation = getServer().getWorlds().get(0).getSpawnLocation();
            bedSpawn = false;
        }

        //Indicate that we are doing the respawning (in case plugins want to know if this is a DeathSpectating respawn)
        player.setMetadata("DS_RESPAWN", new FixedMetadataValue(this, true));

        /*Fire PlayerRespawnEvent*/
        //Player#isDead() == true when PlayerRespawnEvent is fired, hence why we do it now.
        PlayerRespawnEvent respawnEvent = new PlayerRespawnEvent(player, spawnLocation, bedSpawn);
        getServer().getPluginManager().callEvent(respawnEvent);

        //CB calls entityPlayer#reset, so this is what we're mimicking

        try
        {
            player.setHealth(player.getMaxHealth()); //literally same line
            player.setFireTicks(0); //fireTicks = 0
            player.setFallDistance(0); //fallDistance = 0
            player.setFoodLevel(20); //foodData = new FoodMetaData(this)
            player.setSaturation(5f);
            player.setExhaustion(0);
            player.setTicksLived(1); //deathTicks = 0, but this must be at least 1. So maybe this refers to something else...
            try
            {
                player.setArrowsStuck(0); //setArrowCount(0) //paper only
            }
            catch (Throwable ignored){}
            for (PotionEffect potionEffect : player.getActivePotionEffects())
                player.removePotionEffect(potionEffect.getType());
            player.closeInventory();
            player.setLastDamageCause(null); //combatTracker = new CombatTracker(this)
            //Experience is handled in startDeathSpectating, especially since RespawnEvent contains no EXP data.
        }
        catch (Throwable rock)
        {
            rock.printStackTrace();
        }

        /*undo spectating attributes*/
        //This is done before teleporting because:
        //CB fires WorldChangedEvent after player.dead = false;
        //in case a teleport/worldchange event handler wants to set other gamemodes instead (like Multiverse)
        //Oh, and also because the MiscListeners would cancel this teleport otherwise
        setSpectating(player, false, getConfigManager().gameModeToRespawnWith());

        //Teleport player to event#getRespawnLocation
        //CB doesn't nullcheck it seems, so neither will I
        //CB doesn't fire a teleport event (but does fire worldchange), so we'll just set cause as unknown
        player.teleport(respawnEvent.getRespawnLocation(), PlayerTeleportEvent.TeleportCause.UNKNOWN);
        player.removeMetadata("DS_RESPAWN", this);
        return true;
    }

    /**
     * "Kill" player and put them in death spectating mode
     * @param player
     * @return if player was put in death spectating mode
     */
    public boolean startDeathSpectating(Player player, PlayerDeathEvent deathEvent)
    {
        if (isSpectating(player))
            return false;

        try
        {
            boolean keepInventory = deathEvent.getKeepInventory() || player.getGameMode() == GameMode.SPECTATOR;
            boolean showDeathMessages = player.getWorld().getGameRuleValue(GameRule.SHOW_DEATH_MESSAGES);

            /*Set spectating attributes*/
            //Player#isDead() == true when PlayerDeathEvent is fired.
            //Also prevents any potential to pickup anything that's dropped.
            if (!setSpectating(player, true, player.getGameMode()))
                return false;

            /*Start Death Event simulation*/

            //+phoenix616: RoboMWM: it will drop level * 7 exp and 100 as a maximum
            //see https://minecraft.gamepedia.com/Health#Death
            int expToDrop = 0;

            List<ItemStack> itemsToDrop = new ArrayList<>(player.getInventory().getSize());
            if (!keepInventory)
            {
                //Compile a list of null-free/air-free items to drop


                //Calculate and set experience to drop
                expToDrop = Math.min(100, SetExpFix.getTotalExperience(player));
            }

            //TODO: Non-vanilla behavior: scoreboard coloring of death message if unmodified. see issue #5 //May not apply anymore though.
            //Print death message
            if (deathEvent.getDeathMessage() != null && !deathEvent.getDeathMessage().isEmpty() && showDeathMessages)
                getServer().broadcastMessage(deathEvent.getDeathMessage());

            //Clear and drop items if keepInventory == false
            if (!deathEvent.getKeepInventory())
            {
                player.getInventory().clear();

                //drop items
                for (ItemStack itemStack : deathEvent.getDrops())
                {
                    if (itemStack != null && itemStack.getType() != Material.AIR)
                        player.getWorld().dropItemNaturally(player.getLocation(), itemStack);
                }

                //Clear and set experience, if getKeepLevel() == false
                //Note that Exp is kept if either keepLevel _or_ keepInventory is true
                //Technically this is done on respawn, but PlayerRespawnEvent doesn't carry this info - CB stores the `keepLevel` boolean in the Player object apparently?
                if (!deathEvent.getKeepLevel())
                {
                    SetExpFix.setTotalExperience(player, 0);
                    player.setTotalExperience(deathEvent.getNewTotalExp());
                    player.setLevel(deathEvent.getNewLevel());
                    player.setExp(deathEvent.getNewExp());
                }
            }

            //Close any inventory the player may be viewing
            //CB checks to see if player is currently viewing another inventory on death before attempting to close on the player. I'm assuming it will do this check with this Bukkit call though.
            player.closeInventory();
            player.setSpectatorTarget(player); //Remove spectated target

            //Drop experience
            if (deathEvent.getDroppedExp() > 0)
                player.getWorld().spawn(player.getLocation(), ExperienceOrb.class).setExperience(deathEvent.getDroppedExp());

            //Increment/reset death statistics
            player.incrementStatistic(Statistic.DEATHS);
            player.setStatistic(Statistic.TIME_SINCE_DEATH, 0);

            //Clear potion effects
            for (PotionEffect potionEffect : player.getActivePotionEffects())
                player.removePotionEffect(potionEffect.getType());

            //TODO: Non-vanilla behavior: Player death animation (red and falling over) (Issue #13)
            //Smoke effect //TODO: after 20 ticks (Issue #14) Will implement 20 tick delay after issue #13 is resolved
            player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 25, 1, 0.5, 1, 0.001);

            //Play the "death" sound (to all other players except the killed player; vanilla (spigot?) behavior).
            //fyi, default resource pack doesn't have a different sound for this; only custom resource packs make use of this.
            //TODO: distance check?
            Set<Player> players = new HashSet<>(player.getWorld().getPlayers());
            players.remove(player);
            for (Player p : players)
                p.playSound(player.getLocation(), Sound.ENTITY_PLAYER_DEATH, SoundCategory.PLAYERS, 1.0f, 1.0f);

            /* End Death simulation*/

            //Determine what entity killed this player (Entity#getKiller can only return a Player)
            Entity killer = player.getKiller();
            if (player.getKiller() == null && player.getLastDamageCause() instanceof EntityDamageByEntityEvent)
            {
                killer = ((EntityDamageByEntityEvent) player.getLastDamageCause()).getDamager();
                if (killer != null && killer instanceof Projectile)
                {
                    Projectile arrow = (Projectile) killer;
                    if (arrow.getShooter() instanceof LivingEntity)
                        killer = (Entity) arrow.getShooter();
                }
            }
            if (killer == player) //Though we don't care if they did it themselves (don't count as player kill stat)
                killer = null;

            //Increment player's ENTITY_KILLED_BY if killer is an entitytype recorded by this statistic
            if (killer != null)
            {
                try //The list of entities this statistic applies to is arbitrary and may change with future MC updates - so I will stick to eating exceptions here.
                {
                    player.incrementStatistic(Statistic.ENTITY_KILLED_BY, killer.getType());
                }
                catch (IllegalArgumentException ignored) {} // "The supplied EntityType does not have a corresponding statistic"
                catch (NullPointerException ignored) {} // Generally occurs when a player killed another player
            }

            //Increment _killer's_ PLAYER_KILLS
            if (killer != null && killer.getType() == EntityType.PLAYER)
            {
                Player playerKiller = (Player)killer;
                playerKiller.incrementStatistic(Statistic.PLAYER_KILLS);
            }

            /*Start death spectating!*/
            SpectateTask task = new SpectateTask(player, configManager.getRespawnTicks(), killer, this);
            getServer().getPluginManager().callEvent(new DeathSpectatingEvent(task));
            task.runTaskTimer(this, 1L, 1L);

            //Send player a message that they were killed and are now spectating, if configured to do so
            if (!configManager.getYouDiedMessage().isEmpty())
                player.sendMessage(configManager.getYouDiedMessage());

            return true;
        }
        catch (Exception e)
        {
            getLogger().log(Level.SEVERE, "An error occurred while trying to start death spectating for " + player.getName());
            getLogger().log(Level.SEVERE, "DeathSpectating v" + getDescription().getVersion() + " running on " + getServer().getVersion());
            getLogger().log(Level.SEVERE, "Report the following stacktrace in full:\n");
            e.printStackTrace();
            setSpectating(player, false, null);
            return false;
        }
    }
}
