package AnonymousRand.ExtremeDifficultyPlugin.customEntities.customMobs;

import AnonymousRand.ExtremeDifficultyPlugin.customGoals.CustomPathfinderGoalNearestAttackableTarget;
import AnonymousRand.ExtremeDifficultyPlugin.customGoals.CustomPathfinderTargetCondition;
import net.minecraft.server.v1_16_R1.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;

public class CustomEntityGuardianElder extends EntityGuardianElder {

    public CustomEntityGuardianElder(World world) {
        super(EntityTypes.ELDER_GUARDIAN, world);
    }

    @Override
    public void initPathfinder() { /**no longer randomly looks around or at guardians (only at players)*/
        PathfinderGoalMoveTowardsRestriction pathfindergoalmovetowardsrestriction = new PathfinderGoalMoveTowardsRestriction(this, 1.0D);
        this.goalRandomStroll = new PathfinderGoalRandomStroll(this, 1.0D, 80);
        this.goalSelector.a(3, new CustomEntityGuardianElder.CustomPathfinderGoalGuardianAttack(this)); //todo: custom look at player goal
        this.goalSelector.a(4, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, 40.0F, 1.0f)); //always looks at player so blocks are broken in that direction
        this.goalSelector.a(5, pathfindergoalmovetowardsrestriction);
        this.goalSelector.a(7, this.goalRandomStroll);
        this.goalRandomStroll.a(EnumSet.of(PathfinderGoal.Type.MOVE, PathfinderGoal.Type.LOOK));
        pathfindergoalmovetowardsrestriction.a(EnumSet.of(PathfinderGoal.Type.MOVE, PathfinderGoal.Type.LOOK));
        this.targetSelector.a(1, new CustomPathfinderGoalNearestAttackableTarget<>(this, EntityLiving.class, 10, false, false, new CustomEntityGuardianElder.EntitySelectorGuardianTargetHumanSquid(this))); /**uses the custom goal which doesn't need line of sight to start shooting at players (passes to CustomPathfinderGoalNearestAttackableTarget.g() which passes to CustomIEntityAccess.customFindPlayer() which passes to CustomIEntityAccess.customFindEntity() which passes to CustomPathfinderTargetConditions.a() which removes line of sight requirement)*/
    }

    @Override
    public boolean damageEntity(DamageSource damagesource, float f) {
        if (!this.eO() && !damagesource.isMagic() && damagesource.j() instanceof EntityLiving) {
            EntityLiving entityliving = (EntityLiving) damagesource.j();

            if (!damagesource.isExplosion()) {
                entityliving.damageEntity(DamageSource.a(this), 6.0F); /**thorns damage increased from 2 to 6*/
            }
        }

        if (this.goalRandomStroll != null) {
            this.goalRandomStroll.h();
        }

        return super.damageEntity(damagesource, f);
    }

    @Override
    protected void mobTick() {
        super.mobTick();

        if ((this.ticksLived  + this.getId()) % 20 == 0) { /**applies mining fatigue every second*/
            MobEffectList mobeffectlist = MobEffects.SLOWER_DIG;
            List<EntityPlayer> list = ((WorldServer)this.world).a((entityplayer) -> {
                return this.h((Entity)entityplayer) < 2500.0D && entityplayer.playerInteractManager.d();
            });

            Iterator iterator = list.iterator();

            while (iterator.hasNext()) {
                EntityPlayer entityplayer = (EntityPlayer)iterator.next();

                //plays the animation every time mining fatigue happens (every second)
                entityplayer.playerConnection.sendPacket(new PacketPlayOutGameStateChange(PacketPlayOutGameStateChange.k, this.isSilent() ? 0.0F : 1.0F));
                entityplayer.addEffect(new MobEffect(mobeffectlist, 6000, 2));
            }
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (this.ticksLived % 40 == 10) { /**elder guardians have 40 block detection range (setting attribute doesn't work)*/
            EntityPlayer player = this.getWorld().a(EntityPlayer.class, new CustomPathfinderTargetCondition(), this, this.locX(), this.locY(), this.locZ(), this.getBoundingBox().grow(40.0, 128.0, 40.0)); //get closest player within bounding box
            if (player != null && this.getGoalTarget() == null) {
                this.setGoalTarget(player);
            }
        }

        Location thisLoc = new Location(this.getWorld().getWorld(), this.locX(), this.locY(), this.locZ());
        Location thisLoc2 = new Location(this.getWorld().getWorld(), this.locX(), this.locY() + 1.0, this.locZ());
        if (thisLoc.getBlock().getType() == org.bukkit.Material.COBWEB || thisLoc2.getBlock().getType() == org.bukkit.Material.COBWEB) { /**non-player mobs gain Speed 11 while in a cobweb (approx original speed)*/
            this.addEffect(new MobEffect(MobEffects.FASTER_MOVEMENT, 2, 10));
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

                if (d0 > (double) j && this.isTypeNotPersistent(d0)) {
                    this.die();
                }

                int k = this.getEntityType().e().g() + 28; /**random despawn distance increased to 60 blocks*/
                int l = k * k;

                if (this.ticksFarFromPlayer > 600 && this.random.nextInt(800) == 0 && d0 > (double)l && this.isTypeNotPersistent(d0)) {
                    this.die();
                } else if (d0 < (double) l) {
                    this.ticksFarFromPlayer = 0;
                }
            }

        } else {
            this.ticksFarFromPlayer = 0;
        }
    }

    @Override
    public double g(double d0, double d1, double d2) {
        double d3 = this.locX() - d0; /**for determining distance to entities, y-level does not matter, eg. mob follow range*/
        double d5 = this.locZ() - d2;

        return d3 * d3 + d5 * d5;
    }

    @Override
    public double d(Vec3D vec3d) {
        double d0 = this.locX() - vec3d.x; /**for determining distance to entities, y-level does not matter, eg. mob follow range*/
        double d2 = this.locZ() - vec3d.z;

        return d0 * d0 + d2 * d2;
    }

    static class CustomPathfinderGoalGuardianAttack extends PathfinderGoal { /**guardian no longer stops attacking if player is too close*/

    private final CustomEntityGuardianElder entity;
        private int b;
        private final boolean isElder;

        public CustomPathfinderGoalGuardianAttack(CustomEntityGuardianElder entityguardian) {
            this.entity = entityguardian;
            this.isElder = true;
            this.a(EnumSet.of(PathfinderGoal.Type.MOVE, PathfinderGoal.Type.LOOK));
        }

        @Override
        public boolean a() {
            EntityLiving entityliving = this.entity.getGoalTarget();

            return entityliving != null && entityliving.isAlive();
        }

        @Override
        public void c() {
            this.b = -10;
            this.entity.getNavigation().o();
            this.entity.getControllerLook().a(this.entity.getGoalTarget(), 90.0F, 90.0F);
            this.entity.impulse = true;
        }

        @Override
        public void d() {
            this.entity.a(0);
            this.entity.setGoalTarget((EntityLiving) null);
            this.entity.goalRandomStroll.h();
        }

        @Override
        public void e() {
            EntityLiving entityliving = this.entity.getGoalTarget();

            this.entity.getNavigation().o();
            this.entity.getControllerLook().a(entityliving, 90.0F, 90.0F);

            if (entityliving != null) {
                ++this.b; /**laser no longer disengages when there is a block between guardian and player*/

                if (this.b == 0) {
                    this.entity.a(this.entity.getGoalTarget().getId());
                    if (!this.entity.isSilent()) {
                        this.entity.world.broadcastEntityEffect(this.entity, (byte) 21);
                    }
                } else if (this.b >= this.entity.eL()) {
                    float f = 1.0F;

                    if (this.entity.world.getDifficulty() == EnumDifficulty.HARD) {
                        f += 2.0F;
                    }

                    if (this.isElder) {
                        f += 2.0F;
                    }

                    //todo: not breaking enough blocks (see: blockiterator along a vector) https://hub.spigotmc.org/javadocs/spigot/org/bukkit/util/BlockIterator.html#BlockIterator(org.bukkit.World,%20org.bukkit.util.Vector,%20org.bukkit.util.Vector,%20double,%20int)
                    ArrayList<Block> blocks = (ArrayList<Block>)((LivingEntity)this.entity.getBukkitEntity()).getLineOfSight(null, 20); //gets all blocks in elder guardian's line of sight up to 20 blocks away including all transparent blocks
                    Random rand = new Random();

                    BlockIterator iterator = new BlockIterator((LivingEntity)this.entity.getBukkitEntity(), 20);
                    while (iterator.hasNext()) {
                        if (iterator.next().getType() != org.bukkit.Material.BEDROCK && iterator.next().getType() != org.bukkit.Material.END_GATEWAY && iterator.next().getType() != org.bukkit.Material.END_PORTAL && iterator.next().getType() != org.bukkit.Material.END_PORTAL_FRAME && iterator.next().getType() != org.bukkit.Material.NETHER_PORTAL && iterator.next().getType() != org.bukkit.Material.OBSIDIAN && iterator.next().getType() != org.bukkit.Material.CRYING_OBSIDIAN && iterator.next().getType() != org.bukkit.Material.COMMAND_BLOCK && iterator.next().getType() != org.bukkit.Material.COMMAND_BLOCK_MINECART && iterator.next().getType() != org.bukkit.Material.STRUCTURE_BLOCK && iterator.next().getType() != org.bukkit.Material.JIGSAW && iterator.next().getType() != org.bukkit.Material.BARRIER && iterator.next().getType() != org.bukkit.Material.END_STONE && iterator.next().getType() != org.bukkit.Material.SPAWNER && iterator.next().getType() != org.bukkit.Material.COBWEB) { //as long as it isn't one of these blocks
                            iterator.next().setType(org.bukkit.Material.AIR);
                        }
                    }

                    /*for (Block b : blocks) { /**elder guardians break all blocks in a 3 by 3 by 3 in its line of sight when the laser fires*/
                        /*Location locBase = b.getLocation();
                        Location loc = locBase;

                        for (int x = -1; x <= 1; x++) {
                            for (int y = -1; y <= 1; y++) {
                                for (int z = -1; z <= 1; z++) {
                                    loc = new Location(this.entity.getWorld().getWorld(), locBase.getX() + x, locBase.getY() + y, locBase.getZ() + z);

                                    if (loc.getBlock().getType() != org.bukkit.Material.BEDROCK && loc.getBlock().getType() != org.bukkit.Material.END_GATEWAY && loc.getBlock().getType() != org.bukkit.Material.END_PORTAL && loc.getBlock().getType() != org.bukkit.Material.END_PORTAL_FRAME && loc.getBlock().getType() != org.bukkit.Material.NETHER_PORTAL && loc.getBlock().getType() != org.bukkit.Material.OBSIDIAN && loc.getBlock().getType() != org.bukkit.Material.CRYING_OBSIDIAN && loc.getBlock().getType() != org.bukkit.Material.COMMAND_BLOCK && loc.getBlock().getType() != org.bukkit.Material.COMMAND_BLOCK_MINECART && loc.getBlock().getType() != org.bukkit.Material.STRUCTURE_BLOCK && loc.getBlock().getType() != org.bukkit.Material.JIGSAW && loc.getBlock().getType() != org.bukkit.Material.BARRIER && loc.getBlock().getType() != org.bukkit.Material.END_STONE && loc.getBlock().getType() != org.bukkit.Material.SPAWNER && loc.getBlock().getType() != org.bukkit.Material.COBWEB) { //as long as it isn't one of these blocks
                                        loc.getBlock().setType(org.bukkit.Material.AIR);
                                    } else if (loc.getBlock().getType() == org.bukkit.Material.OBSIDIAN || loc.getBlock().getType() == org.bukkit.Material.CRYING_OBSIDIAN || loc.getBlock().getType() == org.bukkit.Material.ANCIENT_DEBRIS || loc.getBlock().getType() == org.bukkit.Material.NETHERITE_BLOCK) { //50% chance to break these blocks
                                        if (rand.nextDouble() < 0.5) {
                                            loc.getBlock().setType(org.bukkit.Material.AIR);
                                        }
                                    }
                                }
                            }
                        }
                    }*/

                    entityliving.damageEntity(DamageSource.c(this.entity, this.entity), f);
                    entityliving.damageEntity(DamageSource.mobAttack(this.entity), (float)this.entity.b(GenericAttributes.ATTACK_DAMAGE));
                    this.entity.setGoalTarget((EntityLiving)null);
                }

                if (this.b >= this.entity.eL() / 3.35 && this.entity.ticksLived % 3 == 0) { /**stronger tractor beam-like effect every 3 ticks for the latter ~70% of the laser charging period*/
                    LivingEntity bukkitEntity = (LivingEntity)entityliving.getBukkitEntity();
                    bukkitEntity.setVelocity(new Vector((this.entity.locX() - bukkitEntity.getLocation().getX()) / 20.0, (this.entity.locY() - bukkitEntity.getLocation().getY()) / 20.0, (this.entity.locZ() - bukkitEntity.getLocation().getZ()) / 20.0));
                }
            }

            super.e();
        }
    }

    static class EntitySelectorGuardianTargetHumanSquid implements Predicate<EntityLiving> {

        private final EntityGuardian a;

        public EntitySelectorGuardianTargetHumanSquid(EntityGuardian entityguardian) {
            this.a = entityguardian;
        }

        public boolean test(@Nullable EntityLiving entityliving) {
            return (entityliving instanceof EntityHuman || entityliving instanceof EntitySquid) && entityliving.h((Entity)this.a) > 9.0D;
        }
    }
}
