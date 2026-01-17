package suike.suikefoxfriend.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.FoxEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import suike.suikefoxfriend.SuiKe;
import suike.suikefoxfriend.api.IOwnable;

@Mixin(targets = "net.minecraft.entity.passive.FoxEntity$MoveToHuntGoal")
public abstract class MoveToHuntGoalMixin {

    @Shadow
    public abstract void stop();

    private FoxEntity fox;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void getFoxEntity(FoxEntity fox, CallbackInfo ci) {
        this.fox = fox;
    }

    @Inject(method = "start", at = @At("HEAD"))
    private void onStart(CallbackInfo ci) {
        SuiKe.LOGGER.warn("Started MoveToHuntGoal!");
    }

    @Inject(method = "stop", at = @At("HEAD"))
    private void onStop(CallbackInfo ci) {
        SuiKe.LOGGER.warn("Stopped MoveToHuntGoal!");
    }

    @ModifyReturnValue(method = "canStart", at = @At("RETURN"))
    private boolean checkIfFoxTamed(boolean original) {
        return original && !(((IOwnable) this.fox).isTamed());
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void onTick(CallbackInfo ci) {
        LivingEntity livingEntity = fox.getTarget();
        if (((IOwnable) fox).isTamed()) {
            SuiKe.LOGGER.warn("Trying to stop MoveToHuntGoal");
            fox.setRollingHead(true);
            fox.setCrouching(true);
            fox.getNavigation().stop();
            fox.getLookControl().lookAt(livingEntity, fox.getMaxHeadRotation(), fox.getMaxLookPitchChange());
        }
    }
}
