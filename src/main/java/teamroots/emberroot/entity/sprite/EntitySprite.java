package teamroots.emberroot.entity.sprite;

import java.util.List;
import java.util.Random;
import javax.annotation.Nullable;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import teamroots.emberroot.Const;
import teamroots.emberroot.EmberRootZoo;
import teamroots.emberroot.config.ConfigManager;
import teamroots.emberroot.config.ConfigSpawnEntity;
import teamroots.emberroot.util.EntityUtil;
import teamroots.emberroot.util.Util;

public class EntitySprite extends EntityFlying implements ISprite {// implements IRangedAttackMob {

  public static final DataParameter<Float> targetDirectionX = EntityDataManager.<Float> createKey(EntitySprite.class, DataSerializers.FLOAT);
  public static final DataParameter<Float> targetDirectionY = EntityDataManager.<Float> createKey(EntitySprite.class, DataSerializers.FLOAT);
  public static final DataParameter<Integer> dashTimer = EntityDataManager.<Integer> createKey(EntitySprite.class, DataSerializers.VARINT);
  public static final DataParameter<Float> happiness = EntityDataManager.<Float> createKey(EntitySprite.class, DataSerializers.FLOAT);
  public static final DataParameter<Boolean> stunned = EntityDataManager.<Boolean> createKey(EntitySprite.class, DataSerializers.BOOLEAN);
  public static final DataParameter<BlockPos> targetBlock = EntityDataManager.<BlockPos> createKey(EntitySprite.class, DataSerializers.BLOCK_POS);
  public static final DataParameter<BlockPos> lastTargetBlock = EntityDataManager.<BlockPos> createKey(EntitySprite.class, DataSerializers.BLOCK_POS);
  public static final DataParameter<BlockPos> lastLastTargetBlock = EntityDataManager.<BlockPos> createKey(EntitySprite.class, DataSerializers.BLOCK_POS);
  public static final String NAME = "rootsonesprite";
  private static final double RANGE_ATTACK = 16;
  public float range = 64;
  public static ConfigSpawnEntity config = new ConfigSpawnEntity(EntitySprite.class, EnumCreatureType.MONSTER);
  public float addDirectionX = 0;
  public float addDirectionY = 0;
  public float twirlTimer = 0;
  public float prevYaw1 = 0;
  public float prevYaw2 = 0;
  public float prevYaw3 = 0;
  public float prevYaw4 = 0;
  public float prevPitch1 = 0;
  public float prevPitch2 = 0;
  public float prevPitch3 = 0;
  public float prevPitch4 = 0;
  public Vec3d moveVec = new Vec3d(0, 0, 0);
  public Vec3d prevMoveVec = new Vec3d(0, 0, 0);
  Random random = new Random();
  public int offset = random.nextInt(25);
  public static SoundEvent ambientSound = new SoundEvent(new ResourceLocation(Const.MODID, "spiritambient"));
  public static SoundEvent hurtSound = new SoundEvent(new ResourceLocation(Const.MODID, "spirithurt"));
  public static SoundEvent staffcast = new SoundEvent(new ResourceLocation(Const.MODID, "staffcast"));
  public static float speedup = 1;


  public EntitySprite(World worldIn) {
    super(worldIn);
    this.noClip = true;
    setSize(0.75f, 0.75f);
    this.isAirBorne = true;
    this.experienceValue = 10;
    this.rotationYaw = rand.nextInt(240) + 60;
  }

  @Override
  public int getMaxSpawnedInChunk() {
    return config.settings.max;
  }

  @Override
  public boolean getCanSpawnHere() {
    int i = MathHelper.floor(this.posX);
    int j = MathHelper.floor(this.getEntityBoundingBox().minY);
    int k = MathHelper.floor(this.posZ);
    BlockPos blockpos = new BlockPos(i, j, k);
    boolean canSpawn = this.world.getBlockState(blockpos.down()).getBlock() != Blocks.AIR
        && this.world.getLight(blockpos) < ConfigManager.LIGHT_LEVEL
        && super.getCanSpawnHere()
        && this.rand.nextInt(config.settings.weightedProb) == 0;
    return canSpawn;
  }

  @Override
  protected void entityInit() {
    super.entityInit();
    this.getDataManager().register(targetDirectionX, Float.valueOf(0));
    this.getDataManager().register(targetDirectionY, Float.valueOf(0));
    this.getDataManager().register(dashTimer, Integer.valueOf(0));
    this.getDataManager().register(happiness, Float.valueOf(0));
    this.getDataManager().register(stunned, Boolean.valueOf(false));
    this.getDataManager().register(targetBlock, new BlockPos(0, -1, 0));
    this.getDataManager().register(lastTargetBlock, new BlockPos(0, -1, 0));
    this.getDataManager().register(lastLastTargetBlock, new BlockPos(0, -1, 0));
  }

