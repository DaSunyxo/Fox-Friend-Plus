package suike.suikefoxfriend.api;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.player.PlayerEntity;

public interface IOwnable {
    DefaultAttributeContainer.Builder createFoxAttributes();

    boolean canAttackWithOwner(LivingEntity target, LivingEntity owner);

    void playerTamedFox(PlayerEntity player);

    boolean isTamed();

    boolean isWaiting();

    void playerSetWaiting(PlayerEntity player);

    void setWaiting(boolean waiting);

    boolean getIsSleeping();

    LivingEntity getOwner();

    void mixinStopActions();

    void mixinSetSleeping(boolean sleeping);

    void mixinSetAggressive(boolean aggressive);
}