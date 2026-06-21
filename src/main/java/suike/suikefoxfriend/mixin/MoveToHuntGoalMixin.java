package suike.suikefoxfriend.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.fox.Fox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import suike.suikefoxfriend.SuiKe;
import suike.suikefoxfriend.api.IOwnable;

@Mixin(targets = "net.minecraft.world.entity.animal.fox.Fox$StalkPreyGoal")
public abstract class MoveToHuntGoalMixin {

    @Shadow
    public abstract void stop();

    private Fox fox;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void getFoxEntity(Fox fox, CallbackInfo ci) {
        this.fox = fox;
    }

    @ModifyReturnValue(method = "canUse", at = @At("RETURN"))
    private boolean checkIfFoxTamed(boolean original) {
        return original && !(((IOwnable) this.fox).isTamed());
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void onTick(CallbackInfo ci) {
        LivingEntity livingEntity = fox.getTarget();
        if (((IOwnable) fox).isTamed()) {
            fox.setIsInterested(true);
            fox.setIsCrouching(true);
            fox.getNavigation().stop();
            fox.getLookControl().setLookAt(livingEntity, fox.getMaxHeadXRot(), fox.getMaxHeadYRot());
        }
    }
}
