package AnonymousRand.anonymousrand.extremedifficultyplugin.listeners;

import AnonymousRand.anonymousrand.extremedifficultyplugin.customentities.mobs.CustomEntityPufferfish;
import AnonymousRand.anonymousrand.extremedifficultyplugin.customgoals.util.CustomPathfinderTargetCondition;
import net.minecraft.server.v1_16_R1.*;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_16_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_16_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;

import java.util.HashMap;

public class ListenerSleep implements Listener {

    private static int cycles; // for extending duration of sleep; intentionally global instead of hashmapped for each player
    private static int peeCounter;
    private static final HashMap<Player, Long> enterBedTime= new HashMap<>(); // keeps track of time of last successful bed enter; 0 if restarting a new night cycle
    private static final HashMap<Player, Long> leaveBedTime = new HashMap<>(); // keeps track of time of last successful bed leave; 0 if restarting a new night cycle

    public ListenerSleep() {
        cycles = 0;
        peeCounter = 0;
    }

    @EventHandler
    public void playerBedEnter(PlayerBedEnterEvent event) { /* players can't sleep even if there are monsters below or above it (doesn't count horizontal range; no monster range increased to 30 blocks) */
        EntityPlayer nmsPlayer = ((CraftPlayer)event.getPlayer()).getHandle();
        Block bukkitBed = event.getBed();
        World nmsWorld = ((CraftWorld)event.getPlayer().getWorld()).getHandle();

        if (nmsWorld.getMinecraftWorld().isRainingAt(new BlockPosition(bukkitBed.getX(), bukkitBed.getY(), bukkitBed.getZ())) && nmsWorld.isDay()) { /* can't sleep in day thunderstorm anymore */
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw " + nmsPlayer.getName() + " \"The thunder is too loud and you can't fall asleep\"");
            event.setCancelled(true);
            return;
        }

        EntityLiving closestMonster = nmsWorld.a(EntityMonster.class, CustomPathfinderTargetCondition.DEFAULT, nmsPlayer, nmsPlayer.locX(), nmsPlayer.locY(), nmsPlayer.locZ(), nmsPlayer.getBoundingBox().grow(50.0, 128.0, 50.0)); // get closest monster within 50 blocks horizontally of player
        CustomEntityPufferfish closestPufferfish = nmsWorld.a(CustomEntityPufferfish.class, CustomPathfinderTargetCondition.DEFAULT, nmsPlayer, nmsPlayer.locX(), nmsPlayer.locY(), nmsPlayer.locZ(), nmsPlayer.getBoundingBox().grow(50.0, 128.0, 50.0)); // get closest pufferfish within 50 blocks horizontally of player
        double monsterDistanceNoY;
        double pufferfishDistanceNoY;

        try {
            monsterDistanceNoY = Math.pow(closestMonster.locX() - bukkitBed.getX(), 2) + Math.pow(closestMonster.locZ() - bukkitBed.getZ(), 2);
        } catch (NullPointerException e) {
            monsterDistanceNoY = Integer.MAX_VALUE;
        }

        try {
            pufferfishDistanceNoY = Math.pow(closestPufferfish.locX() - bukkitBed.getX(), 2) + Math.pow(closestPufferfish.locZ() - bukkitBed.getZ(), 2);
        } catch (NullPointerException e) {
            pufferfishDistanceNoY = Integer.MAX_VALUE;
        }

        if (pufferfishDistanceNoY < monsterDistanceNoY) { /* pufferfish also count as monsters to prevent sleeping */
            closestMonster = closestPufferfish;
            monsterDistanceNoY = pufferfishDistanceNoY;
        }

