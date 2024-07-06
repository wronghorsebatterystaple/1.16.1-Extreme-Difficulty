package AnonymousRand.anonymousrand.extremedifficultyplugin.customentities.mobs;

import AnonymousRand.anonymousrand.extremedifficultyplugin.customentities.mobs.util.AttackLevelingController;
import AnonymousRand.anonymousrand.extremedifficultyplugin.customentities.mobs.util.IAttackLevelingMob;
import AnonymousRand.anonymousrand.extremedifficultyplugin.customentities.mobs.util.ICustomHostile;
import AnonymousRand.anonymousrand.extremedifficultyplugin.customgoals.CustomPathfinderGoalNearestAttackableTarget;
import AnonymousRand.anonymousrand.extremedifficultyplugin.customgoals.NewPathfinderGoalMoveFasterInCobweb;
import AnonymousRand.anonymousrand.extremedifficultyplugin.customgoals.NewPathfinderGoalGetBuffedByMobs;
import AnonymousRand.anonymousrand.extremedifficultyplugin.util.SpawnEntity;
import net.minecraft.server.v1_16_R1.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.function.Predicate;

public class CustomEntityGuardian extends EntityGuardian implements ICustomHostile, IAttackLevelingMob {

    private AttackLevelingController attackLevelingController;

    public CustomEntityGuardian(World world) {
        super(EntityTypes.GUARDIAN, world);
        this.initCustom();
        this.initAttackLevelingMob();
    }

