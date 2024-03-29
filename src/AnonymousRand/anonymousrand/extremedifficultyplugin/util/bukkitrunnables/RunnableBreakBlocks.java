package AnonymousRand.anonymousrand.extremedifficultyplugin.util.bukkitrunnables;

import net.minecraft.server.v1_16_R1.Entity;
import net.minecraft.server.v1_16_R1.EntityInsentient;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;
import java.util.function.Predicate;

import static AnonymousRand.anonymousrand.extremedifficultyplugin.util.Predicates.*;

public class RunnableBreakBlocks extends BukkitRunnable {

    protected final Entity entity;
    protected final World bukkitWorld;
    protected Location bukkitLoc;
    protected final int radX, radY, radZ, yOffset;
    protected final boolean removeFluids, immuneBlocks;
    protected int cycles;
    protected final int maxCycles;
    protected int baseX, baseY, baseZ;
    protected Block bukkitBlock;
    protected Material bukkitMaterial;
    protected Predicate<Material> blockBreakable = (type) -> blockBreakable_Base.test(type) && notBedrock.test(type) && notHardBlocks.test(type) && notFireWitherRose.test(type);
    protected static final Random random = new Random();

    public RunnableBreakBlocks(Entity entity, int radX, int radY, int radZ, int yOffset, boolean removeFluids) {
        this.entity = entity;
        this.bukkitWorld = entity.getWorld().getWorld();
        this.radX = radX;
        this.radY = radY;
        this.radZ = radZ;
        this.yOffset = yOffset;
        this.removeFluids = removeFluids;
        this.immuneBlocks = true;
        this.cycles = 0;
        this.maxCycles = 1;
    }

    public RunnableBreakBlocks(Entity entity, int radX, int radY, int radZ, int yOffset, boolean removeFluids, boolean immuneBlocks, int maxCycles) {
        this.entity = entity;
        this.bukkitWorld = entity.getWorld().getWorld();
        this.radX = radX;
        this.radY = radY;
        this.radZ = radZ;
        this.yOffset = yOffset;
        this.removeFluids = removeFluids;
        this.immuneBlocks = immuneBlocks;
        this.cycles = 0;
        this.maxCycles = maxCycles;
    }

    public RunnableBreakBlocks(Location bukkitLoc, World bukkitWorld, int radX, int radY, int radZ, int yOffset, boolean removeFluids) {
        this.entity = null;
        this.bukkitLoc = bukkitLoc;
        this.bukkitWorld = bukkitWorld;
        this.radX = radX;
        this.radY = radY;
        this.radZ = radZ;
        this.yOffset = yOffset;
        this.removeFluids = removeFluids;
        this.immuneBlocks = true;
        this.cycles = 0;
        this.maxCycles = 1;
    }

    @Override
    public void run() {
        if (++this.cycles > this.maxCycles) {
            this.cancel();
            return;
        }

        if (this.entity != null) {
            this.baseX = (int)Math.floor(this.entity.getPositionVector().getX());
            this.baseY = (int)Math.floor(this.entity.getPositionVector().getY()) + this.yOffset;
            this.baseZ = (int)Math.floor(this.entity.getPositionVector().getZ());
        } else {
            this.baseX = (int)Math.floor(this.bukkitLoc.getX());
            this.baseY = (int)Math.floor(this.bukkitLoc.getY()) + this.yOffset;
            this.baseZ = (int)Math.floor(this.bukkitLoc.getZ());
        }

        if (this.entity instanceof EntityInsentient) {
            if (((EntityInsentient)this.entity).getGoalTarget() != null) {
                if (((EntityInsentient)this.entity).getGoalTarget().locY() < this.entity.locY()) { // move downwards if player is below entity
                    this.baseY--;
                }
            }
        }

        for (int x = -this.radX; x <= this.radX; x++) {
            for (int y = -this.radY; y <= this.radY; y++) {
                for (int z = -this.radZ; z <= this.radZ; z++) {
                    int x1 = this.baseX + x, y1 = this.baseY + y, z1 = this.baseZ + z;
                    this.bukkitBlock = this.bukkitWorld.getBlockAt(x1, y1, z1);
                    this.bukkitMaterial = this.bukkitBlock.getType();

                    if (blockBreakable.test(this.bukkitMaterial) && (this.removeFluids ? true : notFluid.test(this.bukkitMaterial)) && (this.immuneBlocks ? notPreciousBlocks.test(this.bukkitMaterial) : true)) { // as long as it isn't one of these blocks
                        this.bukkitBlock.setType(Material.AIR);
                    } else if (!notHardBlocks.test(this.bukkitMaterial)) { // 50% chance to break these blocks
                        if (random.nextDouble() < 0.5) {
                            this.bukkitBlock.setType(Material.AIR);
                        }
                    }
                }
            }
        }
    }
}
