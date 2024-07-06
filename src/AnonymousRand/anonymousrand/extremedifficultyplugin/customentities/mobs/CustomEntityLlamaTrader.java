package AnonymousRand.anonymousrand.extremedifficultyplugin.customentities.mobs;

import AnonymousRand.anonymousrand.extremedifficultyplugin.customentities.mobs.util.AttackLevelingController;
import AnonymousRand.anonymousrand.extremedifficultyplugin.customentities.mobs.util.IAttackLevelingMob;
import AnonymousRand.anonymousrand.extremedifficultyplugin.customentities.mobs.util.ICustomHostile;
import AnonymousRand.anonymousrand.extremedifficultyplugin.customentities.projectiles.CustomEntityLlamaSpit;
import AnonymousRand.anonymousrand.extremedifficultyplugin.customgoals.CustomPathfinderGoalHurtByTarget;
import AnonymousRand.anonymousrand.extremedifficultyplugin.customgoals.CustomPathfinderGoalNearestAttackableTarget;
import AnonymousRand.anonymousrand.extremedifficultyplugin.customgoals.NewPathfinderGoalMoveFasterInCobweb;
import AnonymousRand.anonymousrand.extremedifficultyplugin.customgoals.NewPathfinderGoalGetBuffedByMobs;
import net.minecraft.server.v1_16_R1.*;
import org.bukkit.entity.LivingEntity;

import java.lang.reflect.Field;

public class CustomEntityLlamaTrader extends EntityLlamaTrader implements ICustomHostile, IAttackLevelingMob {

    private AttackLevelingController attackLevelingController;
    private Field didSpit;

    public CustomEntityLlamaTrader(World world) {
        super(EntityTypes.TRADER_LLAMA, world);
        this.initCustom();
        this.initAttackLevelingMob();
    }

    private void initCustom() {
        this.initAttributes();

        try {
            this.didSpit = EntityLlama.class.getDeclaredField("bH");
            this.didSpit.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }

        /* No longer avoids lava and fire */
        this.a(PathType.LAVA, 0.0F);
        this.a(PathType.DAMAGE_FIRE, 0.0F);

        this.setStrength(1); /* makes sure wolves etc. don't run away from llamas; also makes their inventory smaller */
    }

    private void initAttributes() {
        ((LivingEntity) this.getBukkitEntity()).setMaxHealth(30.0); /* Trader llamas have 30 health */
        this.setHealth(30.0F);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    //                                      ICustomHostile                                       //
    ///////////////////////////////////////////////////////////////////////////////////////////////

    public double getFollowRange() { /* trader llamas have 24 block detection range */
        return 24.0;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    //                                    IAttackLevelingMob                                     //
    ///////////////////////////////////////////////////////////////////////////////////////////////

    private void initAttackLevelingMob() {
        this.attackLevelingController = new AttackLevelingController(15);
    }

    public int getAttacks() {
        return this.attackLevelingController.getAttacks();
    }

    public void increaseAttacks(int increase) {
        for (int metThreshold : this.attackLevelingController.increaseAttacks(increase)) {
            int[] attackThresholds = this.attackLevelingController.getAttacksThresholds();
            if (metThreshold == attackThresholds[0]) {
                /* After 15 attacks, trader llamas get 50 max health and health */
                ((LivingEntity) this.getBukkitEntity()).setMaxHealth(50.0);
                this.setHealth(50.0F);
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    //                               Overridden vanilla functions                                //
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected void initPathfinder() { /* llamas won't panic anymore, are always aggro towards players, don't stop attacking after spitting once, and no longer defend wolves */
        /* Still moves fast in cobwebs */
        this.goalSelector.a(0, new NewPathfinderGoalMoveFasterInCobweb(this));
        /* Takes buffs from bats, piglins, etc. */
        this.goalSelector.a(0, new NewPathfinderGoalGetBuffedByMobs(this));
        this.goalSelector.a(0, new PathfinderGoalFloat(this));
        this.goalSelector.a(2, new PathfinderGoalLlamaFollow(this, 2.0999999046325684D));
        this.goalSelector.a(3, new PathfinderGoalArrowAttack(this, 1.25D, 40, 20.0F));
        this.goalSelector.a(4, new PathfinderGoalBreed(this, 1.0D));
        this.goalSelector.a(5, new PathfinderGoalFollowParent(this, 1.0D));
        this.goalSelector.a(6, new PathfinderGoalRandomStrollLand(this, 0.7D));
        this.goalSelector.a(7, new PathfinderGoalLookAtPlayer(this, EntityPlayer.class, 6.0F));
        this.goalSelector.a(8, new PathfinderGoalRandomLookaround(this));
        this.targetSelector.a(1, new CustomPathfinderGoalHurtByTarget(this, new Class[0])); /* custom goal that allows llama to keep spitting indefinitely and prevents mobs from retaliating against other mobs in case the EntityDamageByEntityEvent listener doesn't register and cancel the damage */
        this.targetSelector.a(2, new CustomPathfinderGoalNearestAttackableTarget<>(this, EntityPlayer.class)); /* Doesn't take into account y-level or line of sight to aggro a target */
    }

    public void a(EntityLiving entityLiving, float f) { // shoot()s custom spit instead of vanilla
        this.increaseAttacks(1);

        CustomEntityLlamaSpit entityLlamaspit = new CustomEntityLlamaSpit(this.getWorld(), this, this.getAttacks() < 6 ? 12.0 : 18.0); /* after 6 attacks, trader llamas do 18 damage */
        double d0 = entityLiving.locX() - this.locX();
        double d1 = entityLiving.e(0.3333333333333333D) - entityLlamaspit.locY();
        double d2 = entityLiving.locZ() - this.locZ();
        f = MathHelper.sqrt(d0 * d0 + d2 * d2) * 0.2F;

        entityLlamaspit.shoot(d0, d1 + (double) f, d2, 1.5F, 10.0F);
        if (!this.isSilent()) {
            this.getWorld().playSound(null, this.locX(), this.locY(), this.locZ(), SoundEffects.ENTITY_LLAMA_SPIT, this.getSoundCategory(), 1.0F, 1.0F + (random.nextFloat() - random.nextFloat()) * 0.2F);
        }

        this.getWorld().addEntity(entityLlamaspit);

        try {
            this.didSpit.setBoolean(this, true);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}