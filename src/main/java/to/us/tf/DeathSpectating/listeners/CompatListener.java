package to.us.tf.DeathSpectating.listeners;

import me.robomwm.usefulutil.compat.UsefulCompat;
import me.robomwm.usefulutil.compat.UsefulCompatEvent;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Statistic;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import to.us.tf.DeathSpectating.ConfigManager;
import to.us.tf.DeathSpectating.DeathSpectating;
import to.us.tf.DeathSpectating.SetExpFix;
import to.us.tf.DeathSpectating.events.DeathSpectatingEvent;
import to.us.tf.DeathSpectating.tasks.SpectateTask;

import java.util.ArrayList;
import java.util.List;

/**
 * Created on 3/1/2017.
 *
 * @author RoboMWM
 */
public class CompatListener implements Listener
{
    DeathSpectating instance;

    public CompatListener(DeathSpectating instance)
    {
        if (UsefulCompat.isCurrentOrNewer())
            return;
        instance.registerListener(this);
    }
    @EventHandler(ignoreCancelled = true)
    private void onStartDeath(UsefulCompatEvent event)
    {
        if (event.getCallingPlugin() != instance)
            return;
        event.setCancelled(true);
        switch (event.getIdentifier())
        {
            case "deathspectating":
                compatSpectateCall((Player)event.objects().get(0), (ConfigManager)event.objects().get(0));
                break;
            case "predeathspectating":
                onPreDeath((EntityDamageEvent)event.objects().get(0));
                break;
            default:
                event.setCancelled(false);
        }
    }

    private void compatSpectateCall(Player player, ConfigManager configManager)
    {
        /*Set spectating attributes*/
        //Player#isDead() == true when PlayerDeathEvent is fired.
        instance.setSpectating(player, true, player.getGameMode());

            /*Start Death simulation*/

        boolean keepInventory = Boolean.valueOf(player.getWorld().getGameRuleValue("keepInventory"));
        boolean showDeathMessages = Boolean.valueOf(player.getWorld().getGameRuleValue("showDeathMessages"));

        //+phoenix616: RoboMWM: it will drop level * 7 exp and 100 as a maximum
        //see https://minecraft.gamepedia.com/Health#Death
        int expToDrop = SetExpFix.getTotalExperience(player);
        if (expToDrop > 100)
            expToDrop = 100;

        List<ItemStack> itemsToDrop = new ArrayList<>(player.getInventory().getSize());
        if (!keepInventory)
        {
            //Compile a list of null-free/air-free items to drop
            for (ItemStack itemStack : player.getInventory().getContents())
            {
                if (itemStack != null && itemStack.getType() != Material.AIR && !itemStack.containsEnchantment(Enchantment.VANISHING_CURSE))
                    itemsToDrop.add(itemStack);
            }
        }

        //TODO: Non-vanilla behavior, see issue #4
        String deathMessage = "";

            /*Fire PlayerDeathEvent*/
        PlayerDeathEvent deathEvent = new PlayerDeathEvent(player, itemsToDrop, expToDrop, deathMessage);
        deathEvent.setKeepInventory(keepInventory); //CB's constructor does indeed set whether the inventory is kept or not, using the gamerule's value
        instance.getServer().getPluginManager().callEvent(deathEvent);

        //TODO: Non-vanilla behavior, see issue #5
        //Print death message
        if (deathEvent.getDeathMessage() != null && !deathEvent.getDeathMessage().isEmpty() && showDeathMessages)
            instance.getServer().broadcastMessage(deathEvent.getDeathMessage());

        //Clear and drop items
        if (!deathEvent.getKeepInventory())
        {
            player.getInventory().clear();
            for (ItemStack itemStack : deathEvent.getDrops())
            {
                if (itemStack != null && itemStack.getType() != Material.AIR)
                    player.getWorld().dropItemNaturally(player.getLocation(), itemStack);
            }
        }

        //Clear and set experience, if getKeepLevel() == false
        if (!deathEvent.getKeepLevel())
        {
            SetExpFix.setTotalExperience(player, 0);
            player.setTotalExperience(deathEvent.getNewTotalExp());
            player.setLevel(deathEvent.getNewLevel());
            player.setExp(deathEvent.getNewExp());
        }

        //Drop experience
        if (deathEvent.getDroppedExp() > 0)
            player.getWorld().spawn(player.getLocation(), ExperienceOrb.class).setExperience(deathEvent.getDroppedExp());

        //Close any inventory the player may be viewing
        player.closeInventory();

        //Increment/reset death statistics
        player.incrementStatistic(Statistic.DEATHS);
        player.setStatistic(Statistic.TIME_SINCE_DEATH, 0);

        //TODO: Non-vanilla behavior: Player death animation (red and falling over) (Issue #13)
        //Smoke effect //TODO: after 20 ticks (Issue #14) (Will implement 20 tick delay after issue #13 is resolved
        if (instance.isSpectating(player)) //TODO: does smoke effect/death animation occur if player#spigot()#respawn() is called on death? My guess is no.
            player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 25);

        //Clear potion effects
        for (PotionEffect potionEffect : player.getActivePotionEffects())
            player.removePotionEffect(potionEffect.getType());

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
                arrow.remove(); //Delete projectile
            }
        }
        if (killer == player) //Though we don't care if they did it themselves
            killer = null;

        //Increment player's ENTITY_KILLED_BY if killer is an entitytype recorded by this statistic
        if (killer != null)
        {
            try //Not going to manually check entities, TODO: ESPECIALLY WHEN IT IS NOT DOCUMENTED
            {
                player.incrementStatistic(Statistic.ENTITY_KILLED_BY, killer.getType());
            }
            catch (IllegalArgumentException e) {} // "The supplied EntityType does not have a corresponding statistic"
            catch (NullPointerException e)
            {
                instance.getLogger().warning("NPE: Was unable to increment ENTITY_KILLED_BY statistic.");
                instance.getLogger().info("If you wish to report this, please include the information below:");
                instance.getLogger().info("Killer was " + killer.toString());
                instance.getLogger().info("Player was " + player.toString());
            }
        }

        //Increment _killer's_ PLAYER_KILLS
        if (killer != null && killer.getType() == EntityType.PLAYER)
        {
            Player playerKiller = (Player)killer;
            playerKiller.incrementStatistic(Statistic.PLAYER_KILLS);
        }

            /*Start death spectating!*/
        SpectateTask task = new SpectateTask(player, configManager.getRespawnTicks(), killer, instance);
        instance.getServer().getPluginManager().callEvent(new DeathSpectatingEvent(task));
        task.runTaskTimer(instance, 1L, 1L);

        //Send player a message that they were killed and are now spectating, if configured to do so
        if (!configManager.getYouDiedMessage().isEmpty())
            player.sendMessage(configManager.getYouDiedMessage());
    }

    void onPreDeath(EntityDamageEvent event)
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
        if (inventory.getItemInMainHand().getType() == Material.TOTEM || inventory.getItemInOffHand().getType() == Material.TOTEM)
            return;

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
