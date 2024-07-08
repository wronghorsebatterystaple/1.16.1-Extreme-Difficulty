package AnonymousRand.anonymousrand.extremedifficultyplugin.customentities.mobs;

import AnonymousRand.anonymousrand.extremedifficultyplugin.customentities.mobs.util.IAttackLevelingMob;
import AnonymousRand.anonymousrand.extremedifficultyplugin.customentities.mobs.util.ICustomHostile;
import AnonymousRand.anonymousrand.extremedifficultyplugin.customentities.mobs.util.VanillaPathfinderGoalsAccess;
import AnonymousRand.anonymousrand.extremedifficultyplugin.customgoals.*;
import net.minecraft.server.v1_16_R1.*;
import org.bukkit.entity.LivingEntity;

public class CustomEntityRavager extends EntityRavager implements ICustomHostile, IAttackLevelingMob {

    public PathfinderGoalSelector vanillaTargetSelector;
    private int attacks;
    private boolean a20, a60, a90, launchHigh;

    public CustomEntityRavager(World world) {
        super(EntityTypes.RAVAGER, world);
        this.vanillaTargetSelector = super.targetSelector;
        /* No longer avoids fire and lava */
        this.a(PathType.DAMAGE_FIRE, 0.0F);
        this.a(PathType.LAVA, 0.0F);
        this.attacks = 0;
        this.a20 = false;
        this.a60 = false;
        this.a90 = false;
        this.launchHigh = false;
        this.getAttributeInstance(GenericAttributes.ATTACK_DAMAGE).setValue(0.25); /* ravagers have 400 health and extra knockback, but only 0.25 damage */
        this.getAttributeInstance(GenericAttributes.ATTACK_KNOCKBACK).setValue(3.5);
        ((LivingEntity) this.getBukkitEntity()).setMaxHealth(400.0);
        this.setHealth(400.0F);
        this.addEffect(new MobEffect(MobEffects.FASTER_MOVEMENT, Integer.MAX_VALUE, 2)); /* changing attributes don't work on ravagers so ravagers have speed 3 */
        VanillaPathfinderGoalsAccess.removePathfinderGoals(this); // remove vanilla HurtByTarget and NearestAttackableTarget goals and replace them with custom ones
    }

    @Override
    protected void initPathfinder() {
        /* No longer targets villagers */
        super.initPathfinder();
        this.goalSelector.a(0, new NewPathfinderGoalBreakBlocksAround(this, 40, 2, 1, 2, 1, true)); /* Breaks most blocks around the mob periodically */
        this.goalSelector.a(0, new NewPathfinderGoalMoveFasterInCobweb(this)); /* Still moves fast in cobwebs */
        this.goalSelector.a(0, new NewPathfinderGoalGetBuffedByMobs(this)); /* Takes buffs from bats, piglins, etc. */
        this.goalSelector.a(1, new CustomPathfinderGoalMeleeAttack(this, 1.0D)); /* uses the custom melee attack goal that attacks regardless of the y-level */
        this.targetSelector.a(1, new CustomPathfinderGoalNearestAttackableTarget<>(this, EntityPlayer.class)); /* Doesn't take into account y-level or line of sight to initially find a target or and maintain it as the target */
    }

    @Override
    protected void f(EntityLiving entityLiving) { /* ravager can't get stunned and instead heals when its attack is blocked by a shield */
        this.heal(this.attacks < 30 ? 20.0F : 30.0F);
    }

    public boolean getLaunchHigh() {
        return this.launchHigh;
    }

    public void setLaunchHigh(boolean launchHigh) {
        this.launchHigh = launchHigh;
    }

    public double getDetectionRange() { /* ravagers have 40 block detection range (80 after 20 attacks) */
        return this.attacks < 20 ? 40.0 : 80.0;
    }

    @Override
    public void checkDespawn() {
        if (this.world.getDifficulty() == EnumDifficulty.PEACEFUL && this.L()) {
            this.die();
        } else if (!this.isPersistent() && !this.isSpecialPersistence()) {
            EntityHuman nearestPlayer = this.world.findNearbyPlayer(this, -1.0D);

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

    @Override
    public int bL() {
        return Integer.MAX_VALUE;
    }

    public int getAttacks() {
        return this.attacks;
    }

    public void increaseAttacks(int increase) {
        this.attacks += increase;
    }

//    public int[] getAttacksThresholds() {
//        return this.attackLevelingController.getAttacksThresholds();
//    }

    @Override
    public void tick() {
        super.tick();

        if (this.attacks == 20 && !this.a20) { /* after 20 attacks, ravagers gain speed 5 and 1 damage */
            this.a20 = true;
            this.getAttributeInstance(GenericAttributes.ATTACK_DAMAGE).setValue(1.0);
            this.addEffect(new MobEffect(MobEffects.FASTER_MOVEMENT, Integer.MAX_VALUE, 4));
            this.targetSelector.a(1, new CustomPathfinderGoalNearestAttackableTarget<>(this, EntityPlayer.class)); // update follow range
        }

        if (this.attacks == 60 && !this.a60) { /* after 60 attacks, ravagers get extra knockback */
            this.a60 = true;
            this.getAttributeInstance(GenericAttributes.ATTACK_KNOCKBACK).setValue(4.5);
        }

        if (this.attacks == 90 && !this.a90) { /* after 90 attacks, ravagers get regen 3 */
            this.a90 = true;
            this.addEffect(new MobEffect(MobEffects.REGENERATION, Integer.MAX_VALUE, 2));
        }
    }
}
