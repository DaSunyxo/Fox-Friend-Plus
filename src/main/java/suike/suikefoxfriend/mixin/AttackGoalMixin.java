package suike.suikefoxfriend.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import suike.suikefoxfriend.api.IOwnable;

@Mixin(targets = "net.minecraft.world.entity.animal.fox.Fox$FoxMeleeAttackGoal")
public class AttackGoalMixin {

    Fox fox;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(Fox fox, double speed, boolean pauseWhenIdle, CallbackInfo ci) {
        this.fox = fox;
    }

    @ModifyReturnValue(method = "canUse", at = @At("RETURN"))
    private boolean checkFoxTamed(boolean original) {
        return original && !(((IOwnable) fox).isTamed() && (fox.getTarget() instanceof Chicken || fox.getTarget() instanceof Rabbit));
    }
}
