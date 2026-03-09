package com.hbm_m.api.bomb;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Interface for blocks that can be triggered to explode.
 */
public interface IBomb {

    BombReturnCode explode(Level level, BlockPos pos);

    enum BombReturnCode {
        UNDEFINED(false, ""),
        DETONATED(true, "bomb.detonated"),
        TRIGGERED(true, "bomb.triggered"),
        LAUNCHED(true, "bomb.launched"),
        ERROR_MISSING_COMPONENT(false, "bomb.missingComponent"),
        ERROR_INCOMPATIBLE(false, "bomb.incompatible"),
        ERROR_NO_BOMB(false, "bomb.nobomb");

        private final boolean success;
        private final String unlocalizedMessage;

        BombReturnCode(boolean success, String unlocalizedMessage) {
            this.success = success;
            this.unlocalizedMessage = unlocalizedMessage;
        }

        public String getUnlocalizedMessage() { return unlocalizedMessage; }
        public boolean wasSuccessful() { return success; }
    }
}
