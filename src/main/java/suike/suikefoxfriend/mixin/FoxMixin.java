package suike.suikefoxfriend.mixin;

import java.util.*;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.serialization.Codec;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.attribute.modifier.*;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import suike.suikefoxfriend.SuiKe;
import suike.suikefoxfriend.api.*;
import suike.suikefoxfriend.entity.ai.FoxAttackWithOwnerGoal;
import suike.suikefoxfriend.entity.ai.FoxWaitingGoal;
import suike.suikefoxfriend.entity.ai.FoxFollowOwnerGoal;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Mob;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.server.level.ServerPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import org.slf4j.Logger;

@Mixin(Fox.class)
public abstract class FoxMixin implements IOwnable {//, Tameable {

    private int foxCooldownTicks = 0;
    private int foxNameWaitingTicks = 0;
    private static Random random = new Random();
    private FoxFollowOwnerGoal foxFollowOwnerGoal;
    private boolean isSleeping = false;
    private int sleepingTime = 0;
    public boolean getIsSleeping() {return this.isSleeping;}

    @Shadow abstract void clearStates();
    @Shadow abstract void setSleeping(boolean sleeping);
    @Shadow abstract void setDefending(boolean aggressive);

    @Shadow
    @Final
    private static Codec<List<EntityReference<LivingEntity>>> TRUSTED_LIST_CODEC;

    @Shadow
    @Final
    private static int MIN_TICKS_BEFORE_EAT;

    @Shadow
    @Final
    private static int FLAG_SITTING;

    @Shadow
    private int ticksSinceEaten;

    @Shadow
    @Final
    private static EntityDataAccessor<Optional<EntityReference<LivingEntity>>> DATA_TRUSTED_ID_1;

    public void mixinStopActions() {this.clearStates();}
    public void mixinSetSleeping(boolean sleeping) {this.setSleeping(sleeping);}
    public void mixinSetAggressive(boolean aggressive) {this.setDefending(aggressive);}


