package teamroots.emberroot.entity.golem;

import java.awt.Color;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackMelee;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAIMoveTowardsRestriction;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import teamroots.emberroot.Const;
import teamroots.emberroot.config.ConfigSpawnEntity;
import teamroots.emberroot.entity.deer.EntityDeer;

public class EntityAncientGolem extends EntityMob {

  public static final DataParameter<Integer> variant = EntityDataManager.<Integer> createKey(EntityAncientGolem.class, DataSerializers.VARINT);
  public static final DataParameter<Integer> FIRESPEED = EntityDataManager.<Integer> createKey(EntityAncientGolem.class, DataSerializers.VARINT);
  public static final String NAME = "rainbow_golem";
  public static SoundEvent ambientSound;
  public static SoundEvent hurtSound;
  public static SoundEvent deathSound;
  public static float speedup = 1;
  public static float fireRateModifier = 1;

  public static enum VariantColors {
    RED, ORANGE, YELLOW, GREEN, BLUE, PURPLE;

    public String nameLower() {
      return this.name().toLowerCase();
    }

    /**
     * r,g,b passed into projectile shot
     * 
     * @return
     */
    public Color getColor() {
      switch (this) {
        case BLUE:
          return new Color(0, 173, 255);
        case GREEN:
          return new Color(57, 255, 56);
        case ORANGE:
          return new Color(255, 64, 16);
        case PURPLE:
          return new Color(255, 56, 249);
        case RED:
          return new Color(179, 3, 2);
        case YELLOW:
          return new Color(227, 225, 2);
        default:
        break;
      }
      return null;//new Color(0, 0, 0);
    }
  }

  public static ConfigSpawnEntity config = new ConfigSpawnEntity(EntityAncientGolem.class, EnumCreatureType.MONSTER);
  public static boolean attacksSomeMobs;

  public EntityAncientGolem(World worldIn) {
    super(worldIn);
    setSize(0.6f, 1.8f);
    this.experienceValue = 10;
  }

  public Integer getVariant() {
    return getDataManager().get(variant);
  }

  public VariantColors getVariantEnum() {
    return VariantColors.values()[getVariant()];
  }

  @Override
  protected void entityInit() {
    super.entityInit();
    this.getDataManager().register(FIRESPEED, MathHelper.getInt(rand, 40, 110));
    this.getDataManager().register(variant, rand.nextInt(VariantColors.values().length));
    switch (this.getVariantEnum()) {
      case ORANGE:
      case RED:
      case PURPLE:
        this.isImmuneToFire = true;
      break;
      case BLUE:
      case GREEN:
      default:
        this.isImmuneToFire = false;
      break;
    }
  }

  @Override
  public String getName() {
    if (this.hasCustomName()) {
      return this.getCustomNameTag();
    }
    else {
      String s = EntityList.getEntityString(this);
      if (s == null) {
        s = "generic";
      }
      String var = this.getVariantEnum().nameLower();
      return I18n.translateToLocal("entity." + s + "." + var + ".name");
    }
  }

  @Override
  protected void initEntityAI() {
    this.tasks.addTask(0, new EntityAISwimming(this));
    this.tasks.addTask(2, new EntityAIAttackMelee(this, 0.46D*speedup, true));
    this.tasks.addTask(5, new EntityAIMoveTowardsRestriction(this, 0.46D*speedup));
    this.tasks.addTask(7, new EntityAIWander(this, 0.46D*speedup));
    this.tasks.addTask(8, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
    this.tasks.addTask(8, new EntityAILookIdle(this));
    this.targetTasks.addTask(2, new EntityAINearestAttackableTarget(this, EntityPlayer.class, true));
    if (attacksSomeMobs) {
      switch (this.getVariantEnum()) {
        case BLUE:
          this.targetTasks.addTask(3, new EntityAINearestAttackableTarget(this, EntitySlime.class, true));
        break;
        case GREEN:
          this.targetTasks.addTask(3, new EntityAINearestAttackableTarget(this, EntityZombie.class, true));
        break;
        case ORANGE:
          this.targetTasks.addTask(3, new EntityAINearestAttackableTarget(this, EntitySkeleton.class, true));
        break;
        case PURPLE:
          this.targetTasks.addTask(3, new EntityAINearestAttackableTarget(this, EntityEnderman.class, true));
        break;
        case RED:
          this.targetTasks.addTask(3, new EntityAINearestAttackableTarget(this, EntityPigZombie.class, true));
        break;
        case YELLOW://gold is the only one starting passive to the player
          this.targetTasks.addTask(3, new EntityAINearestAttackableTarget(this, EntityDeer.class, true));
        break;
        default:
        break;
      }
    }
  }

  @Override
  protected void applyEntityAttributes() {
    super.applyEntityAttributes();
    this.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0D);
    //    this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.5D);
    ConfigSpawnEntity.syncInstance(this, config.settings);
  }

  @Override
  public void onUpdate() {
    super.onUpdate();
    this.rotationYaw = this.rotationYawHead;
    if (this.ticksExisted % getDataManager().get(FIRESPEED) * fireRateModifier == 0 && this.getAttackTarget() != null) {
      if (!getEntityWorld().isRemote) {
        EntityGolemLaser proj = new EntityGolemLaser(getEntityWorld());
        proj.getDataManager().set(EntityGolemLaser.variant, this.getVariant());
        proj.initCustom(posX, posY + 1.6, posZ, getLookVec().x * 0.5, getLookVec().y * 0.5, getLookVec().z * 0.5, 4.0f, this.getUniqueID());
        getEntityWorld().spawnEntity(proj);
      }
    }
  }

  @Override
  public void readEntityFromNBT(NBTTagCompound compound) {
    super.readEntityFromNBT(compound);
    getDataManager().set(variant, compound.getInteger("variant"));
  }

  @Override
  public void writeEntityToNBT(NBTTagCompound compound) {
    super.writeEntityToNBT(compound);
    compound.setInteger("variant", getDataManager().get(variant));
  }

  @Override
  public ResourceLocation getLootTable() {
    return new ResourceLocation(Const.MODID, "entity/golem_" + getVariantEnum().nameLower());
  }

  @Override
  protected SoundEvent getAmbientSound() {
    return ambientSound;
  }

  @Override
  protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
    return hurtSound;
  }

  @Override
  protected SoundEvent getDeathSound() {
    return deathSound;
  }
}
