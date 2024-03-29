package AnonymousRand.anonymousrand.extremedifficultyplugin.customentities.custommobs;

import AnonymousRand.anonymousrand.extremedifficultyplugin.customentities.custommobs.util.AttackController;
import AnonymousRand.anonymousrand.extremedifficultyplugin.customentities.custommobs.util.IAttackLevelingMob;
import AnonymousRand.anonymousrand.extremedifficultyplugin.customentities.custommobs.util.ICustomHostile;
import AnonymousRand.anonymousrand.extremedifficultyplugin.customentities.custommobs.util.VanillaPathfinderGoalsAccess;
import AnonymousRand.anonymousrand.extremedifficultyplugin.customgoals.*;
import net.minecraft.server.v1_16_R1.*;
import org.bukkit.entity.LivingEntity;

import java.util.stream.Collectors;

public class CustomEntityIllusionerFake extends CustomEntityIllusioner implements ICustomHostile, IAttackLevelingMob {

    private AttackController attackController;
    private final CustomEntityIllusioner parentIllusioner;
    private boolean deathExplosion;

    public CustomEntityIllusionerFake(World world, CustomEntityIllusioner parentIllusioner) {
        super(world);
        this.parentIllusioner = parentIllusioner;
    }

    @Override // yes, this works
    public void initCustom() {
        /** No longer avoids lava */
        this.a(PathType.LAVA, 0.0F);
        /** No longer avoids fire */
        this.a(PathType.DAMAGE_FIRE, 0.0F);

        this.setSlot(EnumItemSlot.MAINHAND, new ItemStack(Items.BOW)); // makes sure that it has a bow
        this.deathExplosion = false;

        this.initAttributes();
    }

    private void initAttributes() {
        float health = (float) (random.nextDouble() * 12.0 + 20.0); /** fake illusioners have anywhere between 20 and 32 health */
        this.setHealth(health);
        ((LivingEntity)this.getBukkitEntity()).setMaxHealth(health);
    }

    //////////////////////////  IAttackLevelingMob  //////////////////////////
    public void initAttacks() {
        this.attackController = new AttackController(20, 40);
    }

    public int getAttacks() {
        return this.attackController.getAttacks();
    }

    public void increaseAttacks(int increase) {
        for (int metThreshold : this.attackController.increaseAttacks(increase)) {
            int[] attackThresholds = this.attackController.getAttackThresholds();
            if (metThreshold == attackThresholds[0]) {
                /** After 20 attacks, summoned fake illusioners attack faster */
                for (PathfinderGoal goal : VanillaPathfinderGoalsAccess.getPathfinderGoals(this.goalSelector.d().collect(Collectors.toSet()), CustomPathfinderGoalRangedBowAttack.class)) {
                    ((CustomPathfinderGoalRangedBowAttack<?>) goal).changeAttackInterval(random.nextInt(9) + 12);
                }
            } else if (metThreshold == attackThresholds[1]) {
                /** After 40 attacks, summoned fake illusioners attack even faster */
                for (PathfinderGoal goal : VanillaPathfinderGoalsAccess.getPathfinderGoals(this.goalSelector.d().collect(Collectors.toSet()), CustomPathfinderGoalRangedBowAttack.class)) {
                    ((CustomPathfinderGoalRangedBowAttack<?>) goal).changeAttackInterval(random.nextInt(4) + 5);
                }
            }
        }
    }

    /////////////////////  Overridden vanilla functions  /////////////////////
    @Override
    public void initPathfinder() { /** no longer target iron golems or villagers, and only shoots arrows (can't apply spells) */
        this.goalSelector.a(1, new EntityRaider.b<>(this));
        this.goalSelector.a(3, new PathfinderGoalRaid<>(this));
        this.goalSelector.a(5, new CustomEntityIllusioner.c(this));
        this.goalSelector.a(4, new CustomEntityIllusioner.d(this, 1.0499999523162842D, 1));

        /** Still moves fast in cobwebs */
        this.goalSelector.a(0, new NewPathfinderGoalCobwebMoveFaster(this));
        /** Takes buffs from bats and piglins etc. */
        this.goalSelector.a(0, new NewPathfinderGoalGetBuffedByMobs(this));
        this.goalSelector.a(0, new NewPathfinderGoalBreakBlockLookingAt(this)); /** custom goal that allows the mob to break the block it is looking at every 4 seconds as long as it has a target, it breaks the block that it is looking at up to 40 blocks away */
        this.goalSelector.a(0, new PathfinderGoalFloat(this));
        this.goalSelector.a(1, new EntityIllagerWizard.b());
        this.goalSelector.a(6, new CustomPathfinderGoalRangedBowAttack<>(this, 0.5D, random.nextInt(11) + 20, 32.0F)); /** uses the custom goal that attacks regardless of the y level (the old goal stopped the mob from attacking even if the mob has already recognized a target via CustomNearestAttackableTarget goal) */
        this.goalSelector.a(8, new PathfinderGoalRandomStroll(this, 0.6D));
        this.goalSelector.a(9, new PathfinderGoalLookAtPlayer(this, EntityPlayer.class, 3.0F, 1.0F));
        this.goalSelector.a(10, new PathfinderGoalLookAtPlayer(this, EntityInsentient.class, 8.0F));
        this.targetSelector.a(1, (new CustomPathfinderGoalHurtByTarget(this, new Class[0])));
        this.targetSelector.a(2, (new CustomPathfinderGoalNearestAttackableTarget<>(this, EntityPlayer.class)).a(300)); /** uses the custom goal which doesn't need line of sight to start attacking (passes to CustomPathfinderGoalNearestAttackableTarget.g() which passes to CustomIEntityAccess.customFindPlayer() which passes to CustomIEntityAccess.customFindEntity() which passes to CustomPathfinderTargetConditions.a() which removes line of sight requirement) */
    }

    @Override
    public void tick() {
        super.tick();

        if (this.getHealth() <= 0.0 && this.parentIllusioner.getAttacks() >= 12 && !this.deathExplosion) { /** after 12 attacks, summoned fake illusioners explode when killed */
            this.deathExplosion = true;
            this.getWorld().createExplosion(this, this.locX(), this.locY(), this.locZ(), 1.0F, false, Explosion.Effect.NONE);
        }

        if (this.ticksLived >= 1500) { /** fake illusioners die after 75 seconds */
            this.die();
        }
    }
}
