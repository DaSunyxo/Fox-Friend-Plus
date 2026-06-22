package suike.suikefoxfriend.entity.ai;

import java.util.Random;
import java.util.EnumSet;

import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.core.Direction;
import suike.suikefoxfriend.SuiKe;
import suike.suikefoxfriend.api.IOwnable;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;

public class FoxFollowOwnerGoal extends Goal {
    private final Fox fox;
    private final Level world;
    private final double speed = 1.0D;
    private final float minDistance = 10.0F;
    private final float maxDistance = 20.0F;
    private LivingEntity owner;
    private int updateCountdownTicks;

    public FoxFollowOwnerGoal(Fox fox) {
        this.fox = fox;
        this.owner = ((IOwnable) this.fox).getOwner();
        this.world = fox.level();
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity owner = ((IOwnable) this.fox).getOwner();
        this.owner = owner;
        if (owner == null || owner.isSpectator() || this.cannotFollow()) {
            return false;
        } else if (this.fox.distanceTo(owner) < (double) this.minDistance) {
            return false;
        } else {
            this.owner = owner;
            return true;
        }
    }

    @Override
    public boolean canContinueToUse() {
        if (this.owner == null) {
            return false;
        } else if (this.cannotFollow()) {
            return false;
        } else if (this.fox.distanceTo(this.owner) >= (double) this.maxDistance || this.fox.distanceTo(this.owner) < 2.0) {
            return false;
        } else if (this.fox.getNavigation().isDone()) {
            return false;
        } else {
            return true;
        }
    }

    private boolean cannotFollow() {
        return ((IOwnable) this.fox).isWaiting(); 
    }

    @Override
    public void start() {
        this.owner = ((IOwnable) this.fox).getOwner();
        this.updateCountdownTicks = 0;
    }

    @Override
    public void stop() {
        this.owner = null;
        this.fox.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (this.owner == null) {
            return;
        }
        // 看向主人
        this.fox.getLookControl().setLookAt(this.owner, 10.0F, (float) this.fox.getMaxHeadXRot());

        // 是否传送到主人
        if (--this.updateCountdownTicks <= 0) {
            this.updateCountdownTicks = this.adjustedTickDelay(10);
            if (this.fox.distanceToSqr(this.owner) >= 144.0D) {
                this.tryTeleport();
            } else if (this.fox.canMove()) {
                this.fox.getNavigation().moveTo(this.owner, this.speed);
            }
        }
    }

    public boolean teleport() {
        this.owner = ((IOwnable) this.fox).getOwner();;
        if (owner != null && this.fox.distanceToSqr(this.owner) >= 144.0D) {
            this.tryTeleport();
            this.owner = null;
            return true;
        }
        this.owner = null;
        return false;
    }

    private void tryTeleport() {
        BlockPos ownerPos = this.owner.getOnPos();
        Direction ownerFacing = this.owner.getDirection();
        for (int i = 0; i < 10; ++i) {
            int x = ownerPos.getX();
            int y = ownerPos.getY();
            int z = ownerPos.getZ();
            switch (ownerFacing) {
                case NORTH : {
                    x += this.getRandomInt(-3, 3);
                    y += this.getRandomInt(-1, 1);
                    z += this.getRandomInt(1, 3);
                    break;
                }
                case SOUTH : {
                    x += this.getRandomInt(-3, 3);
                    y += this.getRandomInt(-1, 1);
                    z += this.getRandomInt(-3, -1);
                    break;
                }

                case WEST : {
                    x += this.getRandomInt(1, 3);
                    y += this.getRandomInt(-1, 1);
                    z += this.getRandomInt(-3, 3);
                    break;
                }

                case EAST : {
                    x += this.getRandomInt(-3, -1);
                    y += this.getRandomInt(-1, 1);
                    z += this.getRandomInt(-3, 3);
                    break;
                }
            }
            if (this.tryTeleportTo(x, y, z)) {
                return;
            }
        }
    }

    private boolean tryTeleportTo(int x, int y, int z) {
        if (Math.abs((double) x - this.owner.getX()) < 2.0D && Math.abs((double) z - this.owner.getZ()) < 2.0D) {
            return false;
        } else if (!this.canTeleportTo(new BlockPos(x, y, z))) {
            return false;
        } else {
            this.fox.setPos((double) x + 0.5D, (double) y, (double) z + 0.5D);
            this.fox.getNavigation().stop();
            return true;
        }
    }

    private boolean canTeleportTo(BlockPos pos) {
        PathType pathNodeType = WalkNodeEvaluator.getPathTypeStatic(this.fox, pos.mutable());
        if (pathNodeType != PathType.WALKABLE) {
            return false;
        } else {
            BlockState blockState = this.world.getBlockState(pos.below());
            if (blockState.getBlock() instanceof LeavesBlock || blockState.getBlock() instanceof PoweredRailBlock || blockState.getBlock() instanceof PoweredRailBlock) {
                return false;
            } else {
                BlockPos blockPos = pos.subtract(this.fox.getOnPos());
                return this.world.isEmptyBlock(blockPos);
            }
        }
    }

    private static Random random = new Random();
    private int getRandomInt(int min, int max) {
        int randInt = random.nextInt(max - min + 1) + min;
        while (randInt == 0) {
            randInt = random.nextInt(max - min + 1) + min;
        }
        return randInt;
    }
}