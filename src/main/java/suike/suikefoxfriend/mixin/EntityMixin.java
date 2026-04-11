package suike.suikefoxfriend.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import suike.suikefoxfriend.api.IOwnable;

@Mixin(Entity.class)
public class EntityMixin {

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    public void onHurt(DamageSource source, float damage, CallbackInfo ci) {
        if (((LivingEntity) (Object) this) instanceof Fox && source.getEntity() instanceof Avatar) {
            Fox fox = (Fox) (Object) this;
            if (((IOwnable) fox).isTamed()) ci.cancel();
        }
    }
}
