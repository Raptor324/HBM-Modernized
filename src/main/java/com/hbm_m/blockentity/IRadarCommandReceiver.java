package com.hbm_m.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;

/**
 * Площадка / устройство, принимающее команды пуска от радара.
 */
public interface IRadarCommandReceiver {

    boolean sendCommandPosition(BlockPos pos);

    boolean sendCommandEntity(Entity target);
}
