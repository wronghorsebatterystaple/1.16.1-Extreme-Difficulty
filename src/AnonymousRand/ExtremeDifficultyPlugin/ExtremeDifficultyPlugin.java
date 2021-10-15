package AnonymousRand.ExtremeDifficultyPlugin;

import AnonymousRand.ExtremeDifficultyPlugin.listeners.*;
import org.bukkit.plugin.java.JavaPlugin;

public class ExtremeDifficultyPlugin extends JavaPlugin {
    @Override
    public void onLoad() {

    }

    @Override
    public void onEnable() { //this runs when the plugin is first enabled (when the server starts up)
        getServer().getPluginManager().registerEvents(new EntitySpawnAndReplaceWithCustomListeners(), this); //registers the listeners
        getServer().getPluginManager().registerEvents(new EntityDamageListeners(), this);
        getServer().getPluginManager().registerEvents(new ProjectileListeners(), this);
        getServer().getPluginManager().registerEvents(new PlayerMovementAndFallDamageListeners(), this);
        getServer().getPluginManager().registerEvents(new VehicleCreateListeners(), this);
        getServer().getPluginManager().registerEvents(new BlockPlaceAndBreakListeners(), this);
        getServer().getPluginManager().registerEvents(new SleepListeners(), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListeners(this), this);
        getServer().getPluginManager().registerEvents(new DropItemListeners(),this);
        getServer().getPluginManager().registerEvents(new PlayerInteractListeners(),this);
        getServer().getPluginManager().registerEvents(new PlayerDamageListeners(),this);
        getServer().getPluginManager().registerEvents(new RaidAndVillageListeners(),this);
        getServer().getPluginManager().registerEvents(new VillagerTradeListeners(),this);
        getServer().getPluginManager().registerEvents(new PlayerItemConsumeListeners(),this);
    }

    @Override
    public void onDisable() {

    }
}