    private LinkedList<ChunkPos> foxForcedChunks = new LinkedList<>();
    private boolean hasAttackWithOwnerGoal = ((MobAccessor) (Object) this).getGoalSelector().getAvailableGoals().contains(new FoxAttackWithOwnerGoal((Fox) (Object) this));
    private boolean hasTamedMaxHealth = ((Fox) (Object) this).getAttributes().hasModifier(Attributes.MAX_HEALTH, Identifier.fromNamespaceAndPath(SuiKe.MOD_ID, "tamed_max_health"));
    private boolean hasTamedAttackDamage = ((Fox) (Object) this).getAttributes().hasModifier(Attributes.ATTACK_DAMAGE, Identifier.fromNamespaceAndPath(SuiKe.MOD_ID, "tamed_mattack_damage"));
    private static final EntityDataAccessor<Byte> TAMEABLE_FLAGS = SynchedEntityData.defineId(Fox.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<String> OWNER_UUID = SynchedEntityData.defineId(Fox.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> WAITING_FLAG = SynchedEntityData.defineId(Fox.class, EntityDataSerializers.BOOLEAN);

    public void playerSetWaiting(Player player) { // 玩家修改等待状态
        if (this.foxCooldownTicks <= 0) {
            if (player.getUUID().equals(this.getOwnerUuid())) {
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
        if (target instanceof Creeper || target instanceof Ghast || target instanceof ArmorStand) return false;
        else if (target instanceof TamableAnimal) return !((TamableAnimal) target).isTame();
        else if (target instanceof Fox) return !((IOwnable) target).isTamed();else if (target instanceof Player playerEntity && owner instanceof Player playerEntity2 && !playerEntity2.canHarmPlayer(playerEntity)) return false;
        else {
            return target instanceof AbstractHorse abstractHorseEntity && abstractHorseEntity.isTamed()
                    ? false
                    : !(target instanceof TamableAnimal tameableEntity && tameableEntity.isTame());
        }
    }

    public void playerTamedFox(Player player) {
        if (player != null) {
            Fox foxEntity = (Fox) (Object) this;
            this.setOwner(player);
            AttributeInstance maxHealth = foxEntity.getAttribute(Attributes.MAX_HEALTH);
            maxHealth.addPermanentModifier(new AttributeModifier((Identifier.fromNamespaceAndPath(SuiKe.MOD_ID, "tamed_max_health")), 30.0, AttributeModifier.Operation.ADD_VALUE));
            foxEntity.setHealth(40f);
            this.hasTamedMaxHealth = true;
            AttributeInstance armor = foxEntity.getAttribute(Attributes.ARMOR);
            armor.addPermanentModifier(new AttributeModifier(Identifier.fromNamespaceAndPath(SuiKe.MOD_ID, "tamed_armour"), 10.0, AttributeModifier.Operation.ADD_VALUE));
            AttributeInstance attackDamage = foxEntity.getAttribute(Attributes.ATTACK_DAMAGE);
            attackDamage.addPermanentModifier(new AttributeModifier(Identifier.fromNamespaceAndPath(SuiKe.MOD_ID, "tamed_attack_damage"), 2.0, AttributeModifier.Operation.ADD_VALUE));
            this.hasTamedAttackDamage = true;
            this.setWaiting(true);
            this.mixinStopActions();
            foxEntity.setPersistenceRequired();
            this.foxFollowOwnerGoal = new FoxFollowOwnerGoal(foxEntity);
            ((MobAccessor) this).getGoalSelector().addGoal(4, this.foxFollowOwnerGoal);
            ((MobAccessor) this).getGoalSelector().addGoal(-1, new FoxWaitingGoal(foxEntity));
            ((MobAccessor) this).getGoalSelector().addGoal(3, new FoxAttackWithOwnerGoal(foxEntity));
            this.hasAttackWithOwnerGoal = true;
        }
    }

    @Inject(method = "defineSynchedData", at = @At("RETURN"))
    private void onInitDataTracker(SynchedEntityData.Builder builder, CallbackInfo ci) {
        builder.define(OWNER_UUID, "");
        builder.define(TAMEABLE_FLAGS, (byte) 0);
        builder.define(WAITING_FLAG, this.isWaiting());
    }

//驯服部分
    public void setOwner(Player player) { // 设置主人
        this.setTamed(true);
        ((Fox) (Object) this).getEntityData().set(DATA_TRUSTED_ID_1, Optional.of(EntityReference.of((LivingEntity) player)));
        this.setOwnerUuid(player.getUUID());
        this.tameAnimalCriteria(player);
    }

    public void setOwnerUuid(UUID uuid) { // 设置主人UUID
        String uuidString = (uuid != null) ? uuid.toString() : "";
        ((Fox) (Object) this).getEntityData().set(OWNER_UUID, uuidString);
    }

    public void setTamed(boolean tamed) { // 设置驯服
        byte b = ((Fox) (Object) this).getEntityData().get(TAMEABLE_FLAGS);
        if (tamed) {
            ((Fox) (Object) this).getEntityData().set(TAMEABLE_FLAGS, (byte) (b | 4));
        } else {
            ((Fox) (Object) this).getEntityData().set(TAMEABLE_FLAGS, (byte) (b & -5));
        }
    }

    public boolean isTamed() { // 是否驯服
        Fox foxEntity = (Fox) (Object) this;
        return foxEntity.getEntityData() != null && (foxEntity.getEntityData().get(TAMEABLE_FLAGS) & 4) != 0;
    }

    public LivingEntity getOwner() { // 获取主人
        UUID ownerUuid = this.getOwnerUuid();
        if (ownerUuid == null) {
            return null;
        }
        LivingEntity owner = ((Fox) (Object) this).level().getPlayerInAnyDimension(ownerUuid);
        return (owner instanceof Player) ? owner : null;
    }

    public UUID getOwnerUuid() { // 获取主人UUID
        String uuidString = ((Fox) (Object) this).getEntityData().get(OWNER_UUID);
        if (uuidString != null && !uuidString.isEmpty()) {
            try {
                return UUID.fromString(uuidString);
            } catch (IllegalArgumentException e) {}
        }
        return null;
    }

    public void tameAnimalCriteria(Player player) { // 成就
        if (player instanceof ServerPlayer) {
            CriteriaTriggers.TAME_ANIMAL.trigger((ServerPlayer) player, (Fox) (Object) this);
        }
    }

//等待部分
    public void setWaiting(boolean waiting) {
        this.setWaiting(waiting, "notIsPlayer");
    }
    public void setWaiting(boolean waiting, String isPlayerSetWaiting) { // 设置等待状态
        if (this.isTamed()) {
            Fox foxEntity = (Fox) (Object) this;

            foxEntity.setInvulnerable(waiting); // 设置无敌状态
            foxEntity.getEntityData().set(WAITING_FLAG, waiting);

            if (isPlayerSetWaiting.equals("isPlayer")) {
                this.setFoxName(foxEntity, waiting);
            } else {
                this.setFoxName(foxEntity, false); // 清除等待字段
            }
        }
    }

    public boolean isWaiting() { // 获取等待状态
        return this.isTamed() && ((Fox) (Object) this).getEntityData().get(WAITING_FLAG);
    }

    public void setFoxName(Fox foxEntity, boolean waiting) {
        String foxName = foxEntity.getName().getString(); // 获取当前名称
        String waitingString = Component.translatable("foxfriend.waiting").getString(); // 获取等待字段

        if (waiting) {
            if (!foxName.contains(waitingString)) { // 如果名称中不包含等待字段
                foxEntity.setCustomName(Component.literal(foxName + waitingString)); // 添加等待字段
            }
            this.foxNameWaitingTicks = 50; // 显示等待状态
        } else {
            String defaultFoxName = Component.translatable("entity.minecraft.fox").getString(); // 获取狐狸默认名称

            String oldFoxName = foxName.replace(waitingString, ""); // 去除等待字段
            foxEntity.setCustomName(Component.literal(oldFoxName)); // 恢复原始名称

            if (oldFoxName.equals(defaultFoxName)) { // 如果玩家未使用命名牌
                foxEntity.setCustomName(null); // 清除名字
            }
            this.foxNameWaitingTicks = 0;
        }
    }

    public boolean isChunkForced(Level world, ChunkPos chunkPos) {
        ChunkSource chunkManager = world.getChunkSource();
        LongSet forcedChunks = chunkManager.getForceLoadedChunks();
        return forcedChunks.contains(chunkPos.pack());
    }

//tick方法
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {

        if (!this.isTamed()) {
            return;
        }

        Fox foxEntity = (Fox) (Object) this;
        IOwnable foxIOwnable = (IOwnable) foxEntity;

        if (((IOwnable) foxEntity).isTamed() && !((MobAccessor) this).getGoalSelector().getAvailableGoals().contains(new FoxAttackWithOwnerGoal(foxEntity))) ((MobAccessor) this).getGoalSelector().addGoal(3, new FoxAttackWithOwnerGoal(foxEntity));
        try {
            if (foxIOwnable.isTamed() && !(foxEntity.getAttributes().getModifierValue(Attributes.ATTACK_DAMAGE, Identifier.fromNamespaceAndPath(SuiKe.MOD_ID, "tamed_attack_damage")) == 2.0)) {
                AttributeInstance attackDamage = foxEntity.getAttribute(Attributes.ATTACK_DAMAGE);
                attackDamage.addPermanentModifier(new AttributeModifier(Identifier.fromNamespaceAndPath(SuiKe.MOD_ID, "tamed_attack_damage"), 2.0, AttributeModifier.Operation.ADD_VALUE));
            }} catch (IllegalArgumentException e) {
            if (foxIOwnable.isTamed()) {
                AttributeInstance attackDamage = foxEntity.getAttribute(Attributes.ATTACK_DAMAGE);
                attackDamage.addPermanentModifier(new AttributeModifier(Identifier.fromNamespaceAndPath(SuiKe.MOD_ID, "tamed_attack_damage"), 2.0, AttributeModifier.Operation.ADD_VALUE));
            }
        }
        try {
            if (foxIOwnable.isTamed() && !(foxEntity.getAttributes().getModifierValue(Attributes.MAX_HEALTH, Identifier.fromNamespaceAndPath(SuiKe.MOD_ID, "tamed_max_health")) == 30.0)) {
                AttributeInstance maxHealth = foxEntity.getAttribute(Attributes.MAX_HEALTH);
                maxHealth.addPermanentModifier(new AttributeModifier((Identifier.fromNamespaceAndPath(SuiKe.MOD_ID, "tamed_max_health")), 30.0, AttributeModifier.Operation.ADD_VALUE));
                foxEntity.setHealth(40f);
            }} catch (IllegalArgumentException e) {
            if (foxIOwnable.isTamed()) {
                AttributeInstance maxHealth = foxEntity.getAttribute(Attributes.MAX_HEALTH);
                maxHealth.addPermanentModifier(new AttributeModifier((Identifier.fromNamespaceAndPath(SuiKe.MOD_ID, "tamed_max_health")), 30.0, AttributeModifier.Operation.ADD_VALUE));
                foxEntity.setHealth(40f);
            }
        }

        ChunkPos foxChunkPos = ChunkPos.containing(foxEntity.getOnPos());
        Level foxWorld = foxEntity.level();
        ChunkSource foxChunkManager = foxWorld.getChunkSource();
        //Check if the chunk the fox is in is already forced. If it's not, mark it as such and
        //add it to a list of chunks forced by the fox. Then, every tick, check if the fox left the chunk it
        //forced, and if it did, "unforce" that chunk and remove it from the list of fox forced chunks.
        if (this.foxForcedChunks == null) this.foxForcedChunks = new LinkedList<>(); //Fixes broken foxes crashing the game (woopsies :3)
        if (!isChunkForced(foxWorld, foxChunkPos)) {
            foxChunkManager.updateChunkForced(foxChunkPos, true);
            this.foxForcedChunks.add(foxChunkPos);
        }
        try {
        if (!this.foxForcedChunks.isEmpty()) {
            for (ChunkPos queriedChunkPos : this.foxForcedChunks) {
                if (!queriedChunkPos.equals(foxChunkPos)) {
                    this.foxForcedChunks.remove(queriedChunkPos);
                    foxChunkManager.updateChunkForced(queriedChunkPos, false);
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
                String waitingString = Component.translatable("foxfriend.waiting").getString(); // 获取等待字段
                String defaultFoxName = Component.translatable("entity.minecraft.fox").getString(); // 获取狐狸默认名称

                foxName = foxName.replace(waitingString, ""); // 去除等待字段
                foxEntity.setCustomName(Component.literal(foxName)); // 恢复原始名称

                if (foxName.equals(defaultFoxName)) { // 如果玩家未使用命名牌
                    foxEntity.setCustomName(null); // 清除名字
                }
            }
        }

        Level world = foxEntity.level();
        BlockPos pos = foxEntity.getOnPos();
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
    @Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
    private void onWriteCustomData(ValueOutput view, CallbackInfo ci) {
        if (this.isTamed()) {
            view.putBoolean("Tamed", this.isTamed());
            view.putBoolean("Waiting", this.isWaiting());
            view.putString("Owner", this.getOwnerUuid().toString()); // 将 UUID 转换为字符串存储
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
    private void onReadCustomData(ValueInput view, CallbackInfo ci) {
        if (view.contains("Owner")) {
            UUID ownerUuid = tryReadOwnerUuid(view);

            if (ownerUuid != null) {
                this.setOwnerUuid(ownerUuid);
                this.setTamed(view.getBooleanOr("Tamed", false));
                this.isSleeping = view.getBooleanOr("Sleeping", false);
                this.setWaiting(view.getBooleanOr("Waiting", false));

                Fox foxEntity = (Fox) (Object) this;
                this.mixinStopActions();
                foxEntity.setPersistenceRequired();
                this.foxFollowOwnerGoal = new FoxFollowOwnerGoal(foxEntity);
                ((MobAccessor) this).getGoalSelector().addGoal(5, this.foxFollowOwnerGoal);
                ((MobAccessor) this).getGoalSelector().addGoal(-1, new FoxWaitingGoal(foxEntity));
            }
        }
    }

    public UUID tryReadOwnerUuid(ValueInput view) {
        Optional<UUID> element = view.read("Owner", UUIDUtil.CODEC);
        if (element.isPresent()) return element.get();
        return null;
    }

//尝试捡起物品
    @Inject(method = "canHoldItem", at = @At("HEAD"), cancellable = true)
    private void onCanPickupItem(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (this.isTamed()) { // 是否驯服
            boolean canPickupItem = false;
            ItemStack handItem = ((Fox) (Object) this).getMainHandItem();

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
        } else if ((FoodProperties) stack.get(DataComponents.FOOD) != null) {
            return true;
        }
        return false;
    }

//禁止等待时停止睡觉
    @Inject(method = "wakeUp", at = @At("HEAD"), cancellable = true)
    private void onStopSleeping(CallbackInfo ci) {
        if (this.isWaiting()) { // 等待状态
            ci.cancel(); // 取消方法执行
        }
    }

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void onTickMovement(CallbackInfo ci) {
        Fox foxEntity = ((Fox) (Object) this);
        if (!foxEntity.level().isClientSide() && foxEntity.isAlive() && foxEntity.isEffectiveAi()) {
            ticksSinceEaten++;
            ItemStack itemStack = foxEntity.getMainHandItem();
            //The following 2 if statements simply replace the private canEat() method
            if (itemStack.has(DataComponents.FOOD) && itemStack.has(DataComponents.CONSUMABLE)) {
                if (foxEntity.getTarget() == null && foxEntity.onGround() && !foxEntity.isSleeping()) {
                    if (ticksSinceEaten > 600) {
                        foxEntity.heal(5f); //Heal 5 HP upon eating food
                    }
                }
            }
            //Regenerate 0.5 HP/s when resting
            if (foxEntity.isSleeping() || foxEntity.isSitting()) {
                sleepingTime++;
                if (sleepingTime % 40 == 0) foxEntity.heal(1f);
            }
            ticksSinceEaten--;
            //Increase damage dealt based on held item (useless because vanilla Minecraft already does this)
//            AttributeInstance damage = foxEntity.getAttribute(EntityAttributes.ATTACK_DAMAGE);
//            damage.clearModifiers();
//            damage.addTemporaryModifier(new AttributeModifier((Identifier.of("held_item_damage")), foxEntity.getEquippedStack(EquipmentSlot.MAINHAND).getDamage(), AttributeModifier.Operation.ADD_VALUE));
        }
    }
}

@Mixin(Mob.class)
interface MobAccessor {
    @Accessor("navigation")
    PathNavigation getNavigation();
    @Accessor("goalSelector")
    GoalSelector getGoalSelector();
}
