package AnonymousRand.anonymousrand.extremedifficultyplugin.customentities.custommobs;

import AnonymousRand.anonymousrand.extremedifficultyplugin.util.bukkitrunnables.SpiderSilverfishSummonMaterialBlock;
import AnonymousRand.anonymousrand.extremedifficultyplugin.customgoals.*;
import AnonymousRand.anonymousrand.extremedifficultyplugin.util.SpawnLivingEntity;
import net.minecraft.server.v1_16_R1.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.CreatureSpawnEvent;

public class CustomEntitySpider extends EntitySpider {

    public int attacks;
    private boolean a18, a20, a50, a80;

    public CustomEntitySpider(World world) {
        super(EntityTypes.SPIDER, world);
        this.attacks = 0;
        this.a18 = false;
        this.a20 = false;
        this.a50 = false;
        this.a80 = false;
    }

    @Override
    protected void initPathfinder() { /**no longer targets iron golems*/
        this.goalSelector.a(0, new PathfinderGoalFloat(this));
        this.goalSelector.a(0, new NewPathfinderGoalBreakBlocksAround(this, 80, 1, 0, 1, 0, true)); /**custom goal that breaks blocks around the mob periodically*/
        this.goalSelector.a(0, new NewPathfinderGoalTeleportTowardsPlayer(this, this.getFollowRange(), 300.0, 0.00333333333)); /**custom goal that gives mob a chance every tick to teleport to within initial follow_range-2 to follow_range+13 blocks of nearest player if it has not seen a player target within follow range for 15 seconds*/
        this.goalSelector.a(0, new NewPathfinderGoalGetBuffedByMobs(this)); /**custom goal that allows this mob to take certain buffs from bats etc.*/
        this.goalSelector.a(1, new NewPathfinderGoalSpawnBlocksEntitiesOnMob(this, org.bukkit.Material.COBWEB, 1)); /**custom goal that allows spider to summon cobwebs on itself constantly*/
        this.goalSelector.a(3, new PathfinderGoalLeapAtTarget(this, 0.4F));
        this.goalSelector.a(4, new CustomPathfinderGoalMeleeAttack(this, 1.0, true)); /**uses the custom goal that attacks even when line of sight is broken (the old goal stopped the mob from attacking even if the mob has already recognized a target via CustomNearestAttackableTarget goal); this custom goal also allows the spider to continue attacking regardless of light level*/
        this.goalSelector.a(5, new PathfinderGoalRandomStrollLand(this, 0.8D));
        this.goalSelector.a(6, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, 8.0F));
        this.goalSelector.a(6, new PathfinderGoalRandomLookaround(this));
        this.targetSelector.a(1, new CustomPathfinderGoalHurtByTarget(this, new Class[0])); /**custom goal that prevents mobs from retaliating against other mobs in case the mob damage event doesn't register and cancel the damage*/
        this.targetSelector.a(1, new CustomPathfinderGoalNearestAttackableTarget<>(this, EntityHuman.class, false)); /**uses the custom goal which doesn't need line of sight to start attacking (passes to CustomPathfinderGoalNearestAttackableTarget.g() which passes to CustomIEntityAccess.customFindPlayer() which passes to CustomIEntityAccess.customFindEntity() which passes to CustomPathfinderTargetConditions.a() which removes line of sight requirement); this custom goal also allows the spider to continue attacking regardless of light level*/
    }

    public double getFollowRange() { /**spiders have 16 block detection range (setting attribute doesn't work)*/
        return 16.0;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.attacks % 19 == 0 && this.a20) { //reset right before the next cycle
            this.a20 = false;
        }

        if (this.attacks % 20 == 0 && this.attacks != 0 && !this.a20) { /**every 20 attacks, spiders lay down cobwebs that last 5 seconds in a 3 by 3 cube around itself*/
            this.a20 = true;
            new SpiderSilverfishSummonMaterialBlock(this, org.bukkit.Material.COBWEB, 1).run();
        }

        if (this.attacks == 18 && !this.a18) { /**after 18 attacks, spiders gain speed 1*/
            this.a18 = true;
            this.addEffect(new MobEffect(MobEffects.FASTER_MOVEMENT, Integer.MAX_VALUE, 0));
        }

        if (this.attacks == 50 && !this.a50) { /**after 50 attacks, spiders summon 3 vanilla cave spiders*/
            this.a50 = true;
            new SpawnLivingEntity(this.getWorld(), new EntityCaveSpider(EntityTypes.CAVE_SPIDER, this.getWorld()), 3, CreatureSpawnEvent.SpawnReason.DROWNED, null, this, false, true).run();
        }

        if (this.attacks == 80 && !this.a80) { /**after 80 attacks, spiders summon 5 cave spiders*/
            this.a80 = true;
            new SpawnLivingEntity(this.getWorld(), new CustomEntitySpiderCave(this.getWorld()), 5, null, null, this, false, true).run();
        }

        if (this.ticksLived == 10) { /**spiders move 70% faster but only do 1 damage and have 12.5 health*/
            this.getAttributeInstance(GenericAttributes.MOVEMENT_SPEED).setValue(0.51);
            this.getAttributeInstance(GenericAttributes.ATTACK_DAMAGE).setValue(1.0);
            this.setHealth(12.5F);
            ((LivingEntity)this.getBukkitEntity()).setMaxHealth(12.5);
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