        if (closestMonster != null) {
            if (monsterDistanceNoY <= 1024.0) { // player within 32 blocks horizontally of closestMonster
                if (Math.pow(closestMonster.locX() - bukkitBed.getX(), 2) + Math.pow(closestMonster.locY() - bukkitBed.getY(), 2) + Math.pow(closestMonster.locZ() - bukkitBed.getZ(), 2) <= 1024.0) { // player within 32 blocks including vertical distance of closestMonster
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw " + nmsPlayer.getName() + " \"You may not sleep now, there are monsters nearby\"");
                    event.setCancelled(true);
                } else if (closestMonster.locY() < bukkitBed.getY()){ // player not within 30 blocks if counting vertical distance and is above mobs
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw " + nmsPlayer.getName() + " \"You may not sleep now, there are monsters below you\"");
                    event.setCancelled(true);
                } else { // player not within 30 blocks if counting vertical distance and is below mobs
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw " + nmsPlayer.getName() + " \"You may not sleep now, there are monsters above you\"");
                    event.setCancelled(true);
                }
            }
        }

        if (!event.isCancelled()) {
            Player player1 = event.getPlayer();

            if (player1.getWorld().getFullTime() - leaveBedTime.getOrDefault(player1, player1.getWorld().getFullTime()) >= 11000) { // to reset these stats if the last of the 3 sleep cycles were not executed and more than 1 night's worth of time has passed
                cycles = 0;                                                                                                         // get full time tells you the amount of RTA that has passed since the world started (not changed by sleeping and time set commands)
                leaveBedTime.put(player1, (long)0);
                peeCounter = 0;
            }

            if (leaveBedTime.getOrDefault(player1, (long)0) != 0 && player1.getWorld().getFullTime() - leaveBedTime.getOrDefault(player1, player1.getWorld().getFullTime()) <= 180) { // it has been less than 9 seconds since the player was last woken up by playerBedLeave and the 3 sleep cycles haven't finished yet
                if (cycles == 1) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw " + nmsPlayer.getName() + " \"You are still being haunted by the nightmare and can't fall asleep yet\"");
                } else {
                    if (peeCounter < 4) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw " + nmsPlayer.getName() + " \"You are still peeing\"");
                        peeCounter++;
                    } else {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw " + nmsPlayer.getName() + " \"Can't you tell that you are still peeing?\"");
                    }
                }

                event.setCancelled(true);
            } else { // if the event is not cancelled and the player gets into the bed successfully
                enterBedTime.put(player1, player1.getWorld().getFullTime()); // to keep track of how long the player spends in a bed before waking up/laeving bed
            }
        }
    }

    @EventHandler
    public void playerBedLeave(PlayerBedLeaveEvent event) { // must sleep 3 times to completely pass night as each time only passes 1/3 of the night; with a 5 sec delay between each sleep attempt
        org.bukkit.World bukkitWorld = event.getPlayer().getWorld();
        Player bukkitPlayer = event.getPlayer();

        if (enterBedTime.containsKey(bukkitPlayer)) {
            if (bukkitWorld.getFullTime() - enterBedTime.get(bukkitPlayer) < 101) { // do not execute the rest of the function if the player leaves the bed before the 5.05 seconds full time is up
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw " + bukkitPlayer.getName() + " \"You must sleep for more than 5 seconds...get your sleep schedule fixed\"");
            } else { // only executes if player has been in a bed continuously for 5 seconds and just woke up from that
                switch (cycles) {
                    case 0:
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw " + bukkitPlayer.getName() + " \"You got woken up by a nightmare\"");
                        leaveBedTime.put(bukkitPlayer, bukkitWorld.getFullTime() - 7000);
                        bukkitWorld.setFullTime(bukkitWorld.getFullTime() - 7000); // approx 2/3 of the night
                        cycles++;
                        break;
                    case 1:
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw " + bukkitPlayer.getName() + " \"It's 3am and you need to pee\"");
                        leaveBedTime.put(bukkitPlayer, bukkitWorld.getFullTime() - 3500);
                        bukkitWorld.setFullTime(bukkitWorld.getFullTime() - 3500); // approx 1/3 of the night
                        cycles++;
                        peeCounter = 1;
                        break;
                    case 2:
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw " + bukkitPlayer.getName() + " \"Congrats, you made it through the night...enjoy all the leftover mobs\"");
                        cycles = 0;
                        leaveBedTime.put(bukkitPlayer, (long) 0);
                        peeCounter = 0;
                        break;
                }
            }
        }
    }
}