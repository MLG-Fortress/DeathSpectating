package com.robomwm.deathspectating.listeners;

import com.robomwm.deathspectating.SetExpFix;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import com.robomwm.deathspectating.DeathSpectating;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created on 2/15/2017.
 * @author RoboMWM
 */
public class DeathListener implements Listener
{
    private DeathSpectating plugin;
    private boolean cancellable = true;

    public DeathListener(DeathSpectating deathSpectating)
    {
        plugin = deathSpectating;

        if (!Cancellable.class.isAssignableFrom(PlayerDeathEvent.class))
        {
            cancellable = false;
            plugin.getLogger().info(" = = = = = = = = = = = = = = = = = = = =");
            plugin.getLogger().info(" ");
            plugin.getLogger().warning("FYI, this plugin works better on Paper");
            plugin.getLogger().warning("(Or any fork of Bukkit that allows you to cancel PlayerDeathEvent, preserving vanilla/plugin death message and exp drop calculation.)");
            plugin.getLogger().warning("Learn and get Paper (it's ez) at https://papermc.io");
            plugin.getLogger().info(" ");
            plugin.getLogger().info(" = = = = = = = = = = = = = = = = = = = =");
            return;
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    void onPlayerDies(PlayerDeathEvent event)
    {
        if (!cancellable)
            return;

        Player player = event.getEntity();

        if (plugin.isSpectating(player))
            return;

        if (!plugin.getConfigManager().canSpectate(player, player.getLastDamageCause().getCause()))
            return;

        if (event.getEntity().hasMetadata("NPC"))
            return;

        /*Put player in death spectating mode*/
        if (plugin.startDeathSpectating(player, event))
        {
            //Cancel event so player doesn't actually die
            event.setCancelled(true);

            //Play the "hit" sound (since we canceled the event, the hit sound will not play)
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
        }
    }

    //CB/Spigot compat
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    void onPlayerBasicallyWouldBeDead(EntityDamageEvent event)
    {
        if (cancellable)
            return;

        if (event.getEntityType() != EntityType.PLAYER)
            return;

        Player player = (Player)event.getEntity();

        if (!plugin.getConfigManager().canSpectate(player, event.getCause()))
            return;

        //Ignore if player will survive this damage
        if (player.getHealth() > event.getFinalDamage())
            return;

        //Ignore if player is holding a totem of undying
        PlayerInventory inventory = player.getInventory();
        if (inventory.getItemInMainHand().getType() == Material.TOTEM_OF_UNDYING || inventory.getItemInOffHand().getType() == Material.TOTEM_OF_UNDYING)
            return;

        //Ignore if this is probably the result of the Essentials suicide command
        //Essentials will perform Player#setHealth(0), which does not fire a damage event, but does kill the player. This will lead to a double death message.
        if ((event.getCause() == EntityDamageEvent.DamageCause.CUSTOM || event.getCause() == EntityDamageEvent.DamageCause.SUICIDE)
                && event.getDamage() == Short.MAX_VALUE)
            return;

        player.setLastDamageCause(event);

        boolean keepInventory = player.getWorld().getGameRuleValue(GameRule.KEEP_INVENTORY) || player.getGameMode() == GameMode.SPECTATOR;

        //+phoenix616: RoboMWM: it will drop level * 7 exp and 100 as a maximum
        //see https://minecraft.gamepedia.com/Health#Death
        int expToDrop = 0;

        List<ItemStack> itemsToDrop = new ArrayList<>(player.getInventory().getSize());
        if (!keepInventory)
        {
            //Compile a list of null-free/air-free items to drop
            for (ItemStack itemStack : player.getInventory().getContents())
            {
                if (itemStack != null && itemStack.getType() != Material.AIR && !itemStack.containsEnchantment(Enchantment.VANISHING_CURSE))
                    itemsToDrop.add(itemStack);
            }

            //Calculate and set experience to drop
            expToDrop = Math.min(100, SetExpFix.getTotalExperience(player));
        }

        /*Prepare PlayerDeathEvent*/
        PlayerDeathEvent deathEvent = new PlayerDeathEvent(player, itemsToDrop, expToDrop, "");
        deathEvent.setKeepInventory(keepInventory); //CB's constructor does indeed set whether the inventory is kept or not, using the gamerule's value
        //And fire
        plugin.getServer().getPluginManager().callEvent(deathEvent);


        /*Put player in death spectating mode*/
        if (plugin.startDeathSpectating(player, deathEvent))
        {
            //Cancel event so player doesn't actually die
            event.setCancelled(true);

            //Play the "hit" sound (since we canceled the event, the hit sound will not play)
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
        }

    }

}
