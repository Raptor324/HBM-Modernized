package com.hbm_m.inventory.fluid.trait;

import java.io.IOException;
import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.hbm_m.inventory.fluid.tank.FluidTank;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

public class FT_VentRadiation extends FluidTrait {
    
    float radPerMB = 0;
    
    public FT_VentRadiation() { }
    
    public FT_VentRadiation(float rad) {
        this.radPerMB = rad;
    }
    
    public float getRadPerMB() {
        return this.radPerMB;
    }
    
    @Override
    public void onFluidRelease(Level level, BlockPos pos, FluidTank tank, int overflowAmount, FluidReleaseType type) {
        // TODO: Интеграция с системой радиации 1.20.1
        // Пример вызова из 1.7.10:
        // ChunkRadiationManager.proxy.incrementRad(level, pos.getX(), pos.getY(), pos.getZ(), overflowAmount * radPerMB);
    }
    
    @Override
    public void addInfo(List<Component> info) {
        info.add(Component.literal("[Radioactive]").withStyle(ChatFormatting.YELLOW));
    }

    @Override
    public void serializeJSON(JsonWriter writer) throws IOException {
        writer.name("radiation").value(radPerMB);
    }
    
    @Override
    public void deserializeJSON(JsonObject obj) {
        this.radPerMB = obj.get("radiation").getAsFloat();
    }
}