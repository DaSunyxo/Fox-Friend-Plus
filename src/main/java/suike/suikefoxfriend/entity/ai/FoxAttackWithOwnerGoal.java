package suike.suikefoxfriend.entity.ai;

import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.entity.TamableAnimal;
import suike.suikefoxfriend.api.IOwnable;

public class FoxAttackWithOwnerGoal extends TargetGoal {
    private final IOwnable tameable;
    private LivingEntity attacking;
    private int lastAttackTime;

    public FoxAttackWithOwnerGoal(Fox tameable) {
        super(tameable, false);
        this.tameable = (IOwnable) tameable;
        this.setFlags(EnumSet.of(Goal.Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        if (this.tameable.isTamed() && !this.tameable.isWaiting()) {
            LivingEntity livingEntity = this.tameable.getOwner();
            if (livingEntity == null) {
                return false;
            } else {
                this.attacking = livingEntity.getLastHurtMob();
                int i = livingEntity.getLastHurtMobTimestamp();
                return i != this.lastAttackTime && this.canAttack(this.attacking, TargetingConditions.DEFAULT) && this.tameable.canAttackWithOwner(this.attacking, livingEntity);
            }
        } else {
            return false;
        }
    }

    @Override
    public void start() {
        this.mob.setTarget(this.attacking);
        LivingEntity livingEntity = this.tameable.getOwner();
        if (livingEntity != null) {
            this.lastAttackTime = livingEntity.getLastHurtMobTimestamp();
        }

        super.start();
    }
}
