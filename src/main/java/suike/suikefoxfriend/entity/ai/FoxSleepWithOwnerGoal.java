package suike.suikefoxfriend.entity.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import suike.suikefoxfriend.SuiKe;
import suike.suikefoxfriend.api.IOwnable;

import java.util.EnumSet;

public class FoxSleepWithOwnerGoal extends Goal {

    private final Fox fox;
    private final IOwnable ownableFox;
    private LivingEntity owner;
    private BlockPos goalPos;
    private int onBedTicks;

    public FoxSleepWithOwnerGoal(Fox fox) {
        this.fox = fox;
        this.ownableFox = (IOwnable) this.fox;
        this.owner = this.ownableFox.getOwner();
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        this.owner = this.ownableFox.getOwner();
        if (this.owner == null) return false;
        if (owner.isSpectator() || this.fox.distanceTo(this.owner) > 20.0) return false;
        BlockPos ownerPos = this.owner.blockPosition();
        BlockState ownerPosState = this.fox.level().getBlockState(ownerPos);
        if (ownerPosState.is(BlockTags.BEDS)) {
            this.goalPos = ownerPosState.getOptionalValue(BedBlock.FACING).map((bedDir) -> ownerPos.relative(bedDir.getOpposite())).orElseGet(() -> new BlockPos(ownerPos.getX(), ownerPos.getY(), ownerPos.getZ()));
            return !this.spaceIsOccupied();
        }
        return owner.isSleeping() && !this.fox.isFaceplanted() && !this.ownableFox.isWaiting();
    }

    @Override
    public void start() {
        this.owner = this.ownableFox.getOwner();
        this.onBedTicks = 0;
        if (this.fox.isSitting() || this.fox.isSleeping()) this.fox.setSitting(false);
    }

    @Override
    public void stop() {
        this.owner = null;
        this.fox.getNavigation().stop();
        this.ownableFox.setSleepingWithOwner(false);
        this.onBedTicks = 0;
    }

    @Override
    public void tick() {
        if (this.fox.distanceToSqr(this.owner) <= 2.5f) {
            this.onBedTicks++;
            if (this.onBedTicks > this.adjustedTickDelay(16))
                this.ownableFox.mixinSetSleeping(true);
            this.ownableFox.setSleepingWithOwner(true);
        }
        this.fox.getNavigation().moveTo(this.goalPos.getX(), this.goalPos.getY(), this.goalPos.getZ(),1.1);
    }

    @Override
    public boolean canContinueToUse() {
        return owner.isSleeping() && !owner.isSpectator() && !this.spaceIsOccupied();
    }

    private boolean spaceIsOccupied() {
        for(Fox otherFox : this.fox.level().getEntitiesOfClass(Fox.class, (new AABB(this.goalPos)).inflate(2.0))) {
            if (otherFox != this.fox && (((IOwnable) otherFox).isSleepingWithOwner())) {
                return true;
            }
        }

        return false;
    }
}
