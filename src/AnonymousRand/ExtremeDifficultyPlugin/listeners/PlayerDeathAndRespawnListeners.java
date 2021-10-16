package AnonymousRand.ExtremeDifficultyPlugin.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class PlayerDeathAndRespawnListeners implements Listener {

    private static HashMap<Player, Collection<PotionEffect>> collections = new HashMap<>();
    private static HashMap<Player, Integer> respawnCount = new HashMap<>();
    private JavaPlugin plugin;

    public PlayerDeathAndRespawnListeners(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void playerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        collections.put(player, player.getActivePotionEffects()); //negativestatus effects now last after respawning
    }

    @EventHandler
    public void playerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable() //delay by 1 tick or else the server does not re-apply the status effects, thinking that the player doesn't exist yet
        {
            public void run()
            {
                for (PotionEffect e : collections.get(player)) { //only re-applies negative status effects
                    if (e.getType().equals(PotionEffectType.SLOW) || e.getType().equals(PotionEffectType.SLOW_DIGGING) || e.getType().equals(PotionEffectType.CONFUSION) || e.getType().equals(PotionEffectType.BLINDNESS) || e.getType().equals(PotionEffectType.HUNGER) || e.getType().equals(PotionEffectType.WEAKNESS) || e.getType().equals(PotionEffectType.POISON) || e.getType().equals(PotionEffectType.WITHER) || e.getType().equals(PotionEffectType.LEVITATION) || e.getType().equals(PotionEffectType.UNLUCK) || e.getType().equals(PotionEffectType.BAD_OMEN)) {
                        player.addPotionEffect(e);
                    }
                }

                respawnCount.put(player, respawnCount.getOrDefault(player, 0) + 1);

                if (respawnCount.get(player) % 2 == 0) { //create explosion on respawn location every 2 respawns regardless of if they switched beds/anchors
                    player.getWorld().createExplosion(event.getRespawnLocation(), 1.5f);
                }
            }
        }, 1);
    }
}