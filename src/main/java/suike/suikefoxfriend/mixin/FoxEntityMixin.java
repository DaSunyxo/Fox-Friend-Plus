package suike.suikefoxfriend.mixin;

import java.util.*;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.serialization.Codec;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.GhastEntity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import suike.suikefoxfriend.SuiKe;
import suike.suikefoxfriend.api.*;
import suike.suikefoxfriend.entity.ai.FoxAttackWithOwnerGoal;
import suike.suikefoxfriend.entity.ai.FoxWaitingGoal;
import suike.suikefoxfriend.entity.ai.FoxFollowOwnerGoal;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.passive.FoxEntity;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;
import net.minecraft.text.Text;
import net.minecraft.fluid.FluidState;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import org.slf4j.Logger;

@Mixin(FoxEntity.class)
public abstract class FoxEntityMixin implements IOwnable {//, Tameable {

    private int foxCooldownTicks = 0;
    private int foxNameWaitingTicks = 0;
    private static Random random = new Random();
    private FoxFollowOwnerGoal foxFollowOwnerGoal;
    private boolean isSleeping = false;
    private int sleepingTime = 0;
    public boolean getIsSleeping() {return this.isSleeping;}

    @Shadow abstract void stopActions();
    @Shadow abstract void setSleeping(boolean sleeping);
    @Shadow abstract void setAggressive(boolean aggressive);

    @Shadow
    @Final
    private static Codec<List<LazyEntityReference<LivingEntity>>> TRUSTED_ENTITIES_CODEC;

    @Shadow
    @Final
    private static int EATING_DURATION;

    @Shadow
    @Final
    private static int SITTING_FLAG;

    @Shadow
    private int eatingTime;

    @Shadow
    @Final
    private static TrackedData<Optional<LazyEntityReference<LivingEntity>>> OTHER_TRUSTED;

    public void mixinStopActions() {this.stopActions();}
    public void mixinSetSleeping(boolean sleeping) {this.setSleeping(sleeping);}
    public void mixinSetAggressive(boolean aggressive) {this.setAggressive(aggressive);}


    private LinkedList<ChunkPos> foxForcedChunks = new LinkedList<>();
    private static final TrackedData<Byte> TAMEABLE_FLAGS = DataTracker.registerData(FoxEntity.class, TrackedDataHandlerRegistry.BYTE);
    private static final TrackedData<String> OWNER_UUID = DataTracker.registerData(FoxEntity.class, TrackedDataHandlerRegistry.STRING);
    private static final TrackedData<Boolean> WAITING_FLAG = DataTracker.registerData(FoxEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    public void playerSetWaiting(PlayerEntity player) { // 玩家修改等待状态
        if (this.foxCooldownTicks <= 0) {
            if (player.getUuid().equals(this.getOwnerUuid())) {
                if (!this.isWaiting()) {
                    this.isSleeping = random.nextBoolean(); // 随机睡觉或坐下
                }
                this.setWaiting(!this.isWaiting(), "isPlayer"); // 修改等待状态
                this.tameAnimalCriteria(player);
                this.foxCooldownTicks = 3; // 修改冷却(tick)
            }
        }
    }

    public boolean canAttackWithOwner(LivingEntity target, LivingEntity owner) {
        if (target instanceof CreeperEntity || target instanceof GhastEntity || target instanceof ArmorStandEntity) return false;
        else if (target instanceof TameableEntity) return !((TameableEntity) target).isTamed();
        else if (target instanceof FoxEntity) return !((IOwnable) target).isTamed();else if (target instanceof PlayerEntity playerEntity && owner instanceof PlayerEntity playerEntity2 && !playerEntity2.shouldDamagePlayer(playerEntity)) return false;
        else {
            return target instanceof AbstractHorseEntity abstractHorseEntity && abstractHorseEntity.isTame()
                    ? false
                    : !(target instanceof TameableEntity tameableEntity && tameableEntity.isTamed());
        }
    }

    public void playerTamedFox(PlayerEntity player) {
        if (player != null) {
            FoxEntity foxEntity = (FoxEntity) (Object) this;
            this.setOwner(player);
            EntityAttributeInstance maxHealth = foxEntity.getAttributeInstance(EntityAttributes.MAX_HEALTH);
            maxHealth.addPersistentModifier(new EntityAttributeModifier((Identifier.of("tamed_max_health")), 30.0, EntityAttributeModifier.Operation.ADD_VALUE));
            foxEntity.setHealth(40f);
            EntityAttributeInstance armor = foxEntity.getAttributeInstance(EntityAttributes.ARMOR);
            armor.addPersistentModifier(new EntityAttributeModifier(Identifier.of("tamed_armour"), 10.0, EntityAttributeModifier.Operation.ADD_VALUE));
            EntityAttributeInstance attackDamage = foxEntity.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE);
            attackDamage.addPersistentModifier(new EntityAttributeModifier(Identifier.of("tamed_attack_damage"), 2.0, EntityAttributeModifier.Operation.ADD_VALUE));
            this.setWaiting(true);
            this.mixinStopActions();
            foxEntity.setPersistent();
            this.foxFollowOwnerGoal = new FoxFollowOwnerGoal(foxEntity);
            ((MobEntityAccessor) this).getGoalSelector().add(4, this.foxFollowOwnerGoal);
            ((MobEntityAccessor) this).getGoalSelector().add(-1, new FoxWaitingGoal(foxEntity));
            ((MobEntityAccessor) this).getGoalSelector().add(3, new FoxAttackWithOwnerGoal(foxEntity));
        }
    }

