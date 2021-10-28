package AnonymousRand.ExtremeDifficultyPlugin.customEntities.customMobs;

import AnonymousRand.ExtremeDifficultyPlugin.customGoals.*;
import AnonymousRand.ExtremeDifficultyPlugin.util.CoordsFromHypotenuse;
import net.minecraft.server.v1_16_R1.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;

public class CustomEntitySpider extends EntitySpider {

    public int attacks;
    private boolean a25, a20, a50, a80;

    public CustomEntitySpider(World world) {
        super(EntityTypes.SPIDER, world);
        this.attacks = 0;
        this.a25 = false;
        this.a20 = false;
        this.a50 = false;
        this.a80 = false;
    }

    @Override
    protected void initPathfinder() { /**no longer targets iron golems*/
        this.goalSelector.a(0, new NewPathfinderGoalTeleportToPlayer(this, this.getFollowRange(), 300.0, 0.00333333333)); /**custom goal that gives mob a chance every tick to teleport to within initial follow_range-2 to follow_range+13 blocks of nearest player if it has not seen a player target within follow range for 15 seconds*/
        this.goalSelector.a(1, new PathfinderGoalFloat(this));
        this.goalSelector.a(2, new NewPathfinderGoalBreakBlocksAround(this, 100, 1, 0, 1, 0, true)); /**custom goal that breaks blocks around the mob periodically*/
        this.goalSelector.a(3, new PathfinderGoalLeapAtTarget(this, 0.4F));
        this.goalSelector.a(4, new CustomPathfinderGoalMeleeAttack(this, 1.0, true)); /**uses the custom goal that attacks even when line of sight is broken (the old goal stopped the mob from attacking even if the mob has already recognized a target via CustomNearestAttackableTarget goal); this custom goal also allows the spider to continue attacking regardless of light level*/
        this.goalSelector.a(5, new PathfinderGoalRandomStrollLand(this, 0.8D));
        this.goalSelector.a(6, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, 8.0F));
        this.goalSelector.a(6, new PathfinderGoalRandomLookaround(this));
        this.targetSelector.a(1, new PathfinderGoalHurtByTarget(this, new Class[0]));
        this.targetSelector.a(2, new CustomPathfinderGoalNearestAttackableTarget<>(this, EntityHuman.class, false)); /**uses the custom goal which doesn't need line of sight to start shooting at players (passes to CustomPathfinderGoalNearestAttackableTarget.g() which passes to CustomIEntityAccess.customFindPlayer() which passes to CustomIEntityAccess.customFindEntity() which passes to CustomPathfinderTargetConditions.a() which removes line of sight requirement); this custom goal also allows the spider to continue attacking regardless of light level*/
    }

    public double getFollowRange() {
        return 16.0;
    }

    protected CoordsFromHypotenuse coordsFromHypotenuse = new CoordsFromHypotenuse();

    @Override
    public void tick() {
        super.tick();

        if (this.ticksLived == 10) { /**spiders move 70% faster but only do 1 damage and 12.5 health*/
            this.getAttributeInstance(GenericAttributes.MOVEMENT_SPEED).setValue(0.51);
            this.getAttributeInstance(GenericAttributes.ATTACK_DAMAGE).setValue(1.0);
            this.setHealth(12.5f);
            ((LivingEntity)this.getBukkitEntity()).setMaxHealth(12.5);
        }

        if (this.attacks % 24 == 0 && this.a25) { //reset right before the next cycle
            this.a25 = false;
        }

        if (this.attacks % 25 == 0 && this.attacks != 0 && !this.a25) { /**every 25 attacks, spiders lay down cobwebs that last 5 seconds in a 5 by 5 cube around itself*/
            this.a25 = true;

            Location loc;
            for (int x = -2; x <= 2; x++) {
                for (int y = -2; y <= 2; y++) {
                    for (int z = -2; z <= 2; z++) {
                        loc = new Location(this.getWorld().getWorld(), Math.floor(this.locX()) + x, Math.floor(this.locY()) + y, Math.floor(this.locZ()) + z);
                        if (loc.getBlock().getType() == org.bukkit.Material.AIR) {
                            loc.getBlock().setType(org.bukkit.Material.COBWEB);
                            Bukkit.getPluginManager().callEvent(new BlockPlaceEvent(loc.getBlock(), loc.getBlock().getState(), null, null, null, false, null)); //fire event that would otherwise not be fired so that the cobweb block can be broken after 4 seconds
                        }
                    }
                }
            }
        }

        if (this.attacks == 20 && !this.a20) { /**after 20 attacks, spiders gain speed 1*/
            this.a20 = true;
            this.addEffect(new MobEffect(MobEffects.FASTER_MOVEMENT, Integer.MAX_VALUE, 0));
        }

        if (this.attacks == 50 && !this.a50) { /**after 50 attacks, spiders summon 3 vanilla cave spiders*/
            this.a50 = true;
            EntityCaveSpider newCaveSpider;

            for (int i = 0; i < 3; i++) {
                newCaveSpider = new EntityCaveSpider(EntityTypes.CAVE_SPIDER, this.getWorld());
                newCaveSpider.setPositionRotation(this.locX(), this.locY(), this.locZ(), this.yaw, this.pitch);
                this.getWorld().addEntity(newCaveSpider, CreatureSpawnEvent.SpawnReason.DROWNED);
            }
        }

        if (this.attacks == 80 && !this.a80) { /**after 80 attacks, spiders summon 5 cave spiders*/
            this.a80 = true;
            CustomEntitySpiderCave newCaveSpider;

            for (int i = 0; i < 6; i++) {
                newCaveSpider = new CustomEntitySpiderCave(this.getWorld());
                newCaveSpider.setPositionRotation(this.locX(), this.locY(), this.locZ(), this.yaw, this.pitch);
                this.getWorld().addEntity(newCaveSpider, CreatureSpawnEvent.SpawnReason.NATURAL);
            }
        }

        Location thisLoc = new Location(this.getWorld().getWorld(), this.locX(), this.locY(), this.locZ());
        if (thisLoc.getBlock().getType() == org.bukkit.Material.AIR) { /**spiders lay down cobwebs that last 4 seconds on itself as long as it is inside an air block*/ //cobwebs also indirectly prevent players from shooting arrows onto the spider as the arrows are blocked by the web hitbox
            thisLoc.getBlock().setType(org.bukkit.Material.COBWEB);
            Bukkit.getPluginManager().callEvent(new BlockPlaceEvent(thisLoc.getBlock(), thisLoc.getBlock().getState(), null, null, null, false, null)); //fire event that would otherwise not be fired so that the cobweb block can be broken after 4 seconds
        }
    }

    @Override
    public void checkDespawn() {
        if (this.world.getDifficulty() == EnumDifficulty.PEACEFUL && this.L()) {
            this.die();
        } else if (!this.isPersistent() && !this.isSpecialPersistence()) {
            EntityHuman entityhuman = this.world.findNearbyPlayer(this, -1.0D);

            if (entityhuman != null) {
                double d0 = Math.pow(entityhuman.getPositionVector().getX() - this.getPositionVector().getX(), 2) + Math.pow(entityhuman.getPositionVector().getZ() - this.getPositionVector().getZ(), 2); //mobs only despawn along horizontal axes; if you are at y level 256 mobs will still spawn below you at y64 and prevent sleepingdouble d0 = entityhuman.h(this);
                int i = this.getEntityType().e().f();
                int j = i * i;

                if (d0 > (double)j && this.isTypeNotPersistent(d0)) {
                    this.die();
                }

                int k = this.getEntityType().e().g() + 8; /**random despawn distance increased to 40 blocks*/
                int l = k * k;

                if (this.ticksFarFromPlayer > 600 && this.random.nextInt(800) == 0 && d0 > (double)l && this.isTypeNotPersistent(d0)) {
                    this.die();
                } else if (d0 < (double)l) {
                    this.ticksFarFromPlayer = 0;
                }
            }

        } else {
            this.ticksFarFromPlayer = 0;
        }
    }

    @Override
    public double g(double d0, double d1, double d2) {
        double d3 = this.locX() - d0; /**for determining distance to entities, y level does not matter, eg. mob follow range, attacking (can hit player no matter the y level)*/
        double d5 = this.locZ() - d2;

        return d3 * d3 + d5 * d5;
    }

    @Override
    public double d(Vec3D vec3d) {
        double d0 = this.locX() - vec3d.x; /**for determining distance to entities, y level does not matter, eg. mob follow range, attacking (can hit player no matter the y level)*/
        double d2 = this.locZ() - vec3d.z;

        return d0 * d0 + d2 * d2;
    }
}
