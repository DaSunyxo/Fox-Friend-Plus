package suike.suikefoxfriend.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.FoxEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import suike.suikefoxfriend.SuiKe;
import suike.suikefoxfriend.api.IOwnable;

import javax.swing.plaf.SeparatorUI;

@Mixin(FoxEntity.JumpChasingGoal.class)
public class JumpChasingGoalMixin {

    private FoxEntity fox;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void getFoxEntity(FoxEntity fox, CallbackInfo ci) {
        this.fox = fox;
    }

    @Inject(method = "start", at = @At("HEAD"))
    private void onStart(CallbackInfo ci) {
        SuiKe.LOGGER.warn("fops gonna jump chase");
    }

    @Inject(method = "canStart", at = @At("HEAD"), cancellable = true)
    private void canStart(CallbackInfoReturnable<Boolean> cir) {
        if (!this.fox.isFullyCrouched()) {
            cir.setReturnValue(false);
        } else {
            LivingEntity livingEntity = fox.getTarget();
            if (livingEntity != null && livingEntity.isAlive()) {
                if (livingEntity.getMovementDirection() != livingEntity.getHorizontalFacing()) {
                    cir.setReturnValue(false);
                } else {
                    boolean bl = FoxEntity.canJumpChase(fox, livingEntity);
                    if (!bl) {
                        if (((IOwnable) this.fox).isTamed()) cir.setReturnValue(false);
                        SuiKe.LOGGER.warn("fops is gonna chase");
                        this.fox.getNavigation().findPathTo(livingEntity, 0);
                        this.fox.setCrouching(false);
                        this.fox.setRollingHead(false);
                    }

                    cir.setReturnValue(bl);
                }
            } else {
                cir.setReturnValue(false);
            }
        }
    }
}
