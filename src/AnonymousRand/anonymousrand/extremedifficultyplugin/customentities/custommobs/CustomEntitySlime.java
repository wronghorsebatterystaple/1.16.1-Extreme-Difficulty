package AnonymousRand.anonymousrand.extremedifficultyplugin.customentities.custommobs;

import AnonymousRand.anonymousrand.extremedifficultyplugin.customentities.custommobs.util.IAttackLevelingMob;
import AnonymousRand.anonymousrand.extremedifficultyplugin.customentities.custommobs.util.ICustomHostile;
import AnonymousRand.anonymousrand.extremedifficultyplugin.customentities.custommobs.util.VanillaPathfinderGoalsAccess;
import AnonymousRand.anonymousrand.extremedifficultyplugin.customgoals.CustomPathfinderGoalNearestAttackableTarget;
import AnonymousRand.anonymousrand.extremedifficultyplugin.customgoals.NewPathfinderGoalBreakBlocksAround;
import AnonymousRand.anonymousrand.extremedifficultyplugin.customgoals.NewPathfinderGoalSlimeMeleeAttack;
import net.minecraft.server.v1_16_R1.*;
import org.bukkit.event.entity.CreatureSpawnEvent;

public class CustomEntitySlime extends EntitySlime implements ICustomHostile, IAttackLevelingMob {

    public PathfinderGoalSelector vanillaTargetSelector;
    private int attacks;
    private boolean a12, a35, deathExplosion;

    public CustomEntitySlime(World world) {
        super(EntityTypes.SLIME, world);
        this.vanillaTargetSelector = super.targetSelector;
        this.a(PathType.LAVA, 0.0F); /** no longer avoids lava */
        this.a(PathType.DAMAGE_FIRE, 0.0F); /** no longer avoids fire */
        this.attacks = 0;
        this.a12 = false;
        this.a35 = false;
        this.deathExplosion = false;
        VanillaPathfinderGoalsAccess.removePathfinderGoals(this); // remove vanilla HurtByTarget and NearestAttackableTarget goals and replace them with custom ones
    }

    public CustomEntitySlime(World world, int size) {
        this(world);
        this.setSize(size, true);
    }

    @Override
    protected void initPathfinder() { /** no longer targets iron golems */
        super.initPathfinder();

        this.goalSelector.a(1, new NewPathfinderGoalSlimeMeleeAttack(this, 1.0)); /** small slimes also do damage; uses the custom goal that attacks regardless of the y level (the old goal stopped the mob from attacking even if the mob has already recognized a target via CustomNearestAttackableTarget goal) */
        this.targetSelector.a(1, new CustomPathfinderGoalNearestAttackableTarget<>(this, EntityPlayer.class)); /** uses the custom goal which doesn't need line of sight to start attacking (passes to CustomPathfinderGoalNearestAttackableTarget.g() which passes to CustomIEntityAccess.customFindPlayer() which passes to CustomIEntityAccess.customFindEntity() which passes to CustomPathfinderTargetConditions.a() which removes line of sight requirement); for some reason the slimes run away after a while without the extra parameters */
    }

    @Override
    public void setSize(int i, boolean flag) { /** toned down stats a bit to account for potential size 8 slimes */
        super.setSize(i, flag);
        this.getAttributeInstance(GenericAttributes.MAX_HEALTH).setValue(1.0 + ((Math.log10(i) / Math.log10(2)) * ((2 * Math.log10(i) + 1) / (Math.log10(1.6))))); // approx: 1 health for size 1, 8.849 health for size 2, 22.596 health for size 4, 42.243 health for size 8
        this.getAttributeInstance(GenericAttributes.MOVEMENT_SPEED).setValue(Math.ceil(0.325F + 0.05F * (float)i)); // 0.375 for 1, 0.425 for 2, 0.525 for 4, 0.725 for 8
        this.getAttributeInstance(GenericAttributes.ATTACK_DAMAGE).setValue(Math.ceil(i / 1.5)); // 1 for 1, 2 for 2, 3 for 4, 6 for 8
        if (flag) {
            this.setHealth(this.getMaxHealth());
        }
    }

    @Override
    protected void j(EntityLiving entityLiving) {} /** slimes use the NewPathfinderGoalSlimeMeleeAttack instead of this attack function */

