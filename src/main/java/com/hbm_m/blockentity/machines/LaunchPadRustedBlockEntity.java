package com.hbm_m.blockentity.machines;

import com.hbm_m.api.network.NodeDirPos;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.missile.MissileTier4;
import com.hbm_m.explosion.MissileWarheadEffects;
import com.hbm_m.inventory.menu.LaunchPadRustedMenu;
import com.hbm_m.item.ModItems;
import com.hbm_m.sound.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Ржавая пусковая площадка (порт {@code TileEntityLaunchPadRusted}, 1.7.10).
 *
 * Отличия от обычного пада — 1:1 с оригиналом:
 * <ul>
 * <li>никакой энергии и флюидов ({@code hasFuel} всегда true);</li>
 * <li>принимает только {@code missile_doomsday_rusted} — в обход
 *     {@code notLaunchable()} обычных падов (ракета «не встаёт на вооружение»,
 *     это музейный агрегат из силоса);</li>
 * <li>пуск требует {@code launch_code} + {@code launch_key} + готовый дизайнатор:
 *     расходуется <b>только код</b> (по одному за пуск), ключ остаётся;</li>
 * <li>пуск — по фронту редстоуна или детонатором ({@code IBomb});</li>
 * <li>бонусом к оригиналу: ракета загружается предметом в слот 0 — в 1.7.10 пад
 *     бывал заряжен только генерацией силоса ({@code SiloComponent}), а здесь
 *     силосной структуры нет.</li>
 * </ul>
 */
public class LaunchPadRustedBlockEntity extends LaunchPadBaseBlockEntity {

    public static final int SLOT_LAUNCH_CODE = 1;
    public static final int SLOT_LAUNCH_KEY = 2;
    private static final int RUSTED_SLOT_DESIGNATOR = 3;

    @Override
    public int getDesignatorSlot() {
        return RUSTED_SLOT_DESIGNATOR;
    }

    public LaunchPadRustedBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LAUNCH_PAD_RUSTED_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, LaunchPadRustedBlockEntity be) {
        if (level.isClientSide) {
            clientLaunchPadSmokeTick(level, pos, state);
        } else {
            commonServerTick(level, pos, state, be);
        }
    }

    @Override
    protected Component getDefaultName() {
        // В оригинале использовался отдельный GUI, поэтому даём отдельный ключ
        return Component.translatable("container.launchPadRusted");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inv, Player player) {
        return new LaunchPadRustedMenu(containerId, inv, this);
    }

    @Override
    protected boolean isReadyForLaunch() {
        return delay <= 0;
    }

    /**
     * 1.7.10 {@code launch()}: только Damaged Doomsday — предмет {@code notLaunchable()},
     * который обычные пады отвергают; ржавый пад принимает его напрямую.
     */
    @Override
    public boolean isMissileValid(@Nullable ItemStack stack) {
        return stack != null && stack.is(ModItems.MISSILE_DOOMSDAY_RUSTED.get());
    }

    /** 1.7.10: ржавый пад не тратит энергию и флюиды. */
    @Override
    public boolean hasFuel() {
        return true;
    }

    /** 1.7.10 {@code launch()}: помимо ракеты нужны {@code launch_code} + {@code launch_key}. */
    @Override
    public boolean canLaunch() {
        return super.canLaunch()
                && inventory.getStackInSlot(SLOT_LAUNCH_CODE).is(ModItems.LAUNCH_CODE.get())
                && inventory.getStackInSlot(SLOT_LAUNCH_KEY).is(ModItems.LAUNCH_KEY.get());
    }

    /**
     * 1.7.10 спавнит {@code EntityMissileDoomsdayRusted} напрямую, мимо общего
     * реестра ракет пада (предмет {@code notLaunchable()}, в {@code MISSILES} не входит).
     */
    @Override
    protected Entity instantiateMissile(int targetX, int targetZ) {
        if (level == null) {
            return null;
        }
        MissileTier4.MissileDoomsdayRusted missile = ModEntities.MISSILE_DOOMSDAY_RUSTED.get().create(level);
        if (missile == null) {
            return null;
        }
        Vec3 launch = launchPosInWorld();
        missile.initLaunch(launch.x, launch.y, launch.z, targetX, targetZ);
        BlockState padState = level.getBlockState(worldPosition);
        if (padState.hasProperty(HorizontalDirectionalBlock.FACING)) {
            missile.setLaunchFacing(padState.getValue(HorizontalDirectionalBlock.FACING));
        }
        return missile;
    }

    /**
     * 1.7.10 {@code launch()}: звук, {@code missileLoaded = false} и расход
     * <b>одного {@code launch_code}</b>; {@code launch_key} остаётся, энергия
     * и флюиды не тратятся.
     */
    @Override
    protected void finalizeLaunch(Entity missile) {
        if (level == null || level.isClientSide) {
            return;
        }

        level.addFreshEntity(missile);
        Vec3 launch = launchPosInWorld();
        if (level instanceof ServerLevel server) {
            MissileWarheadEffects.spawnLaunchSmoke(server, launch.x, launch.y, launch.z);
        }
        level.playSound(null,
                launch.x,
                launch.y - getLaunchOffset(),
                launch.z,
                ModSounds.MISSILE_TAKEOFF.get(),
                SoundSource.PLAYERS,
                15.0F, 1.0F);

        inventory.getStackInSlot(SLOT_LAUNCH_CODE).shrink(1);
        inventory.getStackInSlot(getMissileSlot()).shrink(1);
        this.delay = 100;
        setChanged();
    }

    /** Точка старта (пад + offset), как в базе — через Sable-совместимость. */
    private Vec3 launchPosInWorld() {
        return com.hbm_m.compat.sable.SableCompat.toWorld(level,
                worldPosition.getX() + 0.5D,
                worldPosition.getY() + getLaunchOffset(),
                worldPosition.getZ() + 0.5D);
    }

    @Override
    public NodeDirPos[] getConPos() {
        return buildStandardConPos(worldPosition);
    }
}