  @Override
  public void collideWithEntity(Entity entity) {
    if (this.getAttackTarget() != null && this.getHealth() > 0 && !getDataManager().get(stunned).booleanValue()) {
      if (entity instanceof EntityLivingBase && entity.getUniqueID().compareTo(this.getAttackTarget().getUniqueID()) == 0) {
        EntityLivingBase living = ((EntityLivingBase) entity);
        if (EntityUtil.isCreativePlayer(living)) {
          return;
        }
        living.attackEntityFrom(DamageSource.GENERIC, config.settings.attack);
        float magnitude = (float) Math.sqrt(motionX * motionX + motionZ * motionZ);
        living.knockBack(this, 3.0f * magnitude + 0.1f, -motionX / magnitude + 0.1, -motionZ / magnitude + 0.1);
        living.attackEntityAsMob(this);
        living.setRevengeTarget(this);
      }
    }
  }

  @Override
  public void updateAITasks() {
    super.updateAITasks();
  }

  @Override
  public void onUpdate() {
    super.onUpdate();
    if (this.world.getDifficulty() == EnumDifficulty.PEACEFUL) {
      this.setDead();
    }
    if (getDataManager().get(happiness) > 0) {
      if (this.ticksExisted % 2 == 0) {
        EmberRootZoo.proxy.spawnParticleMagicSparkleScalableFX(getEntityWorld(), 24, posX + width * 0.5f * (random.nextFloat() - 0.5f), posY + height * 0.5f + height * (random.nextFloat() - 0.5f), posZ + width * 0.5f * (random.nextFloat() - 0.5f), 0, 0, 0, this.getDataManager().get(happiness).floatValue() / 20.0f, 107, 255, 28);
      }
    }
    if (this.getDataManager().get(targetBlock).getY() == -1) {
      this.getDataManager().set(targetBlock, this.getPosition());
      this.getDataManager().setDirty(targetBlock);
    }
    prevYaw4 = prevYaw3;
    prevYaw3 = prevYaw2;
    prevYaw2 = prevYaw1;
    prevYaw1 = rotationYaw;
    prevPitch4 = prevPitch3;
    prevPitch3 = prevPitch2;
    prevPitch2 = prevPitch1;
    prevPitch1 = rotationPitch;
    //    	if (this.ticksExisted % 4000 == 0 && !this.getDataManager().get(stunned)){
    //    		if (random.nextInt(6) == 0 && !this.getEntityWorld().isRemote){
    //    			getEntityWorld().spawnEntityInWorld(new EntityItem(getEntityWorld(),posX,posY,posZ,new ItemStack(RegistryManager.otherworldLeaf,1)));
    //    		}
    //    	}

    if (this.getAttackTarget() != null &&!this.getAttackTarget().isEntityAlive()) {
      this.setAttackTarget(null);
    }

    if (getDataManager().get(stunned).booleanValue()) {
      this.setAttackTarget(null);
    }
    if (!getDataManager().get(stunned).booleanValue()) {
      if (this.ticksExisted % 20 == 0) {
        if (random.nextInt(4) == 0 && this.getDataManager().get(stunned).booleanValue() == false) {
          getEntityWorld().playSound(posX, posY, posZ, ambientSound, SoundCategory.NEUTRAL, random.nextFloat() * 0.1f + 0.95f, random.nextFloat() * 0.1f + 0.95f, false);
        }
      }
      if (twirlTimer > 0) {
        twirlTimer -= 1.0f;
      }
      if (getDataManager().get(dashTimer) > 0) {
        getDataManager().set(dashTimer, getDataManager().get(dashTimer) - 1);
        getDataManager().setDirty(dashTimer);
      }
      if (this.getAttackTarget() != null && !this.getEntityWorld().isRemote) {
        if (getDataManager().get(dashTimer) <= 0) {
          this.getDataManager().set(targetDirectionX, (float) Math.toRadians(Util.yawDegreesBetweenPointsSafe(posX, posY, posZ, getAttackTarget().posX, getAttackTarget().posY + getAttackTarget().getEyeHeight() / 2.0, getAttackTarget().posZ, getDataManager().get(targetDirectionX).doubleValue())));
          this.getDataManager().set(targetDirectionY, (float) Math.toRadians(Util.pitchDegreesBetweenPoints(posX, posY, posZ, getAttackTarget().posX, getAttackTarget().posY + getAttackTarget().getEyeHeight() / 2.0, getAttackTarget().posZ)));
          this.getDataManager().setDirty(targetDirectionX);
          this.getDataManager().setDirty(targetDirectionY);
        }
        if (this.ticksExisted % 20 == 0 && random.nextInt(3) == 0) {
          getDataManager().set(dashTimer, 20);
          getDataManager().setDirty(dashTimer);
          twirlTimer = 20;
        }
      }
      else if (getDataManager().get(targetBlock).getY() != -1) {
        if (this.ticksExisted % 40 == 0 && !this.getEntityWorld().isRemote) {
          Vec3d target = new Vec3d(getDataManager().get(targetBlock).getX() + 0.5 + (random.nextFloat() - 0.5f) * 9.0f, getDataManager().get(targetBlock).getY() + 4.0 + (random.nextFloat() - 0.5f) * 9.0f, getDataManager().get(targetBlock).getZ() + 0.5 + (random.nextFloat() - 0.5f) * 9.0f);
          this.getDataManager().set(targetDirectionX, (float) Math.toRadians(Util.yawDegreesBetweenPointsSafe(posX, posY, posZ, target.x, target.y, target.z, getDataManager().get(targetDirectionX).doubleValue())));
          this.getDataManager().set(targetDirectionY, (float) Math.toRadians(Util.pitchDegreesBetweenPoints(posX, posY, posZ, target.x, target.y, target.z)));
          this.getDataManager().setDirty(targetDirectionX);
          this.getDataManager().setDirty(targetDirectionY);
        }
      }
      else {
        if (this.ticksExisted % 40 == 0 && !this.getEntityWorld().isRemote) {
          this.getDataManager().set(targetDirectionX, (float) Math.toRadians(random.nextFloat() * 360.0f));
          this.getDataManager().set(targetDirectionY, (float) Math.toRadians(random.nextFloat() * 180.0f - 90.0f));
          this.getDataManager().setDirty(targetDirectionX);
          this.getDataManager().setDirty(targetDirectionY);
        }
      }
      if (this.ticksExisted % 5 == 0) {
        prevMoveVec = moveVec;
        moveVec = Util.lookVector(this.getDataManager().get(targetDirectionX), this.getDataManager().get(targetDirectionY)).scale(getAttackTarget() != null ? (getDataManager().get(dashTimer) > 0 ? 0.3 : 0.225) : 0.15);
      }
      float motionInterp = ((float) (this.ticksExisted % 5)) / 5.0f;
      this.motionX = (1.0f - motionInterp) * prevMoveVec.x + (motionInterp) * moveVec.x;
      this.motionY = (1.0f - motionInterp) * prevMoveVec.y + (motionInterp) * moveVec.y;
      this.motionZ = (1.0f - motionInterp) * prevMoveVec.z + (motionInterp) * moveVec.z;
      this.rotationYaw = (float) Math.toRadians(Util.yawDegreesBetweenPointsSafe(0, 0, 0, motionX, motionY, motionZ, rotationYaw));
      this.rotationPitch = (float) Math.toRadians(Util.pitchDegreesBetweenPoints(0, 0, 0, motionX, motionY, motionZ));
      if (getDataManager().get(dashTimer) > 0) {
        EmberRootZoo.proxy.spawnParticleMagicSparkleFX(getEntityWorld(), posX + ((random.nextDouble()) - 0.5) * 0.5, posY + 0.25 + ((random.nextDouble()) - 0.5) * 0.5, posZ + ((random.nextDouble()) - 0.5) * 0.5, -0.25 * moveVec.x, -0.25 * moveVec.y, -0.25 * moveVec.z, 107, 255, 28);
      }
      if (getDataManager().get(happiness) < -25) {
        List<EntityPlayer> playersValid = EntityUtil.getNonCreativePlayers(getEntityWorld(), new AxisAlignedBB(posX - RANGE_ATTACK, posY - RANGE_ATTACK, posZ - RANGE_ATTACK, posX + RANGE_ATTACK, posY + RANGE_ATTACK, posZ + RANGE_ATTACK));
        if (playersValid.size() > 0) {
          this.setAttackTarget(playersValid.get(rand.nextInt(playersValid.size())));
        }
      }
    }
    else {
      if (this.getHealth() > 0.75f * getMaxHealth()) {
        getDataManager().set(stunned, false);
        getDataManager().setDirty(stunned);
      }
      this.rotationPitch *= 0.9;
      this.motionX = 0.9 * motionX;
      this.motionY = -0.05;
      this.motionZ = 0.9 * motionZ;
    }
    if (this.getHappiness() > 0) {
      this.setHappiness(getHappiness() - 0.001f);
    }
  }

