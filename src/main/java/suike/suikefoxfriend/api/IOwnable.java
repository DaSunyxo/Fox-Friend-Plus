package suike.suikefoxfriend.api;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.player.Player;

public interface IOwnable {
    AttributeSupplier.Builder createFoxAttributes();

    boolean canAttackWithOwner(LivingEntity target, LivingEntity owner);

    void playerTamedFox(Player player);

    boolean isTamed();

    boolean isWaiting();

    void playerSetWaiting(Player player);

    void setWaiting(boolean waiting);

    boolean getIsSleeping();

    LivingEntity getOwner();

    void mixinStopActions();

    void mixinSetSleeping(boolean sleeping);

    void mixinSetAggressive(boolean aggressive);
}