    private void initCustom() {
        /* No longer avoids lava and fire */
        this.a(PathType.LAVA, 0.0F);
        this.a(PathType.DAMAGE_FIRE, 0.0F);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    //                                      ICustomHostile                                       //
    ///////////////////////////////////////////////////////////////////////////////////////////////

    public double getFollowRange() { /* guardians have 24 block detection range (32 after 8 attacks) */
        return (this.attackLevelingController == null || this.getAttacks() < 8) ? 24.0 : 32.0;
    }

    @Override
    public void checkDespawn() {
        if (this.getWorld().getDifficulty() == EnumDifficulty.PEACEFUL && this.L()) {
            this.die();
        } else if (!this.isPersistent() && !this.isSpecialPersistence()) {
            EntityHuman nearestPlayer = this.getWorld().findNearbyPlayer(this, -1.0D);

            if (nearestPlayer != null) {
                /* Mobs only despawn along horizontal axes, so even at y=256, mobs will spawn below you and prevent sleeping */
                double distSqToNearestPlayer = Math.pow(nearestPlayer.getPositionVector().getX() - this.getPositionVector().getX(), 2)
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

                if (this.ticksFarFromPlayer > 600 && random.nextInt(800) == 0 && distSqToNearestPlayer
                        > (double) randomDespawnDistSq && this.isTypeNotPersistent(distSqToNearestPlayer)) {
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
    public double g(double x, double y, double z) {
        double distX = this.locX() - x;
        double distZ = this.locZ() - z;

        return distX * distX + distZ * distZ;
    }

    @Override
    public double d(Vec3D vec3d) {
        double distX = this.locX() - vec3d.x;
        double distZ = this.locZ() - vec3d.z;

        return distX * distX + distZ * distZ;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    //                                    IAttackLevelingMob                                     //
    ///////////////////////////////////////////////////////////////////////////////////////////////

    private void initAttackLevelingMob() {
        this.attackLevelingController = new AttackLevelingController(8, 12, 40);
    }

    public int getAttacks() {
        return this.attackLevelingController.getAttacks();
    }

    public void increaseAttacks(int increase) {
        for (int metThreshold : this.attackLevelingController.increaseAttacks(increase)) {
            int[] attackThresholds = this.attackLevelingController.getAttacksThresholds();
            if (metThreshold == attackThresholds[0]) {
                this.targetSelector.a(0, new CustomPathfinderGoalNearestAttackableTarget<>(this, EntityPlayer.class)); // update follow range
            } else if (metThreshold == attackThresholds[1]) {
                /* After 12 attacks, guardians gain regen 3 and 40 max health */
                ((LivingEntity) this.getBukkitEntity()).setMaxHealth(40.0);
                this.addEffect(new MobEffect(MobEffects.REGENERATION, Integer.MAX_VALUE, 2));
            } else if (metThreshold == attackThresholds[2]) {
                /* After 40 attacks, guardians summon an elder guardian */
                new SpawnEntity(this.getWorld(), new CustomEntityGuardianElder(this.getWorld()), 1, null, null, this, false, true);
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    //                               Overridden vanilla functions                                //
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void initPathfinder() {
        PathfinderGoalMoveTowardsRestriction pathfindergoalmovetowardsrestriction = new PathfinderGoalMoveTowardsRestriction(this, 1.0D);
        this.goalRandomStroll = new PathfinderGoalRandomStroll(this, 1.0D, 80);
        /* Still moves fast in cobwebs */
        this.goalSelector.a(0, new NewPathfinderGoalMoveFasterInCobweb(this));
        /* Takes buffs from bats, piglins, etc. */
        this.goalSelector.a(0, new NewPathfinderGoalGetBuffedByMobs(this));
        this.goalSelector.a(4, new CustomEntityGuardian.PathfinderGoalGuardianAttack(this));
        this.goalSelector.a(5, pathfindergoalmovetowardsrestriction);
        this.goalSelector.a(7, this.goalRandomStroll);
        this.goalSelector.a(8, new PathfinderGoalLookAtPlayer(this, EntityPlayer.class, 8.0F));
        this.goalSelector.a(8, new PathfinderGoalLookAtPlayer(this, EntityGuardian.class, 12.0F, 0.01F));
        this.goalSelector.a(9, new PathfinderGoalRandomLookaround(this));
        this.goalRandomStroll.a(EnumSet.of(PathfinderGoal.Type.MOVE, PathfinderGoal.Type.LOOK));
        pathfindergoalmovetowardsrestriction.a(EnumSet.of(PathfinderGoal.Type.MOVE, PathfinderGoal.Type.LOOK));
        this.targetSelector.a(0, new CustomPathfinderGoalNearestAttackableTarget<>(this, EntityLiving.class, 10, new CustomEntityGuardian.EntitySelectorGuardianTargetHumanSquid(this))); /* Doesn't take into account y-level or line of sight to aggro a target */
    }

    @Override
    public boolean damageEntity(DamageSource damagesource, float f) {
        if (!this.eO() && !damagesource.isMagic() && damagesource.j() instanceof EntityLiving) {
            EntityLiving entityLiving = (EntityLiving) damagesource.j();

            if (!damagesource.isExplosion()) {
                entityLiving.damageEntity(DamageSource.a(this), f * 0.5F); /* thorns damage increased from 2 to 50% of the damage dealt */
                entityLiving.addEffect(new MobEffect(MobEffects.SLOWER_DIG, 400, this.getAttacks() < 55 ? 0 : 1)); /* guardians give players that hit them mining fatigue 1 (2 after 55 attacks) for 20 seconds */
            }
        }

        if (this.goalRandomStroll != null) {
            this.goalRandomStroll.h();
        }

        return super.damageEntity(damagesource, f);
    }

    static class PathfinderGoalGuardianAttack extends PathfinderGoal { /* guardian no longer stops attacking if player is too close */

        private final CustomEntityGuardian guardian;
        private int b;
        private final boolean isElder;

        public PathfinderGoalGuardianAttack(CustomEntityGuardian entityGuardian) {
            this.guardian = entityGuardian;
            this.isElder = false;
            this.a(EnumSet.of(PathfinderGoal.Type.MOVE, PathfinderGoal.Type.LOOK));
        }

        @Override
        public boolean a() {
            EntityLiving entityLiving = this.guardian.getGoalTarget();

            return entityLiving != null && entityLiving.isAlive();
        }

        @Override
        public void c() {
            this.b = -10;
            this.guardian.getNavigation().o();
            this.guardian.getControllerLook().a(this.guardian.getGoalTarget(), 90.0F, 90.0F);
            this.guardian.impulse = true;
        }

        @Override
        public void d() {
            this.guardian.a(0);
            this.guardian.setGoalTarget(null, EntityTargetEvent.TargetReason.CLOSEST_PLAYER, false);
            this.guardian.goalRandomStroll.h();
        }

        @Override
        public void e() {
            EntityLiving entityLiving = this.guardian.getGoalTarget();

            this.guardian.getNavigation().o();
            this.guardian.getControllerLook().a(entityLiving, 90.0F, 90.0F);

            if (entityLiving != null) {
                ++this.b; /* laser no longer disengages when there is a block between guardian and player */

                if (this.b == 0) {
                    this.guardian.a(this.guardian.getGoalTarget().getId());
                    if (!this.guardian.isSilent()) {
                        this.guardian.world.broadcastEntityEffect(this.guardian, (byte) 21);
                    }
                } else if (this.b >= this.guardian.eL()) {
                    float f = 1.0F;

                    if (this.guardian.world.getDifficulty() == EnumDifficulty.HARD) {
                        f += 2.0F;
                    }

                    if (this.isElder) {
                        f += 2.0F;
                    }

                    this.guardian.increaseAttacks(1);
                    entityLiving.damageEntity(DamageSource.c(this.guardian, this.guardian), f);
                    entityLiving.damageEntity(DamageSource.mobAttack(this.guardian), (float) this.guardian.b(GenericAttributes.ATTACK_DAMAGE));
                    this.guardian.setGoalTarget(null, EntityTargetEvent.TargetReason.CLOSEST_PLAYER, false);
                }

                if (this.b >= this.guardian.eL() / 2.5 && this.guardian.ticksLived % (this.guardian.getAttacks() < 10 ? 4 : 3) == 0) { /* tractor beam-like effect every 4 ticks (3 after 10 attacks) for the latter 60% of the laser charging period */
                    LivingEntity bukkitEntity = (LivingEntity) entityLiving.getBukkitEntity();
                    bukkitEntity.setVelocity(new Vector((this.guardian.locX() - bukkitEntity.getLocation().getX()) / 48.0, (this.guardian.locY() - bukkitEntity.getLocation().getY()) / 48.0, (this.guardian.locZ() - bukkitEntity.getLocation().getZ()) / 48.0));

                    if (this.guardian.getAttacks() >= 35) { /* after 35 attacks, guardians inflict poison 1 while the tractor beam is engaged */
                        if (this.guardian.getAttacks() >= 55) { /* after 55 attacks, guardians inflict hunger 1 and weakness 1 while the tractor beam is engaged */
                            entityLiving.addEffect(new MobEffect(MobEffects.HUNGER, 51, 0));
                            entityLiving.addEffect(new MobEffect(MobEffects.WEAKNESS, 51, 0));
                        }

                        entityLiving.addEffect(new MobEffect(MobEffects.POISON, 51, 0));
                    }
                }
            }

            super.e();
        }
    }

    static class EntitySelectorGuardianTargetHumanSquid implements Predicate<EntityLiving> {

        private final EntityGuardian a;

        public EntitySelectorGuardianTargetHumanSquid(EntityGuardian entityGuardian) {
            this.a = entityGuardian;
        }

        public boolean test(@Nullable EntityLiving entityLiving) {
            return (entityLiving instanceof EntityHuman || entityLiving instanceof EntitySquid) && entityLiving.h(this.a) > 9.0D;
        }
    }
}