package com.hbm_m.blockentity.machines;

import com.hbm_m.api.redstoneoverradio.IRORInteractive;

/** Port of {@code com.hbm.tileentity.machine.storage.RorModes} (NEXT). */
final class RorModes {

    private RorModes() {}

    static int select(int current, String[] params) {
        int mode = IRORInteractive.parseInt(params[0], 0, 3);
        if (mode != current) return mode;
        return params.length > 1 ? IRORInteractive.parseInt(params[1], 0, 3) : current;
    }
}
