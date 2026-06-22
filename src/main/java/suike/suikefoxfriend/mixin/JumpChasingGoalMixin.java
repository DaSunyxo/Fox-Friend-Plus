package suike.suikefoxfriend.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.fox.Fox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import suike.suikefoxfriend.SuiKe;
import suike.suikefoxfriend.api.IOwnable;

import javax.swing.plaf.SeparatorUI;

// TODO(Ravel): can not resolve target class FoxEntity.JumpChasingGoal
// TODO(Ravel): can not resolve target class FoxEntity.JumpChasingGoal
@Mixin(Fox.FoxPounceGoal.class)
public class JumpChasingGoalMixin {

    private Fox fox;

    // TODO(Ravel): no target class
// TODO(Ravel): no target class
    @Inject(method = "<init>", at = @At("RETURN"))
    private void getFoxEntity(Fox fox, CallbackInfo ci) {
        this.fox = fox;
    }

    // TODO(Ravel): no target class
// TODO(Ravel): no target class
    @Inject(method = "start", at = @At("HEAD"))
    private void onStart(CallbackInfo ci) {
        SuiKe.LOGGER.warn("fops gonna jump chase");
    }

    // TODO(Ravel): no target class
// TODO(Ravel): no target class
    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true)
    private void canStart(CallbackInfoReturnable<Boolean> cir) {
        if (!this.fox.isFullyCrouched()) {
            cir.setReturnValue(false);
        } else {
            LivingEntity livingEntity = fox.getTarget();
            if (livingEntity != null && livingEntity.isAlive()) {
                if (livingEntity.getMotionDirection() != livingEntity.getDirection()) {
                    cir.setReturnValue(false);
                } else {
                    boolean bl = Fox.isPathClear(fox, livingEntity);
                    if (!bl) {
                        if (((IOwnable) this.fox).isTamed()) cir.setReturnValue(false);
                        SuiKe.LOGGER.warn("fops is gonna chase");
                        this.fox.getNavigation().createPath(livingEntity, 0);
                        this.fox.setIsCrouching(false);
                        this.fox.setIsInterested(false);
                    }

                    cir.setReturnValue(bl);
                }
            } else {
                cir.setReturnValue(false);
            }
        }
    }
}
