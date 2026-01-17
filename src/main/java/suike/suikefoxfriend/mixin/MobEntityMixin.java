package suike.suikefoxfriend.mixin;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.FoxEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(MobEntity.class)
public class MobEntityMixin {
    @ModifyVariable(method = "tryAttack", at = @At("STORE"), ordinal = 0)
    private float modifyAttackDamage(float originalDamage) {
        if (((Object) this) instanceof FoxEntity foxEntity) {
            ItemStack itemStack = foxEntity.getEquippedStack(EquipmentSlot.MAINHAND);
            if (itemStack.isEmpty()) return originalDamage;
            float itemDamage = itemStack.getDamage();
            return originalDamage + itemDamage;
        }
        return originalDamage;
    }
}