    @Override
    public void die() {
        super.die();

        if (this.attacks >= 35) { /** after 35 attacks, slimes summon 6-8 mini-slimes when killed instead of 2-4 */
            int i = this.getSize();

            if (!this.world.isClientSide && i > 1 && this.dk()) {
                IChatBaseComponent ichatbasecomponent = this.getCustomName();
                boolean flag = this.isNoAI();
                float f = (float)i / 4.0F;
                int j = i / 2;

                for (int l = 0; l < 4; ++l) {
                    float f1 = ((float)(l % 2) - 0.5F) * f;
                    float f2 = ((float)(l / 2) - 0.5F) * f;
                    EntitySlime entitySlime = this.getEntityType().a(this.world);

                    if (this.isPersistent()) {
                        entitySlime.setPersistent();
                    }

                    entitySlime.setCustomName(ichatbasecomponent);
                    entitySlime.setNoAI(flag);
                    entitySlime.setInvulnerable(this.isInvulnerable());
                    entitySlime.setSize(j, true);
                    entitySlime.setPositionRotation(this.locX() + (double)f1, this.locY() + 0.5D, this.locZ() + (double)f2, random.nextFloat() * 360.0F, 0.0F);
                    this.world.addEntity(entitySlime, CreatureSpawnEvent.SpawnReason.SLIME_SPLIT);
                }
            }
        }
    }

    protected int eK() { /** slimes jump faster */
        return random.nextInt(3) + 6;
    }

    public double getFollowRange() { /** slimes have 40 block detection range (setting attribute doesn't work) */
        return 40.0;
    }

    public int getAttacks() {
        return this.attacks;
    }

    public void increaseAttacks(int increase) {
        this.attacks += increase;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.getHealth() <= 0.0 && this.attacks >= 22 && !this.deathExplosion) { /** after 22 attacks, slimes explode when killed */
            this.deathExplosion = true;
            this.getWorld().createExplosion(this, this.locX(), this.locY(), this.locZ(), (float)(Math.log10(this.getSize()) / Math.log10(2.0)), false, Explosion.Effect.DESTROY);
        }

        if (this.attacks == 12 && !this.a12) { /** after 12 attacks, slimes increase in size by 1 unless it is already at the largest possible size or is going to exceed it */
            this.a12 = true;

            if (this.getSize() < 8) {
                this.setSize(this.getSize() + 1, true);
            }
        }

        if (this.attacks == 35 && !this.a35) { /** after 35 attacks, slimes get extra knockback */
            this.a35 = true;
            this.getAttributeInstance(GenericAttributes.ATTACK_KNOCKBACK).setValue(2.0);
        }

        if (this.ticksLived == 5) {
            if (this.getSize() > 3) {
                this.goalSelector.a(0, new NewPathfinderGoalBreakBlocksAround(this, 40, this.getSize() / 4 + 1, this.getSize() / 4, this.getSize() / 4 + 1, this.getSize() / 4, false)); /** custom goal that breaks blocks around the mob periodically except for diamond blocks, emerald blocks, nertherite blocks, and beacons */
            }
        }
    }

    @Override
    public void checkDespawn() {
        if (this.getWorld().getDifficulty() == EnumDifficulty.PEACEFUL && this.L()) {
            this.die();
        } else if (!this.isPersistent() && !this.isSpecialPersistence()) {
            EntityHuman entityHuman = this.getWorld().findNearbyPlayer(this, -1.0D);

            if (entityHuman != null) {
                double d0 = Math.pow(entityHuman.getPositionVector().getX() - this.getPositionVector().getX(), 2) + Math.pow(entityHuman.getPositionVector().getZ() - this.getPositionVector().getZ(), 2); /** mobs only despawn along horizontal axes; if you are at y level 256 mobs will still spawn below you at y64 and prevent sleepingdouble d0 = entityHuman.h(this); */
                int i = this.getEntityType().e().f();
                int j = i * i;

                if (d0 > (double)j && this.isTypeNotPersistent(d0)) {
                    this.die();
                }

                int k = this.getEntityType().e().g() + 8; /** random despawn distance increased to 40 blocks */
                int l = k * k;

                if (this.ticksFarFromPlayer > 600 && random.nextInt(800) == 0 && d0 > (double)l && this.isTypeNotPersistent(d0)) {
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
        double d3 = this.locX() - d0; /** for determining distance to entities, y level does not matter, e.g. mob follow range, attacking (can hit player no matter the y level) */
        double d5 = this.locZ() - d2;

        return d3 * d3 + d5 * d5;
    }

    @Override
    public double d(Vec3D vec3d) {
        double d0 = this.locX() - vec3d.x; /** for determining distance to entities, y level does not matter, e.g. mob follow range, attacking (can hit player no matter the y level) */
        double d2 = this.locZ() - vec3d.z;

        return d0 * d0 + d2 * d2;
    }

    @Override
    public int bL() {
        return Integer.MAX_VALUE; /** mobs are willing to take any fall to reach the player as they don't take fall damage */
    }
}
