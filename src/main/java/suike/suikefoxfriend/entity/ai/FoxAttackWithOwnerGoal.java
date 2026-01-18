package suike.suikefoxfriend.entity.ai;

import java.util.EnumSet;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.TrackTargetGoal;
import net.minecraft.entity.passive.FoxEntity;
import net.minecraft.entity.passive.TameableEntity;
import suike.suikefoxfriend.api.IOwnable;

public class FoxAttackWithOwnerGoal extends TrackTargetGoal {
    private final IOwnable tameable;
    private LivingEntity attacking;
    private int lastAttackTime;

    public FoxAttackWithOwnerGoal(FoxEntity tameable) {
        super(tameable, false);
        this.tameable = (IOwnable) tameable;
        this.setControls(EnumSet.of(Goal.Control.TARGET));
    }

    @Override
    public boolean canStart() {
        if (this.tameable.isTamed() && !this.tameable.isWaiting()) {
            LivingEntity livingEntity = this.tameable.getOwner();
            if (livingEntity == null) {
                return false;
            } else {
                this.attacking = livingEntity.getAttacking();
                int i = livingEntity.getLastAttackTime();
                return i != this.lastAttackTime && this.canTrack(this.attacking, TargetPredicate.DEFAULT) && this.tameable.canAttackWithOwner(this.attacking, livingEntity);
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
            this.lastAttackTime = livingEntity.getLastAttackTime();
        }

        super.start();
    }
}
