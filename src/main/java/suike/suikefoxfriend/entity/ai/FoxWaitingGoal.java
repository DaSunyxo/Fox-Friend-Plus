package suike.suikefoxfriend.entity.ai;

import suike.suikefoxfriend.api.IOwnable;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.phys.Vec3;

public class FoxWaitingGoal extends Goal {
    private final Fox fox;

    public FoxWaitingGoal(Fox fox) {
        this.fox = fox;
    }

    @Override
    public boolean canUse() {
        if (((IOwnable) this.fox).isWaiting()) {
            ((IOwnable) this.fox).mixinStopActions();
            ((IOwnable) this.fox).mixinSetSleeping(true);  // 修复进入等待依然移动的BUG
            if (!((IOwnable) this.fox).getIsSleeping()) {  //
                this.fox.setSitting(true);                 //
            }                                              //*/
            return true;
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return ((IOwnable) this.fox).isWaiting();
    }

    @Override
    public void start() {}

    @Override
    public void stop() {
        this.fox.setSitting(false);
        this.fox.setIsInterested(true);
        this.fox.setCanPickUpLoot(true);
        ((IOwnable) this.fox).mixinSetSleeping(false);
        this.fox.getNavigation().stop();
    }

    @Override
    public void tick() {
        this.fox.setJumping(false);
        ((IOwnable) this.fox).mixinSetAggressive(false);
        if (((IOwnable) this.fox).getIsSleeping()) {
            // 睡觉时
            /*锁定视角*/this.fox.setIsInterested(false);
            /*不允许收集*/this.fox.setCanPickUpLoot(false);
            /*设为睡觉*/((IOwnable) this.fox).mixinSetSleeping(true);
        } else {
            // 不睡觉时
            /*坐下*/this.fox.setSitting(true);
            LivingEntity owner = ((IOwnable) this.fox).getOwner();
            if (owner != null && this.fox.distanceToSqr(owner) < 5.0D) {
                /*看向主人*/this.fox.getLookControl().setLookAt(owner, 10.0F, (float) this.fox.getMaxHeadXRot());
            }
        }
    }
}