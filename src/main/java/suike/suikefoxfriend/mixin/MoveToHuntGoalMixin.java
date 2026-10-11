package suike.suikefoxfriend.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.animal.fox.Fox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import suike.suikefoxfriend.api.IOwnable;

@Mixin(targets = "net.minecraft.world.entity.animal.fox.Fox$StalkPreyGoal")
public abstract class MoveToHuntGoalMixin {

    private Fox fox;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void getFoxEntity(Fox fox, CallbackInfo ci) {
        this.fox = fox;
    }

    @Inject(method = "canUse", at = @At("RETURN"), cancellable = true)
    private void onCanUse(CallbackInfoReturnable<Boolean> cir) {
        if (((IOwnable) this.fox).isTamed()) cir.setReturnValue(false);
    }
}