  @Override
  public int getBrightnessForRender() {
    if (getDataManager().get(stunned).booleanValue()) {
      return 128;
    }
    float f = 0.5F;
    f = MathHelper.clamp(f, 0.0F, 1.0F);
    int i = super.getBrightnessForRender();
    int j = i & 255;
    int k = i >> 16 & 255;
    j = j + (int) (f * 15.0F * 16.0F);
    if (j > 240) {
      j = 240;
    }
    return j | k << 16;
  }

  @Override
  public boolean attackEntityFrom(DamageSource source, float amount) {
    getEntityWorld().playSound(posX, posY, posZ, hurtSound, SoundCategory.NEUTRAL, random.nextFloat() * 0.1f + 0.95f, random.nextFloat() * 0.1f + 0.95f, false);
    getDataManager().set(happiness, getDataManager().get(happiness) - 5);
    this.getDataManager().setDirty(happiness);
    if (source.getTrueSource() instanceof EntityLivingBase) {
      this.setAttackTarget((EntityLivingBase) source.getTrueSource());
    }
    return super.attackEntityFrom(source, amount);
  }

  @Override
  public boolean attackEntityAsMob(Entity entity) {
    if (entity instanceof EntityLivingBase) {
      this.setAttackTarget((EntityLivingBase) entity);
    }
    return super.attackEntityAsMob(entity);
  }

