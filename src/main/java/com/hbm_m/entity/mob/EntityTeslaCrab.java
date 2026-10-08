package com.hbm_m.entity.mob;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.blockentity.machines.TeslaBlockEntity;
import com.hbm_m.item.ModItems;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** 1:1 {@code EntityTeslaCrab}: Tesla-Krabbe (10 HP), zappt jeden Tick alles im Radius 3 (beidseitig fuer die Blitze). */
public class EntityTeslaCrab extends EntityCyberCrab {

    public List<Vec3> targets = new ArrayList<>();

    public EntityTeslaCrab(EntityType<? extends EntityTeslaCrab> type, Level world) {
        super(type, world);
        this.noCulling = true;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return EntityCyberCrab.createAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.5F);
    }

    @Override
    public void aiStep() {
        targets = TeslaBlockEntity.zap(level(), getX(), getY() + 1, getZ(), 3, this);
        super.aiStep();
    }

    @Override
    protected void dropRareDrop() {
        this.spawnAtLocation(ModItems.COIL_COPPER.get(), 1);
    }
}