    @Inject(method = "initDataTracker", at = @At("RETURN"))
    private void onInitDataTracker(DataTracker.Builder builder, CallbackInfo ci) {
        builder.add(OWNER_UUID, "");
        builder.add(TAMEABLE_FLAGS, (byte) 0);
        builder.add(WAITING_FLAG, this.isWaiting());
    }

//驯服部分
    public void setOwner(PlayerEntity player) { // 设置主人
        this.setTamed(true);
        ((FoxEntity) (Object) this).getDataTracker().set(OTHER_TRUSTED, Optional.of(LazyEntityReference.of((LivingEntity) player)));
        this.setOwnerUuid(player.getUuid());
        this.tameAnimalCriteria(player);
    }

    public void setOwnerUuid(UUID uuid) { // 设置主人UUID
        String uuidString = (uuid != null) ? uuid.toString() : "";
        ((FoxEntity) (Object) this).getDataTracker().set(OWNER_UUID, uuidString);
    }

    public void setTamed(boolean tamed) { // 设置驯服
        byte b = ((FoxEntity) (Object) this).getDataTracker().get(TAMEABLE_FLAGS);
        if (tamed) {
            ((FoxEntity) (Object) this).getDataTracker().set(TAMEABLE_FLAGS, (byte) (b | 4));
        } else {
            ((FoxEntity) (Object) this).getDataTracker().set(TAMEABLE_FLAGS, (byte) (b & -5));
        }
    }

    public boolean isTamed() { // 是否驯服
        FoxEntity foxEntity = (FoxEntity) (Object) this;
        return foxEntity.getDataTracker() != null && (foxEntity.getDataTracker().get(TAMEABLE_FLAGS) & 4) != 0;
    }

    public LivingEntity getOwner() { // 获取主人
        UUID ownerUuid = this.getOwnerUuid();
        if (ownerUuid == null) {
            return null;
        }
        LivingEntity owner = ((EntityAccessor) this).getWorld().getPlayerByUuid(ownerUuid);
        return (owner instanceof PlayerEntity) ? owner : null;
    }

    public UUID getOwnerUuid() { // 获取主人UUID
        String uuidString = ((FoxEntity) (Object) this).getDataTracker().get(OWNER_UUID);
        if (uuidString != null && !uuidString.isEmpty()) {
            try {
                return UUID.fromString(uuidString);
            } catch (IllegalArgumentException e) {}
        }
        return null;
    }

    public void tameAnimalCriteria(PlayerEntity player) { // 成就
        if (player instanceof ServerPlayerEntity) {
            Criteria.TAME_ANIMAL.trigger((ServerPlayerEntity) player, (FoxEntity) (Object) this);
        }
    }

//等待部分
    public void setWaiting(boolean waiting) {
        this.setWaiting(waiting, "notIsPlayer");
    }
    public void setWaiting(boolean waiting, String isPlayerSetWaiting) { // 设置等待状态
        if (this.isTamed()) {
            FoxEntity foxEntity = (FoxEntity) (Object) this;

            foxEntity.setInvulnerable(waiting); // 设置无敌状态
            foxEntity.getDataTracker().set(WAITING_FLAG, waiting);

            if (isPlayerSetWaiting.equals("isPlayer")) {
                this.setFoxName(foxEntity, waiting);
            } else {
                this.setFoxName(foxEntity, false); // 清除等待字段
            }
        }
    }

    public boolean isWaiting() { // 获取等待状态
        return this.isTamed() && ((FoxEntity) (Object) this).getDataTracker().get(WAITING_FLAG);
    }