  @Override
  public void setDead() {
    super.setDead();
    getEntityWorld().playSound(posX, posY, posZ, hurtSound, SoundCategory.NEUTRAL, random.nextFloat() * 0.1f + 0.95f, (random.nextFloat() * 0.1f + 0.95f) / 2.0f, false);
  }

  @Override
  public boolean isAIDisabled() {
    return false;
  }

  @Override
  protected boolean canDespawn() {
    return true;
  }

  @Override
  protected void applyEntityAttributes() {
    super.applyEntityAttributes();
    this.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(0.25D);
    ConfigSpawnEntity.syncInstance(this, config.settings);
  }

  @Override
  public void readEntityFromNBT(NBTTagCompound compound) {
    super.readEntityFromNBT(compound);
    getDataManager().set(targetDirectionX, compound.getFloat("targetDirectionX"));
    getDataManager().set(targetDirectionY, compound.getFloat("targetDirectionY"));
    getDataManager().set(dashTimer, compound.getInteger("dashTimer"));
    getDataManager().set(happiness, compound.getFloat("happiness"));
    getDataManager().set(stunned, compound.getBoolean("stunned"));
    getDataManager().set(targetBlock, new BlockPos(compound.getInteger("targetBlockX"), compound.getInteger("targetBlockY"), compound.getInteger("targetBlockZ")));
    getDataManager().set(lastTargetBlock, new BlockPos(compound.getInteger("lastTargetBlockX"), compound.getInteger("lastTargetBlockY"), compound.getInteger("lastTargetBlockZ")));
    getDataManager().set(lastLastTargetBlock, new BlockPos(compound.getInteger("lastLastTargetBlockX"), compound.getInteger("lastLastTargetBlockY"), compound.getInteger("lastLastTargetBlockZ")));
    getDataManager().setDirty(targetDirectionX);
    getDataManager().setDirty(targetDirectionY);
    getDataManager().setDirty(dashTimer);
    getDataManager().setDirty(happiness);
    getDataManager().setDirty(stunned);
    getDataManager().setDirty(targetBlock);
    getDataManager().setDirty(lastTargetBlock);
    getDataManager().setDirty(lastLastTargetBlock);
  }

