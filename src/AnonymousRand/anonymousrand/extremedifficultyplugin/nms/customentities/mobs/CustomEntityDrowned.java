package AnonymousRand.anonymousrand.extremedifficultyplugin.nms.customentities.mobs;

import AnonymousRand.anonymousrand.extremedifficultyplugin.nms.customentities.mobs.util.AttackLevelingController;
import AnonymousRand.anonymousrand.extremedifficultyplugin.nms.customentities.mobs.util.IAttackLevelingMob;
import AnonymousRand.anonymousrand.extremedifficultyplugin.nms.customentities.mobs.util.ICustomHostile;
import AnonymousRand.anonymousrand.extremedifficultyplugin.nms.customgoals.*;
import AnonymousRand.anonymousrand.extremedifficultyplugin.util.SpawnEntity;
import net.minecraft.server.v1_16_R1.*;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.Random;

public class CustomEntityDrowned extends EntityDrowned implements ICustomHostile, IAttackLevelingMob {

    /* Ignores line of sight and y-level for initially finding a player target and maintaining it as the target,
       as well as for retaliating against players. Line of sight is also ignored for melee attack pathfinding. */
    private static final boolean IGNORE_LOS = true;
    private static final boolean IGNORE_Y = true;

    public CustomEntityDrowned(World world) {
        super (EntityTypes.DROWNED, world);
        this.initCustom();
        this.initAttackLevelingMob();
    }