    public void setFoxName(FoxEntity foxEntity, boolean waiting) {
        String foxName = foxEntity.getName().getString(); // 获取当前名称
        String waitingString = Text.translatable("foxfriend.waiting").getString(); // 获取等待字段

        if (waiting) {
            if (!foxName.contains(waitingString)) { // 如果名称中不包含等待字段
                foxEntity.setCustomName(Text.literal(foxName + waitingString)); // 添加等待字段
            }
            this.foxNameWaitingTicks = 50; // 显示等待状态
        } else {
            String defaultFoxName = Text.translatable("entity.minecraft.fox").getString(); // 获取狐狸默认名称

            String oldFoxName = foxName.replace(waitingString, ""); // 去除等待字段
            foxEntity.setCustomName(Text.literal(oldFoxName)); // 恢复原始名称

            if (oldFoxName.equals(defaultFoxName)) { // 如果玩家未使用命名牌
                foxEntity.setCustomName(null); // 清除名字
            }
            this.foxNameWaitingTicks = 0;
        }
    }

    public boolean isChunkForced(World world, ChunkPos chunkPos) {
        ChunkManager chunkManager = world.getChunkManager();
        LongSet forcedChunks = chunkManager.getForcedChunks();
        return forcedChunks.contains(chunkPos.toLong());
    }

//tick方法
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {

        if (!this.isTamed()) {
            return;
        }

        FoxEntity foxEntity = (FoxEntity) (Object) this;

        if (((IOwnable) foxEntity).isTamed() && ((MobEntityAccessor) this).getGoalSelector().getGoals().contains(new FoxAttackWithOwnerGoal(foxEntity))) ((MobEntityAccessor) this).getGoalSelector().add(3, new FoxAttackWithOwnerGoal(foxEntity));
        if (((IOwnable) foxEntity).isTamed()
                && (foxEntity.getAttributeInstance(EntityAttributes.MAX_HEALTH).getValue() != 40
                || foxEntity.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE).getValue() != 4.0)) {
            EntityAttributeInstance attackDamage = foxEntity.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE);
            attackDamage.addPersistentModifier(new EntityAttributeModifier(Identifier.of("tamed_attack_damage"), 2.0, EntityAttributeModifier.Operation.ADD_VALUE));
            EntityAttributeInstance maxHealth = foxEntity.getAttributeInstance(EntityAttributes.MAX_HEALTH);
            maxHealth.addPersistentModifier(new EntityAttributeModifier((Identifier.of("tamed_max_health")), 30.0, EntityAttributeModifier.Operation.ADD_VALUE));
            foxEntity.setHealth(40f);
        }

        ChunkPos foxChunkPos = foxEntity.getChunkPos();
        World foxWorld = foxEntity.getEntityWorld();
        ChunkManager foxChunkManager = foxWorld.getChunkManager();
        //Check if the chunk the fox is in is already forced. If it's not, mark it as such and
        //add it to a list of chunks forced by the fox. Then, every tick, check if the fox left the chunk it
        //forced, and if it did, "unforce" that chunk and remove it from the list of fox forced chunks.
        if (this.foxForcedChunks == null) this.foxForcedChunks = new LinkedList<>(); //Fixes broken foxes crashing the game (woopsies :3)
        if (!isChunkForced(foxWorld, foxChunkPos)) {
            foxChunkManager.setChunkForced(foxChunkPos, true);
            this.foxForcedChunks.add(foxChunkPos);
        }
        try {
        if (!this.foxForcedChunks.isEmpty()) {
            for (ChunkPos queriedChunkPos : this.foxForcedChunks) {
                if (!queriedChunkPos.equals(foxChunkPos)) {
                    this.foxForcedChunks.remove(queriedChunkPos);
                    foxChunkManager.setChunkForced(queriedChunkPos, false);
                }
            }
        }
        } catch(ConcurrentModificationException ignored) {}

        if (this.foxCooldownTicks > 0) {
            this.foxCooldownTicks--; // 减少点击冷却
        }

        if (this.foxNameWaitingTicks > 0) {
            this.foxNameWaitingTicks--; // 减少显示时间
            if (foxNameWaitingTicks == 0) {
                String foxName = foxEntity.getName().getString(); // 获取当前名称
                String waitingString = Text.translatable("foxfriend.waiting").getString(); // 获取等待字段
                String defaultFoxName = Text.translatable("entity.minecraft.fox").getString(); // 获取狐狸默认名称

                foxName = foxName.replace(waitingString, ""); // 去除等待字段
                foxEntity.setCustomName(Text.literal(foxName)); // 恢复原始名称

                if (foxName.equals(defaultFoxName)) { // 如果玩家未使用命名牌
                    foxEntity.setCustomName(null); // 清除名字
                }
            }
        }

        if (!isWaiting() && this.foxFollowOwnerGoal != null) {
            if (this.foxFollowOwnerGoal.teleport()) { // 尝试传送到主人
                return;
            }
        }

        World world = foxEntity.getEntityWorld();
        BlockPos pos = foxEntity.getBlockPos();
        FluidState fluidState = world.getFluidState(pos);
        // 检查是否在液体里
        if (fluidState != null && !fluidState.isEmpty()) {
            this.setWaiting(false);
        }
    }

