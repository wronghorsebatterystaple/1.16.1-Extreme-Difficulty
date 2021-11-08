package AnonymousRand.anonymousrand.extremedifficultyplugin.customentities.custommobs;

import AnonymousRand.anonymousrand.extremedifficultyplugin.customgoals.*;
import AnonymousRand.anonymousrand.extremedifficultyplugin.util.SpawnLivingEntity;
import net.minecraft.server.v1_16_R1.*;
import org.bukkit.entity.LivingEntity;

import java.lang.reflect.Field;
import java.util.Collection;

public class CustomEntityCreeper extends EntityCreeper {

    public Field fuseTicks;

    public CustomEntityCreeper(World world, int fuse) {
        super(EntityTypes.CREEPER, world);
        this.maxFuseTicks = fuse;

        try {
            this.fuseTicks = EntityCreeper.class.getDeclaredField("fuseTicks");
            this.fuseTicks.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void initPathfinder() { /**creeper is no longer scared of cats and ocelots*/
        this.goalSelector.a(0, new NewPathfinderGoalCobwebMoveFaster(this)); /**custom goal that allows non-player mobs to still go fast in cobwebs*/
        this.goalSelector.a(0, new NewPathfinderGoalGetBuffedByMobs(this)); /**custom goal that allows this mob to take certain buffs from bats etc.*/
        this.goalSelector.a(0, new NewPathfinderGoalSummonLightningRandomly(this, 1.0)); /**custom goal that spawns lightning randomly*/
        this.goalSelector.a(0, new NewPathfinderGoalTeleportTowardsPlayer(this, this.getFollowRange(), 300.0, 0.0025)); /**custom goal that gives mob a chance every tick to teleport to within initial follow_range-2 to follow_range+13 blocks of nearest player if it has not seen a player target within follow range for 15 seconds*/
        this.goalSelector.a(0, new NewPathfinderGoalTeleportToPlayerAdjustY(this, 2.5, random.nextDouble() * 5 + 10.0, 0.0002)); /**custom goal that gives mob a chance every tick to teleport to within initial follow_range-2 to follow_range+13 blocks of nearest player if it has not seen a player target within follow range for 15 seconds*/
        this.goalSelector.a(1, new PathfinderGoalFloat(this));
        this.goalSelector.a(2, new PathfinderGoalSwell(this));
        this.goalSelector.a(4, new PathfinderGoalMeleeAttack(this, 1.0D, false));
        this.goalSelector.a(5, new PathfinderGoalRandomStrollLand(this, 0.8D));
        this.goalSelector.a(6, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, 8.0F));
        this.goalSelector.a(6, new PathfinderGoalRandomLookaround(this));
        this.targetSelector.a(1, new CustomPathfinderGoalHurtByTarget(this, new Class[0])); /**custom goal that prevents mobs from retaliating against other mobs in case the mob damage event doesn't register and cancel the damage*/
        this.targetSelector.a(0, new CustomPathfinderGoalNearestAttackableTarget<>(this, EntityHuman.class, true)); /**uses the custom goal which doesn't need line of sight to start attacking (passes to CustomPathfinderGoalNearestAttackableTarget.g() which passes to CustomIEntityAccess.customFindPlayer() which passes to CustomIEntityAccess.customFindEntity() which passes to CustomPathfinderTargetConditions.a() which removes line of sight requirement)*/
    }

    @Override
    public boolean damageEntity(DamageSource damagesource, float f) {
        if (damagesource.getEntity() instanceof EntityPlayer && this.getHealth() - f > 0.0 && random.nextDouble() < (this.isPowered() ? 1.0 : 0.5)) { /**creeper has a 50% chance to duplicate when hit by player and not killed (extra fuse on new creeper) (100% chance to duplicate into 10 if powered)*/
            new SpawnLivingEntity(this.getWorld(), this.maxFuseTicks, new CustomEntityCreeper(this.getWorld(), 20), this.isPowered() ? 10 : 1, null, null, this, false, true).run();
        }

        return super.damageEntity(damagesource, f);
    }

    @Override
    public void explode() {
        if (this.getGoalTarget() != null) {
            if (this.normalGetDistanceSq(this.getPositionVector(), this.getGoalTarget().getPositionVector()) > (this.isPowered() ? 25.0 : 9.0)) { //charged creepers still only explode within 5 blocks of player and normal creepers only explode within 3
                try {
                    this.fuseTicks.setInt(this, 0);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }

                this.addEffect(new MobEffect(MobEffects.FASTER_MOVEMENT, 20, 1)); /**creepers gain speed 2 for 1 second if they inflated and then deflated again*/
                return;
            }
        }

        if (!this.getWorld().isClientSide) {
            if (this.isPowered()) {
                this.getWorld().createExplosion(this, this.locX(), this.locY(), this.locZ(), 15.0F, true, Explosion.Effect.DESTROY); /**charged creepers explode with power 15*/
            } else {
                Explosion.Effect explosion_effect = this.getWorld().getGameRules().getBoolean(GameRules.MOB_GRIEFING) ? Explosion.Effect.DESTROY : Explosion.Effect.NONE;
                this.getWorld().createExplosion(this, this.locX(), this.locY(), this.locZ(), (float)this.explosionRadius, false, explosion_effect);
            }

            this.killed = true;
            this.die();
            this.createEffectCloud();
        }
    }

    @Override
    public void onLightningStrike(EntityLightning entitylightning) {
        super.onLightningStrike(entitylightning);

        this.getAttributeInstance(GenericAttributes.MOVEMENT_SPEED).setValue(0.4); /**charged creepers move 60% faster and have 100 health, but a longer fuse*/
        ((LivingEntity)this.getBukkitEntity()).setMaxHealth(100.0);
        this.setHealth(100.0F);
        this.maxFuseTicks = 30;
        this.targetSelector.a(0, new CustomPathfinderGoalNearestAttackableTarget<>(this, EntityHuman.class, true)); //updates follow range
    }

    private void createEffectCloud() {
        Collection<MobEffect> collection = this.getEffects();

        if (!collection.isEmpty()) {
            EntityAreaEffectCloud entityareaeffectcloud = new EntityAreaEffectCloud(this.getWorld(), this.locX(), this.locY(), this.locZ());

            entityareaeffectcloud.setRadius(2.5F);
            entityareaeffectcloud.setRadiusOnUse(-0.5F);
            entityareaeffectcloud.setWaitTime(10);
            entityareaeffectcloud.setDuration(entityareaeffectcloud.getDuration() / 2);
            entityareaeffectcloud.setRadiusPerTick(-entityareaeffectcloud.getRadius() / (float) entityareaeffectcloud.getDuration());

            for (MobEffect mobeffect : collection) { /**creepers only create area effect clouds of negative effects*/
                if (mobeffect.getMobEffect().equals(MobEffects.SLOWER_MOVEMENT) || mobeffect.getMobEffect().equals(MobEffects.SLOWER_DIG) || mobeffect.getMobEffect().equals(MobEffects.CONFUSION) || mobeffect.getMobEffect().equals(MobEffects.BLINDNESS) || mobeffect.getMobEffect().equals(MobEffects.HUNGER) || mobeffect.getMobEffect().equals(MobEffects.WEAKNESS) || mobeffect.getMobEffect().equals(MobEffects.POISON) || mobeffect.getMobEffect().equals(MobEffects.WITHER) || mobeffect.getMobEffect().equals(MobEffects.LEVITATION) || mobeffect.getMobEffect().equals(MobEffects.UNLUCK) || mobeffect.getMobEffect().equals(MobEffects.BAD_OMEN)) { /**creepers only create area effect clouds of negative effects*/
                    entityareaeffectcloud.addEffect(new MobEffect(mobeffect));
                }
            }

            this.getWorld().addEntity(entityareaeffectcloud);
        }

    }

    public double normalGetDistanceSq(Vec3D vec3d1, Vec3D vec3d2) {
        double d0 = vec3d2.getX() - vec3d1.getX(); //explode function still takes into account y level
        double d1 = vec3d2.getY() - vec3d1.getY();
        double d2 = vec3d2.getZ() - vec3d1.getZ();

        return d0 * d0 + d1 * d1 + d2 * d2;
    }

    public double getFollowRange() { /**creepers have 28 block detection range (64 if powered)*/
        return this.isPowered() ? 64.0 : 28.0;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.isPowered() && this.getGoalTarget() != null && !this.isIgnited()) { /**charged creepers detonate starting 5 blocks away*/
            if (normalGetDistanceSq(this.getPositionVector(), this.getGoalTarget().getPositionVector()) <= 25.0) {
                this.ignite();
            }
        }

        if (this.ticksLived == 10) { /**creepers move 40% faster but only have 12.75 health*/
            this.getAttributeInstance(GenericAttributes.MOVEMENT_SPEED).setValue(0.35);
            this.setHealth(12.75F);
            ((LivingEntity)this.getBukkitEntity()).setMaxHealth(12.75);
        }
    }

    @Override
    public void checkDespawn() {
        if (this.getWorld().getDifficulty() == EnumDifficulty.PEACEFUL && this.L()) {
            this.die();
        } else if (!this.isPersistent() && !this.isSpecialPersistence()) {
            EntityHuman entityhuman = this.getWorld().findNearbyPlayer(this, -1.0D);

            if (entityhuman != null) {
                double d0 = Math.pow(entityhuman.getPositionVector().getX() - this.getPositionVector().getX(), 2) + Math.pow(entityhuman.getPositionVector().getZ() - this.getPositionVector().getZ(), 2); /**mobs only despawn along horizontal axes; if you are at y level 256 mobs will still spawn below you at y64 and prevent sleepingdouble d0 = entityhuman.h(this);*/
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

    @Override
    public int bL() { //getMaxFallHeight
        if (this.getGoalTarget() == null) {
            return 3;
        } else {
            int i = (int)(this.getHealth() * 20.0); /**mobs are willing to take 20 times the fall distance (same damage) to reach and do not stop taking falls if it is at less than 33% health*/

            return i + 3;
        }
    }
}