    private void initCustom() {
        /* No longer avoids fire and lava */
        this.a(PathType.DAMAGE_FIRE, 0.0F);
        this.a(PathType.LAVA, 0.0F);

        /* Drowned always spawn with tridents */
        this.setSlot(EnumItemSlot.MAINHAND, new ItemStack(Items.TRIDENT));
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    //                                      ICustomHostile                                       //
    ///////////////////////////////////////////////////////////////////////////////////////////////

    /* Drowned have 40 block detection range */
    public double getDetectionRange() {
        return 40.0;
    }

    public boolean ignoresLOS() {
        return IGNORE_LOS;
    }

    public boolean ignoresY() {
        return IGNORE_Y;
    }

    @Override
    public void checkDespawn() {
        if (this.world.getDifficulty() == EnumDifficulty.PEACEFUL && this.L()) {
            this.die();
        } else if (!this.isPersistent() && !this.isSpecialPersistence()) {
            EntityHuman nearestPlayer = this.world.findNearbyPlayer(this, -1.0);

            if (nearestPlayer != null) {
                /* Mobs only despawn along horizontal axes, so even at build height,
                   mobs will spawn below you and prevent sleeping */
                double distSqToNearestPlayer =
                        Math.pow(nearestPlayer.getPositionVector().getX() - this.getPositionVector().getX(), 2)
                        + Math.pow(nearestPlayer.getPositionVector().getZ() - this.getPositionVector().getZ(), 2);
                int forceDespawnDist = this.getEntityType().e().f();
                int forceDespawnDistSq = forceDespawnDist * forceDespawnDist;

                if (distSqToNearestPlayer > (double) forceDespawnDistSq
                        && this.isTypeNotPersistent(distSqToNearestPlayer)) {
                    this.die();
                }

                /* Random despawn distance increased to 40 blocks */
                int randomDespawnDist = this.getEntityType().e().g() + 8;
                int randomDespawnDistSq = randomDespawnDist * randomDespawnDist;

                if (this.ticksFarFromPlayer > 600
                        && random.nextInt(800) == 0
                        && distSqToNearestPlayer > (double) randomDespawnDistSq
                        && this.isTypeNotPersistent(distSqToNearestPlayer)) {
                    this.die();
                } else if (distSqToNearestPlayer < (double) randomDespawnDistSq) {
                    this.ticksFarFromPlayer = 0;
                }
            }
        } else {
            this.ticksFarFromPlayer = 0;
        }
    }

    @Override
    public int bL() {
        return Integer.MAX_VALUE;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    //                                    IAttackLevelingMob                                     //
    ///////////////////////////////////////////////////////////////////////////////////////////////

    private AttackLevelingController attackLevelingController = null;

    private void initAttackLevelingMob() {
        this.attackLevelingController = new AttackLevelingController(150, 350);
    }

    public int getAttacks() {
        return this.attackLevelingController.getAttacks();
    }

    public void increaseAttacks(int increase) {
        for (int metThreshold : this.attackLevelingController.increaseAttacks(increase)) {
            int[] attackThresholds = this.getAttacksThresholds();
            if (metThreshold == attackThresholds[0]) {
                /* After 150 attacks, drowned summon a guardian */
                new SpawnEntity(this.world, new CustomEntityGuardian(this.world), 1, null, null, this, false, true);
            } else if (metThreshold == attackThresholds[1]) {
                /* After 350 attacks, drowned summon an elder guardian */
                new SpawnEntity(this.world, new CustomEntityGuardianElder(this.world), 1, null, null, this, false,
                        true);
            }
        }
    }

    public int[] getAttacksThresholds() {
        return this.attackLevelingController.getAttacksThresholds();
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    //                               Overridden vanilla functions                                //
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void m() {
        /* No longer targets iron golems and villagers */
        this.goalSelector.a(0, new NewPathfinderGoalMoveFasterInCobweb(this));                                 /* Still moves fast in cobwebs */
        this.goalSelector.a(0, new NewPathfinderGoalGetBuffedByMobs(this));                                    /* Takes buffs from bats, piglins, etc. */
        this.goalSelector.a(0, new NewPathfinderGoalSummonLightningRandomly(this, 3.0));                       /* Spawns lightning randomly */
        this.goalSelector.a(1, new CustomEntityDrowned.PathfinderGoalTridentAttack(this, 1.0, 8));             /* Drowned throw tridents every 8 ticks, and continue attacking regardless of line of sight and y-level (the old goal stopped the mob from attacking even if it had already recognized a target via CustomNearestAttackableTarget) */
        this.goalSelector.a(2, new CustomEntityDrowned.PathfinderGoalGoToWater(this, 1.0));
        this.goalSelector.a(2, new CustomPathfinderGoalMeleeAttack<>(this));                              /* Drowned also attack in the day */
        this.goalSelector.a(5, new CustomEntityDrowned.PathfinderGoalGoToBeach(this, 1.0));
        this.goalSelector.a(6, new CustomEntityDrowned.PathfinderGoalSwimUp(this, 1.0, this.world.getSeaLevel()));
        this.goalSelector.a(7, new PathfinderGoalRandomStroll(this, 1.0));
        this.targetSelector.a(0, new CustomPathfinderGoalHurtByTarget<>(this));                                /* Always retaliates against players and teleports to them if they are out of range/do not have line of sight, but doesn't retaliate against other mobs (in case the EntityDamageByEntityEvent listener doesn't register and cancel the damage) */
        this.targetSelector.a(1, new CustomPathfinderGoalNearestAttackableTarget<>(this, EntityPlayer.class)); /* Ignores invis/skulls for initially finding a player target and maintaining it as the target, and periodically retargets the nearest option */
        this.targetSelector.a(2, new CustomPathfinderGoalNearestAttackableTarget<>(this, EntityTurtle.class, false, false, EntityTurtle.bv));
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    //                                Mob-specific goals/classes                                 //
    ///////////////////////////////////////////////////////////////////////////////////////////////

    static class PathfinderGoalGoToBeach extends PathfinderGoalGotoTarget {

        private final CustomEntityDrowned drowned;

        public PathfinderGoalGoToBeach(CustomEntityDrowned drowned, double speed) {
            super(drowned, speed, 8, 2);
            this.drowned = drowned;
        }

        @Override
        public boolean a() {
            return super.a() && !this.drowned.getWorld().isDay() && this.drowned.isInWater()
                    && this.drowned.locY() >= (double) (this.drowned.getWorld().getSeaLevel() - 3);
        }

        @Override // shouldMoveTo()
        protected boolean a(IWorldReader iWorldReader, BlockPosition blockPosition) {
            BlockPosition blockPosition1 = blockPosition.up();

            return (iWorldReader.isEmpty(blockPosition1) && iWorldReader.isEmpty(blockPosition1.up()))
                    ? iWorldReader.getType(blockPosition).a(iWorldReader, blockPosition, this.drowned) : false;
        }

        @Override
        public void c() {
            this.drowned.t(false);
            this.drowned.navigation = this.drowned.navigationLand;
            super.c();
        }
    }

    static class PathfinderGoalGoToWater extends PathfinderGoal {

        private final CustomEntityDrowned drowned;
        private double waterX;
        private double waterY;
        private double waterZ;
        private final double speedTowardsTarget;
        private final World world;

        public PathfinderGoalGoToWater(CustomEntityDrowned drowned, double speedTowardsTarget) {
            this.drowned = drowned;
            this.speedTowardsTarget = speedTowardsTarget;
            this.world = drowned.getWorld();
            this.a(EnumSet.of(PathfinderGoal.Type.MOVE));
        }

        @Override
        public boolean a() {
            if (!this.world.isDay()) {
                return false;
            } else if (this.drowned.isInWater()) {
                return false;
            } else {
                Vec3D vec3d = this.findWaterNearby();

                if (vec3d == null) {
                    return false;
                } else {
                    this.waterX = vec3d.x;
                    this.waterY = vec3d.y;
                    this.waterZ = vec3d.z;
                    return true;
                }
            }
        }

        @Override
        public boolean b() {
            return !this.drowned.getNavigation().m();
        }

        @Override
        public void c() {
            this.drowned.getNavigation().a(this.waterX, this.waterY, this.waterZ, this.speedTowardsTarget);
        }

        @Nullable
        private Vec3D findWaterNearby() {
            Random random = this.drowned.getRandom();
            BlockPosition currentPosition = this.drowned.getChunkCoordinates();

            for (int i = 0; i < 10; ++i) {
                BlockPosition blockPosition =
                        currentPosition.b(random.nextInt(20) - 10, 2 - random.nextInt(8), random.nextInt(20) - 10);

                if (this.world.getType(blockPosition).a(Blocks.WATER)) {
                    return Vec3D.c(blockPosition);
                }
            }

            return null;
        }
    }

    static class PathfinderGoalSwimUp extends PathfinderGoal {

        private final CustomEntityDrowned drowned;
        private final double speedTowardsTarget;
        private final int targetY;
        private boolean obstructed;

        public PathfinderGoalSwimUp(CustomEntityDrowned drowned, double speedTowardsTarget, int targetY) {
            this.drowned = drowned;
            this.speedTowardsTarget = speedTowardsTarget;
            this.targetY = targetY;
        }

        @Override
        public boolean a() {
            return !this.drowned.getWorld().isDay() && this.drowned.isInWater()
                    && this.drowned.locY() < (double) (this.targetY - 2);
        }

        @Override
        public boolean b() {
            return this.a() && !this.obstructed;
        }

        @Override
        public void e() {
            if (this.drowned.locY() < (double) (this.targetY - 1)
                    && (this.drowned.getNavigation().m() || this.drowned.eP())) {
                Vec3D vec3d = RandomPositionGenerator.b(this.drowned, 4, 8,
                        new Vec3D(this.drowned.locX(), this.targetY - 1, this.drowned.locZ()));

                if (vec3d == null) {
                    this.obstructed = true;
                    return;
                }

                this.drowned.getNavigation().a(vec3d.x, vec3d.y, vec3d.z, this.speedTowardsTarget);
            }
        }

        @Override
        public void c() {
            this.drowned.t(true);
            this.obstructed = false;
        }

        @Override
        public void d() {
            this.drowned.t(false);
        }
    }

    // Makes sure trident faces the right direction
    static class PathfinderGoalTridentAttack extends CustomPathfinderGoalRangedAttack<CustomEntityDrowned> {

        public PathfinderGoalTridentAttack(CustomEntityDrowned drowned, double speedTowardsTarget, int attackCooldown) {
            super(drowned, speedTowardsTarget, attackCooldown);
        }

        @Override
        public boolean a() {
            return super.a() && this.goalOwner.getItemInMainHand().getItem() == Items.TRIDENT;
        }

        @Override
        public void c() {
            super.c();
            this.goalOwner.c(EnumHand.MAIN_HAND);
        }

        @Override
        public void d() {
            super.d();
            this.goalOwner.clearActiveItem();
        }
    }
}
