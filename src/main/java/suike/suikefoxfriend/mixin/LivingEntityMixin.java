package suike.suikefoxfriend.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.PlayerLikeEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.FoxEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import suike.suikefoxfriend.api.IOwnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    public void onDamage(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> ci) {
        if (((LivingEntity) (Object) this) instanceof FoxEntity && source.getAttacker() instanceof PlayerLikeEntity) {
            FoxEntity fox = (FoxEntity) (Object) this;
            if (((IOwnable) fox).isTamed()) ci.setReturnValue(false);
        }
    }
}
