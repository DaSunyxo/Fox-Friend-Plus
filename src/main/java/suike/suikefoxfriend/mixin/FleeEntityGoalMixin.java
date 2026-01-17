package suike.suikefoxfriend.mixin;

import java.util.function.Predicate;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.passive.FoxEntity;
import net.minecraft.entity.passive.PolarBearEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.ai.goal.FleeEntityGoal;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FleeEntityGoal.class)
public abstract class FleeEntityGoalMixin<T extends LivingEntity> {
    @Shadow
    @Mutable
    private Class<T> classToFleeFrom;

    @Inject(
        method = "<init>(Lnet/minecraft/entity/mob/PathAwareEntity;Ljava/lang/Class;FDDLjava/util/function/Predicate;)V",
        at = @At("TAIL")
    )
    private void onInit(PathAwareEntity fleeingEntity, Class<T> classToFleeFrom, float fleeDistance, double fleeSlowSpeed, double fleeFastSpeed, Predicate<LivingEntity> inclusionSelector, CallbackInfo ci) {
        if (fleeingEntity instanceof FoxEntity && PlayerEntity.class.equals(classToFleeFrom)) {
            this.classToFleeFrom = (Class<T>) PolarBearEntity.class;
        }
    }
}