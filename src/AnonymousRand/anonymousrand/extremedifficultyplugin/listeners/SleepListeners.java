package AnonymousRand.anonymousrand.extremedifficultyplugin.listeners;

import AnonymousRand.anonymousrand.extremedifficultyplugin.customgoals.CustomPathfinderTargetCondition;
import net.minecraft.server.v1_16_R1.*;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_16_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_16_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;

import java.util.HashMap;

public class SleepListeners implements Listener {

    private static int cycles; //for extending duration of sleep; intentionally global instead of hashmapped for each player
    private static int peeCounter;
    private static HashMap<Player, Long> enterBedTime= new HashMap<>(); //keeps track of time of last successful bed enter; 0 if restarting a new night cycle
    private static HashMap<Player, Long> leaveBedTime = new HashMap<>(); //keeps track of time of last successful bed leave; 0 if restarting a new night cycle

    public SleepListeners() {
        cycles = 0;
        peeCounter = 0;
    }

    @EventHandler
    public void playerBedEnter(PlayerBedEnterEvent event) { /**players can't sleep even if there are monsters below or above it (doesn't count horizontal range; no monster range increased to 30 blocks)*/

        EntityPlayer player = ((CraftPlayer)event.getPlayer()).getHandle();
        World world = ((CraftWorld)event.getPlayer().getWorld()).getHandle();
        EntityMonster closestMonster = world.a(EntityMonster.class, new CustomPathfinderTargetCondition(), player, player.locX(), player.locY(), player.locZ(), player.getBoundingBox().grow(128.0, 128.0, 128.0)); //get closest monster within 128 sphere radius of player

        if (world.getMinecraftWorld().isRainingAt(new BlockPosition(player.getPositionVector().getX(), player.getPositionVector().getY(), player.getPositionVector().getZ())) && world.isDay()) { /**can't sleep in day thunderstorm anymore*/
            Bukkit.broadcastMessage("The thunder is too loud and you can't fall asleep");
            event.setCancelled(true);
        }

        if (closestMonster != null && (Math.pow(closestMonster.locX() - player.locX(), 2) + Math.pow(closestMonster.locZ() - player.locZ(), 2) <= 900.0) && (Math.pow(closestMonster.locX() - player.locX(), 2) + Math.pow(closestMonster.locZ() - player.locZ(), 2) > 64.0)) { //player within 30 blocks horizontally of closestMonster but out of the default 8 block range
            if (Math.pow(closestMonster.locX() - player.locX(), 2) + Math.pow(closestMonster.locY() - player.locY(), 2) + Math.pow(closestMonster.locZ() - player.locZ(), 2) <= 900.0) { //player within 30 blocks including vertical distance of closestMonster
                Bukkit.broadcastMessage("There are still monsters nearby");
                event.setCancelled(true);
            } else if (closestMonster.locY() < player.locY()){ //player not within 30 blocks if counting vertical distance and is above mobs
                Bukkit.broadcastMessage("You may not sleep now, there are monsters below you");
                event.setCancelled(true);
            } else { //player not within 30 blocks if counting vertical distance and is below mobs
                Bukkit.broadcastMessage("You may not sleep now, there are monsters above you");
                event.setCancelled(true);
            }
        }

        if (!event.isCancelled()) {
            Player player1 = event.getPlayer();

            if (player1.getWorld().getFullTime() - leaveBedTime.getOrDefault(player1, player1.getWorld().getFullTime()) >= 11000) { //to reset these stats if the last of the 3 sleep cycles were not executed and more than 1 night's worth of time has passed
                cycles = 0;                                                   //get full time tells you the amount of RTA that has passed since the world started (not changed by sleeping and time set commands)
                leaveBedTime.put(player1, (long)0);
                peeCounter = 0;
            }

            if (leaveBedTime.getOrDefault(player1, (long)0) != 0 && player1.getWorld().getFullTime() - leaveBedTime.getOrDefault(player1, player1.getWorld().getFullTime()) <= 150) { //it has been less than 7.5 seconds since the player was last woken up by playerBedLeave and the 3 sleep cycles haven't finished yet
                if (cycles == 1) {
                    Bukkit.broadcastMessage("You are still being haunted by the nightmare and can't sleep yet");
                } else {
                    if (peeCounter == 1) {
                        Bukkit.broadcastMessage("You are still peeing");
                        peeCounter++;
                    } else if (peeCounter < 4) {
                        Bukkit.broadcastMessage("Where does Steve pee?");
                        peeCounter++;
                    } else {
                        Bukkit.broadcastMessage("IT'S YOUR OWN BODY CAN'T YOU TELL THAT YOU ARE STILL PEEING");
                    }
                }

                event.setCancelled(true);
            } else { //if the event is not cancelled and the player gets into the bed successfully
                enterBedTime.put(player1, player1.getWorld().getFullTime()); //to keep track of how long the player spends in a bed before waking up/laeving bed
            }
        }
    }

    @EventHandler
    public void playerBedLeave(PlayerBedLeaveEvent event) { //must sleep 3 times to completely pass night as each time only passes 1/3 of the night; with a 5 sec delay between each sleep attempt
        org.bukkit.World world = event.getPlayer().getWorld();
        Player player = event.getPlayer();

        if (enterBedTime.containsKey(player)) {
            if (world.getFullTime() - enterBedTime.get(player) < 101) { //do not execute the rest of the function if the player leaves the bed before the 5.05 seconds full time is up
                Bukkit.broadcastMessage("You must sleep for more than 5 seconds...get your sleep schedule fixed");
            } else { //only executes if player has been in a bed continuously for 5 seconds and just woke up from that
                switch (cycles) {
                    case 0 -> {
                        Bukkit.broadcastMessage("Congrats, you made it through the night......sike");
                        Bukkit.broadcastMessage("You got woken up by a nightmare");
                        leaveBedTime.put(player, world.getFullTime() - 7000);
                        world.setFullTime(world.getFullTime() - 7000); //approx 2/3 of the night
                        cycles++;
                    }
                    case 1 -> {
                        Bukkit.broadcastMessage("It's 3am and you need to pee");
                        leaveBedTime.put(player, world.getFullTime() - 3500);
                        world.setFullTime(world.getFullTime() - 3500); //approx 1/3 of the night
                        cycles++;
                        peeCounter = 1;
                    }
                    case 2 -> {
                        Bukkit.broadcastMessage("Congrats, you made it through the night...enjoy all the leftover mobs");
                        cycles = 0;
                        leaveBedTime.put(player, (long) 0);
                        peeCounter = 0;
                    }
                }
            }
        }
    }
}