//存储读取
//The three methods below were changed to allow compatibility with 1.21.11 - compatibility with versions 1.21.6 - 1.21.10
//might be possible, but not guaranteed.
// - Suny
    @Inject(method = "writeCustomData", at = @At("RETURN"))
    private void onWriteCustomData(WriteView view, CallbackInfo ci) {
        if (this.isTamed()) {
            view.putBoolean("Tamed", this.isTamed());
            view.putBoolean("Waiting", this.isWaiting());
            view.putString("Owner", this.getOwnerUuid().toString()); // 将 UUID 转换为字符串存储
        }
    }

    @Inject(method = "readCustomData", at = @At("RETURN"))
    private void onReadCustomData(ReadView view, CallbackInfo ci) {
        if (view.contains("Owner")) {
            UUID ownerUuid = tryReadOwnerUuid(view);

            if (ownerUuid != null) {
                this.setOwnerUuid(ownerUuid);
                this.setTamed(view.getBoolean("Tamed", false));
                this.isSleeping = view.getBoolean("Sleeping", false);
                this.setWaiting(view.getBoolean("Waiting", false));

                FoxEntity foxEntity = (FoxEntity) (Object) this;
                this.mixinStopActions();
                foxEntity.setPersistent();
                this.foxFollowOwnerGoal = new FoxFollowOwnerGoal(foxEntity);
                ((MobEntityAccessor) this).getGoalSelector().add(5, this.foxFollowOwnerGoal);
                ((MobEntityAccessor) this).getGoalSelector().add(-1, new FoxWaitingGoal(foxEntity));
            }
        }
    }

    public UUID tryReadOwnerUuid(ReadView view) {
        Optional<UUID> element = view.read("Owner", Uuids.CODEC);
        if (element.isPresent()) return element.get();
        return null;
    }

//尝试捡起物品
    @Inject(method = "canPickupItem", at = @At("HEAD"), cancellable = true)
    private void onCanPickupItem(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (this.isTamed()) { // 是否驯服
            boolean canPickupItem = false;
            ItemStack handItem = ((FoxEntity) (Object) this).getEquippedStack(EquipmentSlot.MAINHAND);

            if (handItem.isEmpty() || !this.canPickup(handItem)) {
                canPickupItem = this.canPickup(stack);
            }

            cir.setReturnValue(canPickupItem); // 设置返回值
        }
    }

    public boolean canPickup(ItemStack stack) {
        Item dropItem = stack.getItem();
        if (SuiKe.foxCanPickupItemList.contains(dropItem)) {
            return true;
        } else if (SuiKe.foxNotPickupItemList.contains(dropItem)) {
            return false;
        } else if ((FoodComponent) stack.get(DataComponentTypes.FOOD) != null) {
            return true;
        }
        return false;
    }

//禁止等待时停止睡觉
    @Inject(method = "stopSleeping", at = @At("HEAD"), cancellable = true)
    private void onStopSleeping(CallbackInfo ci) {
        if (this.isWaiting()) { // 等待状态
            ci.cancel(); // 取消方法执行
        }
    }

    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void onTickMovement(CallbackInfo ci) {
        FoxEntity foxEntity = ((FoxEntity) (Object) this);
        if (!foxEntity.getEntityWorld().isClient() && foxEntity.isAlive() && foxEntity.canActVoluntarily()) {
            eatingTime++;
            ItemStack itemStack = foxEntity.getEquippedStack(EquipmentSlot.MAINHAND);
            //The following 2 if statements simply replace the private canEat() method
            if (itemStack.contains(DataComponentTypes.FOOD) && itemStack.contains(DataComponentTypes.CONSUMABLE)) {
                if (foxEntity.getTarget() == null && foxEntity.isOnGround() && !foxEntity.isSleeping()) {
                    if (eatingTime > 600) {
                        foxEntity.heal(5f); //Heal 5 HP upon eating food
                    }
                }
            }
            //Regenerate 0.5 HP/s when resting
            if (foxEntity.isSleeping() || foxEntity.isSitting()) {
                sleepingTime++;
                if (sleepingTime % 40 == 0) foxEntity.heal(1f);
            }
            eatingTime--;
            //Increase damage dealt based on held item
//            EntityAttributeInstance damage = foxEntity.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE);
//            damage.clearModifiers();
//            damage.addTemporaryModifier(new EntityAttributeModifier((Identifier.of("held_item_damage")), foxEntity.getEquippedStack(EquipmentSlot.MAINHAND).getDamage(), EntityAttributeModifier.Operation.ADD_VALUE));
        }
    }
}

@Mixin(Entity.class)
interface EntityAccessor {
    @Accessor("world")
    World getWorld();
}

@Mixin(MobEntity.class)
interface MobEntityAccessor {
    @Accessor("navigation")
    EntityNavigation getNavigation();
    @Accessor("goalSelector")
    GoalSelector getGoalSelector();
}
