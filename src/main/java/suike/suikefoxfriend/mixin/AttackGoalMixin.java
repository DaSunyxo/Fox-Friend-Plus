package suike.suikefoxfriend.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.entity.passive.FoxEntity;
import net.minecraft.entity.passive.RabbitEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import suike.suikefoxfriend.api.IOwnable;

@Mixin(targets = "net.minecraft.entity.passive.FoxEntity$AttackGoal")
public class AttackGoalMixin {

    FoxEntity fox;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(FoxEntity fox, double speed, boolean pauseWhenIdle, CallbackInfo ci) {
        this.fox = fox;
    }

    @ModifyReturnValue(method = "canStart", at = @At("RETURN"))
    private boolean checkFoxTamed(boolean original) {
        return original && !(((IOwnable) fox).isTamed() && (fox.getTarget() instanceof ChickenEntity || fox.getTarget() instanceof RabbitEntity));
    }
}