  @Override
  public void writeEntityToNBT(NBTTagCompound compound) {
    super.writeEntityToNBT(compound);
    compound.setFloat("targetDirectionX", getDataManager().get(targetDirectionX));
    compound.setFloat("targetDirectionY", getDataManager().get(targetDirectionY));
    compound.setInteger("dashTimer", getDataManager().get(dashTimer));
    compound.setFloat("happiness", getDataManager().get(happiness));
    compound.setBoolean("stunned", getDataManager().get(stunned));
    compound.setInteger("targetBlockX", getDataManager().get(targetBlock).getX());
    compound.setInteger("targetBlockY", getDataManager().get(targetBlock).getY());
    compound.setInteger("targetBlockZ", getDataManager().get(targetBlock).getZ());
    compound.setInteger("lastTargetBlockX", getDataManager().get(lastTargetBlock).getX());
    compound.setInteger("lastTargetBlockY", getDataManager().get(lastTargetBlock).getY());
    compound.setInteger("lastTargetBlockZ", getDataManager().get(lastTargetBlock).getZ());
    compound.setInteger("lastLastTargetBlockX", getDataManager().get(lastLastTargetBlock).getX());
    compound.setInteger("lastLastTargetBlockY", getDataManager().get(lastLastTargetBlock).getY());
    compound.setInteger("lastLastTargetBlockZ", getDataManager().get(lastLastTargetBlock).getZ());
  }

  @Override
  public float getHappiness() {
    return getDataManager().get(happiness).floatValue();
  }

  @Override
  public void setHappiness(float value) {
    getDataManager().set(happiness, value);
    getDataManager().setDirty(happiness);
  }

  @Override
  public void setTargetPosition(BlockPos pos) {
    if (!pos.equals(getDataManager().get(lastTargetBlock)) && !pos.equals(getDataManager().get(targetBlock))) {
      getDataManager().set(lastLastTargetBlock, getDataManager().get(lastTargetBlock));
      getDataManager().setDirty(lastLastTargetBlock);
      getDataManager().set(lastTargetBlock, getDataManager().get(targetBlock));
      getDataManager().setDirty(lastTargetBlock);
      getDataManager().set(targetBlock, pos);
      getDataManager().setDirty(targetBlock);
    }
  }

  @Override
  public BlockPos getTargetPosition() {
    return getDataManager().get(targetBlock);
  }

  @Nullable
  protected ResourceLocation getLootTable() {
    return new ResourceLocation(Const.MODID, "entity/sprite_normal");
  }

  @Override
  public void travel(float strafe, float vertical, float forward)
  {
    float modifier = speedup;

    if (this.isInWater())
    {
      this.moveRelative(strafe, vertical, forward, 0.02F);
      this.move(MoverType.SELF, this.motionX * modifier, this.motionY * modifier, this.motionZ * modifier);
      this.motionX *= 0.800000011920929D;
      this.motionY *= 0.800000011920929D;
      this.motionZ *= 0.800000011920929D;
    }
    else if (this.isInLava())
    {
      this.moveRelative(strafe, vertical, forward, 0.02F);
      this.move(MoverType.SELF, this.motionX * modifier, this.motionY * modifier, this.motionZ * modifier);
      this.motionX *= 0.5D;
      this.motionY *= 0.5D;
      this.motionZ *= 0.5D;
    }
    else
    {
      float f = 0.91F;

      if (this.onGround)
      {
        BlockPos underPos = new BlockPos(MathHelper.floor(this.posX), MathHelper.floor(this.getEntityBoundingBox().minY) - 1, MathHelper.floor(this.posZ));
        IBlockState underState = this.world.getBlockState(underPos);
        f = underState.getBlock().getSlipperiness(underState, this.world, underPos, this) * 0.91F;
      }

      float f1 = 0.16277136F / (f * f * f);
      this.moveRelative(strafe, vertical, forward, this.onGround ? 0.1F * f1 : 0.02F);
      f = 0.91F;

      if (this.onGround)
      {
        BlockPos underPos = new BlockPos(MathHelper.floor(this.posX), MathHelper.floor(this.getEntityBoundingBox().minY) - 1, MathHelper.floor(this.posZ));
        IBlockState underState = this.world.getBlockState(underPos);
        f = underState.getBlock().getSlipperiness(underState, this.world, underPos, this) * 0.91F;
      }

      this.move(MoverType.SELF, this.motionX * modifier, this.motionY * modifier, this.motionZ * modifier);
      this.motionX *= (double)f;
      this.motionY *= (double)f;
      this.motionZ *= (double)f;
    }

    this.prevLimbSwingAmount = this.limbSwingAmount;
    double d1 = this.posX - this.prevPosX;
    double d0 = this.posZ - this.prevPosZ;
    float f2 = MathHelper.sqrt(d1 * d1 + d0 * d0) * 4.0F;

    if (f2 > 1.0F)
    {
      f2 = 1.0F;
    }

    this.limbSwingAmount += (f2 - this.limbSwingAmount) * 0.4F;
    this.limbSwing += this.limbSwingAmount;
  }


}