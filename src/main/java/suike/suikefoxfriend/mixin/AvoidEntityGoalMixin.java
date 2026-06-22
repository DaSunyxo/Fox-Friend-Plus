package suike.suikefoxfriend.mixin;

import java.util.function.Predicate;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.entity.animal.polarbear.PolarBear;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvoidEntityGoal.class)
public abstract class AvoidEntityGoalMixin<T extends LivingEntity> {
    @Shadow
    @Mutable
    private Class<T> avoidClass;

    // TODO(Ravel): no target class
    @Inject(method = "<init>(Lnet/minecraft/world/entity/PathfinderMob;Ljava/lang/Class;FDDLjava/util/function/Predicate;)V",
        at = @At("TAIL"))
    private void onInit(PathfinderMob fleeingEntity, Class<T> classToFleeFrom, float fleeDistance, double fleeSlowSpeed, double fleeFastSpeed, Predicate<LivingEntity> inclusionSelector, CallbackInfo ci) {
        if (fleeingEntity instanceof Fox && Player.class.equals(classToFleeFrom)) {
            this.avoidClass = (Class<T>) PolarBear.class;
        }
    }
}