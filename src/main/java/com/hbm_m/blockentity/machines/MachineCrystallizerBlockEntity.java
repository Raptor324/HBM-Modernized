package com.hbm_m.blockentity.machines;

import com.hbm_m.platform.PlatformHooks;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.VanillaFluidEquivalence;
import com.hbm_m.sound.ClientSoundBootstrap;
import com.hbm_m.block.machines.MachineCrystallizerBlock;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.interfaces.IItemFluidIdentifier;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachineCrystallizerMenu;
import com.hbm_m.item.fekal_electric.ItemCreativeBattery;
import com.hbm_m.item.liquids.FluidIdentifierItem;
import com.hbm_m.recipe.CrystallizerRecipe;
import com.hbm_m.recipe.CrystallizerRecipes;

import dev.architectury.fluid.FluidStack;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
//? if forge {
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
//?}

//? if fabric {
/*import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
*///?}

/**
 * Crystallizer BlockEntity вЂ” СЂСѓРґРЅС‹Р№ РѕРєРёСЃР»РёС‚РµР»СЊ, РїРѕСЂС‚ СЃ 1.7.10.
 *
 * <p>РЎР»РѕС‚С‹:</p>
 * <ul>
 *   <li>0 вЂ” РІС…РѕРґ (СЂСѓРґР° / РїСЂРµРґРјРµС‚)</li>
 *   <li>1 вЂ” Р±Р°С‚Р°СЂРµСЏ</li>
 *   <li>2 вЂ” РІС‹С…РѕРґ (РєСЂРёСЃС‚Р°Р»Р»)</li>
 *   <li>3 вЂ” СЃР»РѕС‚ Р·Р°Р»РёРІРєРё Р¶РёРґРєРѕСЃС‚Рё (РІРµРґСЂРѕ/РєРѕРЅС‚РµР№РЅРµСЂ СЃ РєРёСЃР»РѕС‚РѕР№)</li>
 *   <li>4 вЂ” СЃР»РѕС‚ РІС‹С…РѕРґР° Р¶РёРґРєРѕСЃС‚Рё (РѕРїСѓСЃС‚РµРІС€РёР№ РєРѕРЅС‚РµР№РЅРµСЂ)</li>
 *   <li>5, 6 вЂ” Р°РїРіСЂРµР№РґС‹ (РїРѕРєР° РЅРµ СЂРµР°Р»РёР·РѕРІР°РЅРѕ)</li>
 *   <li>7 вЂ” СЃР»РѕС‚ РёРґРµРЅС‚РёС„РёРєР°С‚РѕСЂР° Р¶РёРґРєРѕСЃС‚Рё (РїРѕРєР° РЅРµ СЂРµР°Р»РёР·РѕРІР°РЅРѕ)</li>
 * </ul>
 *
 * <p>Р›РѕРіРёРєР° РѕР±СЂР°Р±РѕС‚РєРё:</p>
 * <ol>
 *   <li>Р—Р°СЂСЏРґРєР° РѕС‚ Р±Р°С‚Р°СЂРµРё РІ СЃР»РѕС‚Рµ 1.</li>
 *   <li>РџРµСЂРµРЅРѕСЃ Р¶РёРґРєРѕСЃС‚Рё РёР· РєРѕРЅС‚РµР№РЅРµСЂР° РІ СЃР»РѕС‚Рµ 3 РІ РІРЅСѓС‚СЂРµРЅРЅРёР№ Р±Р°Рє.</li>
 *   <li>РџРѕРёСЃРє СЂРµС†РµРїС‚Р° РІ {@link CrystallizerRecipes} РїРѕ РІС…РѕРґСѓ Рё С‚РµРєСѓС‰РµР№ Р¶РёРґРєРѕСЃС‚Рё РІ Р±Р°РєРµ.</li>
 *   <li>Р•СЃР»Рё СЂРµС†РµРїС‚ РЅР°Р№РґРµРЅ Рё РµСЃС‚СЊ СЌРЅРµСЂРіРёСЏ / РєРёСЃР»РѕС‚Р° / РјРµСЃС‚Рѕ РІ РІС‹С…РѕРґРµ вЂ” РєСЂСѓС‚РёРј РїСЂРѕРіСЂРµСЃСЃ.</li>
 *   <li>РџРѕ РґРѕСЃС‚РёР¶РµРЅРёРё {@code duration} вЂ” РІС‹РґР°С‘Рј СЂРµР·СѓР»СЊС‚Р°С‚, С‚СЂР°С‚РёРј РєРёСЃР»РѕС‚Сѓ,
 *       СЃ СѓС‡С‘С‚РѕРј productivity С‚СЂР°С‚РёРј (РёР»Рё РЅРµ С‚СЂР°С‚РёРј) РІС…РѕРґ.</li>
 * </ol>
 */
public class MachineCrystallizerBlockEntity extends BaseMachineBlockEntity
        implements com.hbm_m.api.fluids.IFluidStandardReceiverMK2 {

    private static final String CRYSTALLIZER_SOUND_INSTANCE = "com.hbm_m.sound.CrystallizerSoundInstance";

    private static final int SLOT_INPUT = 0;
    private static final int SLOT_BATTERY = 1;
    private static final int SLOT_OUTPUT = 2;
    private static final int SLOT_FLUID_INPUT = 3;
    private static final int SLOT_FLUID_OUTPUT = 4;
    private static final int SLOT_UPGRADE_1 = 5;
    private static final int SLOT_UPGRADE_2 = 6;
    private static final int SLOT_FLUID_ID = 7;

    private static final int SLOT_COUNT = 8;
    private static final long MAX_POWER = 1_000_000;
    private static final long MAX_RECEIVE = 1_000;
    private static final int TANK_CAPACITY = 8_000;
    private static final int DEFAULT_DURATION = 600;
    private static final int BASE_POWER_PER_TICK = 1_000;

    private final FluidTank tank = new FluidTank(TANK_CAPACITY) {
        @Override
        public void onContentsChanged() {
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    };
    //? if forge {
    // FluidTank itself is NOT an IFluidHandler; it exposes Forge handler via getCapability().
    private final LazyOptional<IFluidHandler> tankHandler = tank.getCapability();
    //?}

    private int progress = 0;
    private int duration = DEFAULT_DURATION;
    private boolean isOn = false;

    // Client-side visual state for the rotating center part.
    public float angle = 0.0F;
    public float prevAngle = 0.0F;

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> getDuration();
                case 2 -> (int) (getEnergyStored() & 0xFFFFFFFFL);          // energy low 32 bits
                case 3 -> (int) ((getEnergyStored() >>> 32) & 0xFFFFFFFFL); // energy high 32 bits
                case 4 -> (int) (getMaxEnergyStored() & 0xFFFFFFFFL);          // maxEnergy low 32 bits
                case 5 -> (int) ((getMaxEnergyStored() >>> 32) & 0xFFFFFFFFL); // maxEnergy high 32 bits
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return 6;
        }
    };

    public MachineCrystallizerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRYSTALLIZER.get(), pos, state, SLOT_COUNT, MAX_POWER, MAX_RECEIVE);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineCrystallizerBlockEntity entity) {
        if (level.isClientSide) {
            entity.clientTick(level, pos);
            return;
        }

        entity.ensureNetworkInitialized();
        entity.chargeFromBattery();
        entity.applyFluidIdentifier();
        entity.transferFluidsFromItems();

        // РџРѕРёСЃРє СЂРµС†РµРїС‚Р° РїРѕ РІС…РѕРґСѓ + С‚РµРєСѓС‰РµР№ Р¶РёРґРєРѕСЃС‚Рё РІ Р±Р°РєРµ.
        ItemStack inputStack = entity.inventory.getStackInSlot(SLOT_INPUT);
        FluidStack tankFluid = entity.getTankFluidStack();
        CrystallizerRecipe recipe = CrystallizerRecipes.findRecipe(inputStack, tankFluid);

        boolean wasOn = entity.isOn;
        entity.isOn = false;

        if (recipe != null) {
            // Р”Р»РёС‚РµР»СЊРЅРѕСЃС‚СЊ СЃ СѓС‡С‘С‚РѕРј Р°РїРіСЂРµР№РґР° СЃРєРѕСЂРѕСЃС‚Рё (РїРѕРєР° Р·Р°РіР»СѓС€РєР° вЂ” Р±РµСЂС‘Рј РёР· СЂРµС†РµРїС‚Р°).
            entity.duration = entity.calcDuration(recipe);

            if (entity.canProcess(recipe)) {
                int powerCost = entity.getPowerRequired();
                entity.setEnergyStored(entity.getEnergyStored() - powerCost);
                entity.progress++;
                entity.isOn = true;

                if (entity.progress >= entity.duration) {
                    entity.progress = 0;
                    entity.processItem(recipe);
                }
                entity.setChanged();
                entity.sendUpdateToClient();
            } else {
                if (entity.progress != 0) {
                    entity.progress = 0;
                    entity.setChanged();
                }
            }
        } else {
            if (entity.progress != 0) {
                entity.progress = 0;
                entity.setChanged();
            }
        }

        // РћР±РЅРѕРІРёРј РєР»РёРµРЅС‚Р°, РµСЃР»Рё РїРѕРјРµРЅСЏР»СЃСЏ СЃС‚Р°С‚СѓСЃ "РІРєР»/РІС‹РєР»" (РґР»СЏ СЂРµРЅРґРµСЂР° Рё РёРЅРґРёРєР°С‚РѕСЂРѕРІ).
        if (wasOn != entity.isOn) {
            entity.sendUpdateToClient();
        }
    }


    /**
     * Client-side visuals: rotate the center part while the machine works and spawn
     * small old-style white steam particles from the roof.
     */
    private void clientTick(Level level, BlockPos pos) {
        ClientSoundBootstrap.updateSound(this, isOn, () -> newCrystallizerSoundInstance());

        prevAngle = angle;

        if (isOn) {
            angle += 5.0F;
            if (angle >= 360.0F) {
                angle -= 360.0F;
                prevAngle -= 360.0F;
            }

            // The original 1.7.10 machine used small white smoke/steam puffs, not the
            // large campfire smoke that exists in newer Minecraft versions. CLOUD is
            // visually closer: small white squares that drift upward and fade out.
            if (level.random.nextInt(4) == 0) {
                double x = pos.getX() + 0.5D + (level.random.nextDouble() - 0.5D) * 0.85D;
                double y = pos.getY() + 6.5D;
                double z = pos.getZ() + 0.5D + (level.random.nextDouble() - 0.5D) * 0.85D;

                double vx = (level.random.nextDouble() - 0.5D) * 0.010D;
                double vy = 0.025D + level.random.nextDouble() * 0.015D;
                double vz = (level.random.nextDouble() - 0.5D) * 0.010D;

                level.addParticle(ParticleTypes.CLOUD, x, y, z, vx, vy, vz);
            }
        }
    }

    private Object newCrystallizerSoundInstance() {
        try {
            return Class.forName(CRYSTALLIZER_SOUND_INSTANCE)
                    .getConstructor(BlockPos.class)
                    .newInstance(this.getBlockPos());
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * РџСЂРѕРІРµСЂСЏРµС‚, РјРѕР¶РЅРѕ Р»Рё Р·Р°РїСѓСЃС‚РёС‚СЊ РёР»Рё РїСЂРѕРґРѕР»Р¶РёС‚СЊ РєСЂР°С„С‚.
     */
    private boolean canProcess(CrystallizerRecipe recipe) {
        ItemStack inputStack = inventory.getStackInSlot(SLOT_INPUT);

        // РҐРІР°С‚Р°РµС‚ Р»Рё РєРѕР»РёС‡РµСЃС‚РІР° РІС…РѕРґРЅРѕРіРѕ РїСЂРµРґРјРµС‚Р°.
        if (inputStack.getCount() < recipe.getInputCount()) return false;

        // РҐРІР°С‚Р°РµС‚ Р»Рё СЌРЅРµСЂРіРёРё РЅР° С‚РёРє.
        if (getEnergyStored() < getPowerRequired()) return false;

        // РҐРІР°С‚Р°РµС‚ Р»Рё РєРёСЃР»РѕС‚С‹ РІ Р±Р°РєРµ.
        if (recipe.getAcid() != null && tank.getFluidAmountMb() < recipe.getAcidAmount()) {
            return false;
        }

        // РџРѕРјРµСЃС‚РёС‚СЃСЏ Р»Рё СЂРµР·СѓР»СЊС‚Р°С‚ РІ РІС‹С…РѕРґРЅРѕР№ СЃР»РѕС‚.
        ItemStack outSlot = inventory.getStackInSlot(SLOT_OUTPUT);
        ItemStack out = recipe.getOutput();
        if (!outSlot.isEmpty()) {
            if (!PlatformHooks.isSameItemSameTags(outSlot, out)) return false;
            if (outSlot.getCount() + out.getCount() > outSlot.getMaxStackSize()) return false;
        }

        return true;
    }

    /**
     * Р—Р°РІРµСЂС€РµРЅРёРµ РєСЂР°С„С‚Р°: РІС‹РґР°С‚СЊ РІС‹С…РѕРґ, СЃР»РёС‚СЊ РєРёСЃР»РѕС‚Сѓ, РїРѕС‚СЂР°С‚РёС‚СЊ РІС…РѕРґ (СЃ СѓС‡С‘С‚РѕРј productivity).
     */
    private void processItem(CrystallizerRecipe recipe) {
        ItemStack out = recipe.getOutput().copy();
        ItemStack outSlot = inventory.getStackInSlot(SLOT_OUTPUT);
        if (outSlot.isEmpty()) {
            inventory.setStackInSlot(SLOT_OUTPUT, out);
        } else {
            outSlot.grow(out.getCount());
        }

        // РЎР»РёС‚СЊ РєРёСЃР»РѕС‚Сѓ, РµСЃР»Рё СЂРµС†РµРїС‚ РµС‘ С‚СЂРµР±СѓРµС‚.
        if (recipe.getAcid() != null && recipe.getAcidAmount() > 0) {
            tank.drainMb(recipe.getAcidAmount());
        }

        // Productivity: С€Р°РЅСЃ РЅРµ С‚СЂР°С‚РёС‚СЊ РІС…РѕРґ. РЎ Р°РїРіСЂРµР№РґРѕРј EFFECT С€Р°РЅСЃ СЂР°СЃС‚С‘С‚
        // (РїРѕРєР° Р±РµР· Р°РїРіСЂРµР№РґРѕРІ вЂ” Р±РµСЂС‘Рј Р±Р°Р·РѕРІРѕРµ Р·РЅР°С‡РµРЅРёРµ РёР· СЂРµС†РµРїС‚Р°).
        float freeChance = recipe.getProductivity();
        if (freeChance <= 0f || level.random.nextFloat() >= freeChance) {
            inventory.getStackInSlot(SLOT_INPUT).shrink(recipe.getInputCount());
        }

        setChanged();
    }

    /**
     * РџСЂРёРјРµРЅСЏРµС‚ Р¶РёРґРєРѕСЃС‚РЅС‹Р№ РёРґРµРЅС‚РёС„РёРєР°С‚РѕСЂ РёР· СЃР»РѕС‚Р° 7 Рє Р±Р°РєСѓ.
     *
     * <p>Р•СЃР»Рё РІ СЃР»РѕС‚Рµ Р»РµР¶РёС‚ {@link IItemFluidIdentifier} (РёР»Рё {@link FluidIdentifierItem}),
     * Р±РµСЂС‘С‚СЃСЏ РїРµСЂРІРёС‡РЅС‹Р№ С‚РёРї Р¶РёРґРєРѕСЃС‚Рё Рё СЃСЂР°РІРЅРёРІР°РµС‚СЃСЏ СЃ С‚РµРєСѓС‰РёРј С‚РёРїРѕРј Р±Р°РєР°. Р•СЃР»Рё РѕРЅРё СЂР°Р·Р»РёС‡Р°СЋС‚СЃСЏ вЂ”
     * Р±Р°Рє РїРµСЂРµРєР»СЋС‡Р°РµС‚СЃСЏ РЅР° РЅРѕРІС‹Р№ С‚РёРї, РёРјРµСЋС‰Р°СЏСЃСЏ Р¶РёРґРєРѕСЃС‚СЊ СЃР»РёРІР°РµС‚СЃСЏ.</p>
     *
     * <p>Р’С‹Р·С‹РІР°РµС‚СЃСЏ РєР°Р¶РґС‹Р№ С‚РёРє, РїРѕСЌС‚РѕРјСѓ РґРѕСЃС‚Р°С‚РѕС‡РЅРѕ РїРѕР»РѕР¶РёС‚СЊ РёРґРµРЅС‚РёС„РёРєР°С‚РѕСЂ РІ СЃР»РѕС‚ РѕРґРёРЅ СЂР°Р·
     * (РґР°Р¶Рµ РЅР° 1 С‚РёРє): РјР°С€РёРЅР° РїРµСЂРµРєР»СЋС‡РёС‚СЃСЏ РјРіРЅРѕРІРµРЅРЅРѕ, РїРѕСЃР»Рµ С‡РµРіРѕ РёРґРµРЅС‚РёС„РёРєР°С‚РѕСЂ РјРѕР¶РЅРѕ Р·Р°Р±СЂР°С‚СЊ
     * Р±РµР· РїРѕСЃР»РµРґСЃС‚РІРёР№ вЂ” Р±Р°Рє СЃРѕС…СЂР°РЅСЏРµС‚ СЃРІРѕР№ С‚РёРї.</p>
     */
    private void applyFluidIdentifier() {
        ItemStack idStack = inventory.getStackInSlot(SLOT_FLUID_ID);
        if (idStack.isEmpty()) return;

        Fluid resolved = resolveIdentifierFluid(idStack);
        if (resolved == null) return;

        Fluid currentType = tank.getTankType();

        // Р•СЃР»Рё С‚РёРї СѓР¶Рµ СЃРѕРІРїР°РґР°РµС‚ вЂ” РЅРёС‡РµРіРѕ РЅРµ РґРµР»Р°РµРј (РЅРµ С‚СЂРѕРіР°РµРј СЃРѕРґРµСЂР¶РёРјРѕРµ Р±Р°РєР°).
        if (VanillaFluidEquivalence.sameSubstance(resolved, currentType)) {
            return;
        }

        // РџРµСЂРµРєР»СЋС‡Р°РµРј С‚РёРї Р±Р°РєР°. Р•СЃР»Рё РІ РЅС‘Рј РµСЃС‚СЊ В«СЃС‚Р°СЂР°СЏВ» Р¶РёРґРєРѕСЃС‚СЊ вЂ” РѕРЅР° СЃР»РёРІР°РµС‚СЃСЏ.
        tank.assignTypeAndZeroFluid(resolved);
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /**
     * РР·РІР»РµРєР°РµС‚ РїРµСЂРІРёС‡РЅС‹Р№ С‚РёРї Р¶РёРґРєРѕСЃС‚Рё РёР· РёРґРµРЅС‚РёС„РёРєР°С‚РѕСЂР°. Р’РѕР·РІСЂР°С‰Р°РµС‚ {@code null}, РµСЃР»Рё СЃС‚СЌРє
     * РЅРµ РёРґРµРЅС‚РёС„РёРєР°С‚РѕСЂ РёР»Рё РµРіРѕ С‚РёРї РЅРµР»СЊР·СЏ РїСЂРµРІСЂР°С‚РёС‚СЊ РІ СЂРµР°Р»СЊРЅС‹Р№ {@link Fluid}.
     */
    @Nullable
    private Fluid resolveIdentifierFluid(ItemStack stack) {
        if (stack.getItem() instanceof FluidIdentifierItem) {
            // РЈ FluidIdentifierItem СѓР¶Рµ РµСЃС‚СЊ СѓРґРѕР±РЅС‹Р№ resolver: РІРѕР·РІСЂР°С‰Р°РµС‚ {@code ModFluids.NONE} РІРјРµСЃС‚Рѕ EMPTY.
            return FluidIdentifierItem.resolvePrimaryForTank(stack);
        }
        if (stack.getItem() instanceof IItemFluidIdentifier idItem) {
            Fluid f = idItem.getType(level, worldPosition, stack);
            if (f == null || f == Fluids.EMPTY) return null;
            return f;
        }
        return null;
    }

    /**
     * РџРѕСЃС‚РµРїРµРЅРЅР°СЏ РїРµСЂРµРєР°С‡РєР° Р¶РёРґРєРѕСЃС‚Рё РёР· РєРѕРЅС‚РµР№РЅРµСЂР° РІ СЃР»РѕС‚Рµ 3 РІ РІРЅСѓС‚СЂРµРЅРЅРёР№ Р±Р°Рє.
     *
     * <p>Р›РѕРіРёРєР°: РєР°Р¶РґС‹Р№ С‚РёРє РїС‹С‚Р°РµРјСЃСЏ РІС‹СЃРѕСЃР°С‚СЊ РјР°РєСЃРёРјСѓРј РёР· РєРѕРЅС‚РµР№РЅРµСЂР° РІ Р±Р°Рє (РЅРѕ РЅРµ Р±РѕР»РµРµ
     * С‚РѕРіРѕ, С‡С‚Рѕ РІР»РµР·Р°РµС‚). Р•СЃР»Рё Р±Р°Рє РЅРµ РїСЂРёРЅРёРјР°РµС‚ (С‚РёРї Р¶РёРґРєРѕСЃС‚Рё РЅРµ СЃРѕРІРїР°РґР°РµС‚ РёР»Рё РЅРµС‚ РјРµСЃС‚Р°) вЂ”
     * РЅРёС‡РµРіРѕ РЅРµ РґРµР»Р°РµРј. РљРѕРіРґР° РєРѕРЅС‚РµР№РЅРµСЂ СЃС‚Р°РЅРѕРІРёС‚СЃСЏ РїСѓСЃС‚ вЂ” РїРµСЂРµРјРµС‰Р°РµРј РµРіРѕ РІ СЃР»РѕС‚ 4 (РІС‹С…РѕРґ).</p>
     *
     * <p>Р­С‚Рѕ СЂРµС€Р°РµС‚ РїСЂРѕР±Р»РµРјСѓ СЃ Р±РѕР»СЊС€РёРјРё Р±РѕС‡РєР°РјРё (16 000 mB), РєРѕС‚РѕСЂС‹Рµ РЅРµ РїРѕРјРµС‰Р°СЋС‚СЃСЏ РІ Р±Р°Рє
     * Р·Р° РѕРґРёРЅ РїСЂРёСЃРµСЃС‚: Р±РѕС‡РєР° РѕСЃС‚Р°С‘С‚СЃСЏ РІ РІРµСЂС…РЅРµРј СЃР»РѕС‚Рµ Рё РїСЂРѕРґРѕР»Р¶Р°РµС‚ РґРѕР»РёРІР°С‚СЊ РїРѕ РјРµСЂРµ С‚РѕРіРѕ,
     * РєР°Рє РјР°С€РёРЅР° СЂР°СЃС…РѕРґСѓРµС‚ РєРёСЃР»РѕС‚Сѓ.</p>
     */
    private void transferFluidsFromItems() {
        ItemStack fillStack = inventory.getStackInSlot(SLOT_FLUID_INPUT);
        if (fillStack.isEmpty()) return;

        //? if forge {
        IFluidHandler tankH = tankHandler.orElse(null);
        if (tankH == null) return;

        // Р‘РµР· Р¶РёРґРєРѕСЃС‚РЅРѕРіРѕ РёРґРµРЅС‚РёС„РёРєР°С‚РѕСЂР° Р±Р°Рє РЅРµ РїСЂРёРЅРёРјР°РµС‚ РЅРёС‡РµРіРѕ вЂ” РєР°Рє РІ РѕСЂРёРіРёРЅР°Р»Рµ 1.7.10.
        // Р•СЃР»Рё С‚РёРї Р±Р°РєР° РЅРµ Р·Р°РґР°РЅ (Fluids.EMPTY РёР»Рё ModFluids.NONE) вЂ” РІС‹С…РѕРґРёРј, РїСѓСЃС‚СЊ РёРіСЂРѕРє
        // СЃРЅР°С‡Р°Р»Р° РїРѕСЃС‚Р°РІРёС‚ РёРґРµРЅС‚РёС„РёРєР°С‚РѕСЂ РІ СЃР»РѕС‚ 7.
        Fluid currentType = tank.getTankType();
        if (currentType == Fluids.EMPTY || currentType == ModFluids.NONE.getSource()) {
            return;
        }

        // Р‘РµСЂС‘Рј РѕС‚РґРµР»СЊРЅС‹Р№ СЃС‚Р°Рє РЅР° 1 РїСЂРµРґРјРµС‚ вЂ” С‡С‚РѕР±С‹ РЅРµ РјРѕРґРёС„РёС†РёСЂРѕРІР°С‚СЊ С†РµР»С‹Р№ СЃС‚Р°Рє СЃСЂР°Р·Сѓ.
        // (РҐРѕС‚СЏ РІ СЃР»РѕС‚ РјС‹ Рё С‚Р°Рє РїСѓСЃРєР°РµРј РјР°РєСЃРёРјСѓРј 1, РЅР° РІСЃСЏРєРёР№ СЃР»СѓС‡Р°Р№ СЃС‚СЂР°С…СѓРµРјСЃСЏ.)
        ItemStack singleItem = fillStack.copy();
        singleItem.setCount(1);

        var itemCapOpt = singleItem.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER_ITEM);
        var itemHandler = itemCapOpt.orElse(null);
        if (itemHandler == null) return;

        // РЎРѕСЃС‘Рј РёР· РєРѕРЅС‚РµР№РЅРµСЂР° СЃС‚РѕР»СЊРєРѕ, СЃРєРѕР»СЊРєРѕ РІР»РµР·Р°РµС‚ РІ Р±Р°Рє.
        net.minecraftforge.fluids.FluidStack drained = itemHandler.drain(
                tankH.getTankCapacity(0), IFluidHandler.FluidAction.SIMULATE);
        if (drained.isEmpty()) {
            // РљРѕРЅС‚РµР№РЅРµСЂ СѓР¶Рµ РїСѓСЃС‚ вЂ” РїСЂРѕР±СѓРµРј РїРµСЂРµРјРµСЃС‚РёС‚СЊ РµРіРѕ РІ РІС‹С…РѕРґРЅРѕР№ СЃР»РѕС‚ (СЃРѕ СЃС‚СЌРєРѕРІР°РЅРёРµРј).
            tryMoveContainerToOutput(itemHandler.getContainer(), fillStack);
            return;
        }

        int filled = tankH.fill(drained, IFluidHandler.FluidAction.SIMULATE);
        if (filled <= 0) return; // Р±Р°Рє РЅРµ РїСЂРёРЅРёРјР°РµС‚ (РґСЂСѓРіР°СЏ Р¶РёРґРєРѕСЃС‚СЊ / РЅРµС‚ РјРµСЃС‚Р°).

        // Р РµР°Р»СЊРЅРѕ РїРµСЂРµР»РёРІР°РµРј filled mB.
        net.minecraftforge.fluids.FluidStack actuallyDrained = itemHandler.drain(filled, IFluidHandler.FluidAction.EXECUTE);
        tankH.fill(actuallyDrained, IFluidHandler.FluidAction.EXECUTE);

        // РљРѕРЅС‚РµР№РЅРµСЂ РјРѕРі РѕР±РЅРѕРІРёС‚СЊСЃСЏ (РЅРѕРІС‹Р№ NBT, РЅР°РїСЂРёРјРµСЂ).
        ItemStack updatedContainer = itemHandler.getContainer();

        // Р•СЃР»Рё РїРѕСЃР»Рµ СЃР»РёРІР° РєРѕРЅС‚РµР№РЅРµСЂ РїСѓСЃС‚ вЂ” РІС‹РіРѕРЅСЏРµРј РµРіРѕ РІ РІС‹С…РѕРґРЅРѕР№ СЃР»РѕС‚.
        var afterCapOpt = updatedContainer.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER_ITEM);
        var afterHandler = afterCapOpt.orElse(null);
        boolean nowEmpty = afterHandler == null
                || afterHandler.drain(1, IFluidHandler.FluidAction.SIMULATE).isEmpty();

        if (nowEmpty) {
            tryMoveContainerToOutput(updatedContainer, fillStack);
        } else {
            // РљРѕРЅС‚РµР№РЅРµСЂ РµС‰С‘ РЅРµ РїСѓСЃС‚РѕР№ вЂ” РѕСЃС‚Р°РІР»СЏРµРј РІРѕ РІС…РѕРґРЅРѕРј СЃР»РѕС‚Рµ.
            inventory.setStackInSlot(SLOT_FLUID_INPUT, updatedContainer);
        }
        setChanged();
        //?}

        //? if fabric {
        /*ItemStack one = fillStack.copy();
        one.setCount(1);

        Storage<FluidVariant> itemStorage = FluidStorage.ITEM.find(one, null);
        if (itemStorage == null) return;

        try (Transaction tx = Transaction.openOuter()) {
            long moved = net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil.move(
                    itemStorage,
                    tank.getStorage(),
                    v -> true,
                    Long.MAX_VALUE,
                    tx
            );
            if (moved > 0) {
                tx.commit();
                inventory.setStackInSlot(SLOT_FLUID_INPUT, ItemStack.EMPTY);
                inventory.setStackInSlot(SLOT_FLUID_OUTPUT, one);
                setChanged();
            }
        }
        *///?}
    }

    //? if forge {
    /**
     * РџС‹С‚Р°РµС‚СЃСЏ РїРѕР»РѕР¶РёС‚СЊ РѕРїСѓСЃС‚РѕС€С‘РЅРЅС‹Р№ РєРѕРЅС‚РµР№РЅРµСЂ РІ РІС‹С…РѕРґРЅРѕР№ СЃР»РѕС‚.
     *
     * <p>РЎС†РµРЅР°СЂРёРё:</p>
     * <ul>
     *   <li>РљРѕРЅС‚РµР№РЅРµСЂ РёСЃС‡РµР· (РЅР°РїСЂРёРјРµСЂ, РІРµРґСЂРѕ РІ Forge вЂ” drain СѓРЅРёС‡С‚РѕР¶Р°РµС‚ РІРµРґСЂРѕ) в†’ РїСЂРѕСЃС‚Рѕ
     *       СѓР±РёСЂР°РµРј 1 РїСЂРµРґРјРµС‚ РёР· РІС…РѕРґРЅРѕРіРѕ СЃР»РѕС‚Р°, РІ РІС‹С…РѕРґ РЅРёС‡РµРіРѕ РЅРµ РєР»Р°РґС‘Рј.</li>
     *   <li>Р’С‹С…РѕРґРЅРѕР№ СЃР»РѕС‚ РїСѓСЃС‚ в†’ РєР»Р°РґС‘Рј РєРѕРЅС‚РµР№РЅРµСЂ С‚СѓРґР°.</li>
     *   <li>Р’ РІС‹С…РѕРґРµ СѓР¶Рµ Р»РµР¶РёС‚ РёРґРµРЅС‚РёС‡РЅС‹Р№ РїСѓСЃС‚РѕР№ РєРѕРЅС‚РµР№РЅРµСЂ СЃ РјРµСЃС‚РѕРј РґРѕ maxStackSize в†’
     *       СЃС‚СЌРєСѓРµРј (СѓРІРµР»РёС‡РёРІР°РµРј count).</li>
     *   <li>Р’ РІС‹С…РѕРґРµ Р»РµР¶РёС‚ РґСЂСѓРіРѕР№ РїСЂРµРґРјРµС‚ РёР»Рё СЃС‚СЌРє СѓР¶Рµ РїРѕР»РѕРЅ в†’ РЅРµ РґРµР»Р°РµРј РЅРёС‡РµРіРѕ,
     *       РєРѕРЅС‚РµР№РЅРµСЂ РѕСЃС‚Р°С‘С‚СЃСЏ РІРѕ РІС…РѕРґРЅРѕРј СЃР»РѕС‚Рµ РґРѕ С‚РµС… РїРѕСЂ РїРѕРєР° РІС‹С…РѕРґ РЅРµ РѕСЃРІРѕР±РѕРґРёС‚СЃСЏ.</li>
     * </ul>
     */
    private void tryMoveContainerToOutput(ItemStack emptyContainer, ItemStack originalFillStack) {
        // РЎР»СѓС‡Р°Р№ 0 вЂ” РєРѕРЅС‚РµР№РЅРµСЂ РёСЃС‡РµР·.
        if (emptyContainer.isEmpty()) {
            ItemStack remaining = originalFillStack.copy();
            remaining.shrink(1);
            inventory.setStackInSlot(SLOT_FLUID_INPUT, remaining);
            return;
        }

        ItemStack outSlot = inventory.getStackInSlot(SLOT_FLUID_OUTPUT);

        // РЎР»СѓС‡Р°Р№ 1 вЂ” РІС‹С…РѕРґ РїСѓСЃС‚РѕР№.
        if (outSlot.isEmpty()) {
            ItemStack remaining = originalFillStack.copy();
            remaining.shrink(1);
            inventory.setStackInSlot(SLOT_FLUID_INPUT, remaining);
            inventory.setStackInSlot(SLOT_FLUID_OUTPUT, emptyContainer);
            return;
        }

        // РЎР»СѓС‡Р°Р№ 2 вЂ” РІС‹С…РѕРґ СЃРѕРґРµСЂР¶РёС‚ С‚Р°РєРѕР№ Р¶Рµ РїСЂРµРґРјРµС‚ СЃ РјРµСЃС‚РѕРј в†’ СЃС‚СЌРєСѓРµРј.
        if (PlatformHooks.isSameItemSameTags(outSlot, emptyContainer)) {
            int max = outSlot.getMaxStackSize();
            int totalAfter = outSlot.getCount() + emptyContainer.getCount();
            if (totalAfter <= max) {
                ItemStack newOut = outSlot.copy();
                newOut.setCount(totalAfter);
                inventory.setStackInSlot(SLOT_FLUID_OUTPUT, newOut);

                ItemStack remaining = originalFillStack.copy();
                remaining.shrink(1);
                inventory.setStackInSlot(SLOT_FLUID_INPUT, remaining);
            }
            // Р•СЃР»Рё totalAfter > max вЂ” РІС‹С…РѕРґ РїРѕР»РѕРЅ, Р¶РґС‘Рј, РєРѕРЅС‚РµР№РЅРµСЂ РѕСЃС‚Р°С‘С‚СЃСЏ РІРѕ РІС…РѕРґРµ.
            return;
        }

        // РЎР»СѓС‡Р°Р№ 3 вЂ” РІС‹С…РѕРґ Р·Р°РЅСЏС‚ РґСЂСѓРіРёРј РїСЂРµРґРјРµС‚РѕРј в†’ Р¶РґС‘Рј, РєРѕРЅС‚РµР№РЅРµСЂ РѕСЃС‚Р°С‘С‚СЃСЏ РІРѕ РІС…РѕРґРµ.
    }
    //?}

    private void chargeFromBattery() {
        ItemStack stack = inventory.getStackInSlot(SLOT_BATTERY);
        if (!stack.isEmpty() && stack.getItem() instanceof ItemCreativeBattery) {
            setEnergyStored(getMaxEnergyStored());
            return;
        }
        chargeFromBatterySlot(SLOT_BATTERY);
    }

    private int calcDuration(CrystallizerRecipe recipe) {
        return recipe.getDuration();
    }

    /** РЎРЅРёРјРѕРє Р±Р°РєР° РґР»СЏ СЃРѕРїРѕСЃС‚Р°РІР»РµРЅРёСЏ СЃ СЂРµС†РµРїС‚Р°РјРё (Architectury {@link FluidStack}, РјРёР»Р»РёР±Р°РєРµС‚С‹). */
    private FluidStack getTankFluidStack() {
        var fluid = tank.getStoredFluid();
        int amount = tank.getFluidAmountMb();
        if (fluid == null || fluid == Fluids.EMPTY || amount <= 0) {
            return FluidStack.empty();
        }
        return FluidStack.create(fluid, amount);
    }

    private boolean canProcess() {
        if (inventory.getStackInSlot(SLOT_INPUT).isEmpty()) return false;
        if (getEnergyStored() < getPowerRequired()) return false;
        // Р—Р°РіР»СѓС€РєР°: CrystallizerRecipes.getOutput - РІСЃРµРіРґР° null
        return false;
    }

    private void processItem() {
        // Р—Р°РіР»СѓС€РєР°: Р»РѕРіРёРєР° РєСЂР°С„С‚РѕРІ
    }

    public int getPowerRequired() {
        return BASE_POWER_PER_TICK;
    }

    public int getDuration() {
        return duration;
    }

    public boolean isOn() {
        return isOn;
    }

    public long getPowerScaled(int scale) {
        long max = getMaxEnergyStored();
        return max <= 0 ? 0 : (getEnergyStored() * scale) / max;
    }

    public int getProgressScaled(int scale) {
        int dur = getDuration();
        return dur <= 0 ? 0 : (progress * scale) / dur;
    }

    public FluidTank getTank() {
        return tank;
    }

    public ContainerData getContainerData() {
        return data;
    }

    // в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ IFluidStandardReceiverMK2 в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ
    // Р РµРіРёСЃС‚СЂРёСЂСѓРµС‚ РѕРєРёСЃР»РёС‚РµР»СЊ РєР°Рє РїСЂРёС‘РјРЅРёРє РІ Р¶РёРґРєРѕСЃС‚РЅРѕР№ СЃРµС‚Рё MK2 (РёСЃРїРѕР»СЊР·СѓРµС‚СЃСЏ
    // UniversalMachinePartBlockEntity РІ СѓРіР»Р°С… РЅРёР¶РЅРµРіРѕ СЃР»РѕСЏ). Р‘РµР· СЌС‚РѕРіРѕ РёРЅС‚РµСЂС„РµР№СЃР° СЃРµС‚СЊ РЅРµ
    // Р·РЅР°РµС‚ С‡С‚Рѕ РЅР°С€ Р±Р°Рє РіРѕС‚РѕРІ РїСЂРёРЅСЏС‚СЊ Р¶РёРґРєРѕСЃС‚СЊ, РїРѕРєР° РІ Р±Р°РєРµ РїСѓСЃС‚Рѕ вЂ” collectControllerFluidTypes
    // РІ part-BE РІРѕР·РІСЂР°С‰Р°Р» Р±С‹ РїСѓСЃС‚РѕРµ РјРЅРѕР¶РµСЃС‚РІРѕ (РѕРЅ СЃРјРѕС‚СЂРёС‚ С‚РѕР»СЊРєРѕ РЅР° Р·Р°Р»РёС‚СѓСЋ Р¶РёРґРєРѕСЃС‚СЊ С‡РµСЂРµР·
    // getFluidInTank). РЎ РёРЅС‚РµСЂС„РµР№СЃРѕРј same РїСѓС‚СЊ РёРґС‘С‚ С‡РµСЂРµР· mk2.getAllTanks() в†’ tank.getTankType(),
    // РєРѕС‚РѕСЂРѕРµ РєРѕСЂСЂРµРєС‚РЅРѕ РІС‹РґР°С‘С‚ РЅР°СЃС‚СЂРѕРµРЅРЅС‹Р№ С‚РёРї РґР°Р¶Рµ РїСЂРё РїСѓСЃС‚РѕРј Р±Р°РєРµ.

    private final FluidTank[] receivingTanksArr = new FluidTank[] { tank };

    @Override
    public FluidTank[] getReceivingTanks() {
        return receivingTanksArr;
    }

    @Override
    public FluidTank[] getAllTanks() {
        return receivingTanksArr;
    }

    @Override
    public boolean isLoaded() {
        // РўРѕС‡РЅР°СЏ РїСЂРѕРІРµСЂРєР° вЂ” РїРѕР·РёС†РёСЏ РґРµР№СЃС‚РІРёС‚РµР»СЊРЅРѕ РІ Р·Р°РіСЂСѓР¶РµРЅРЅРѕРј С‡Р°РЅРєРµ.
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }
    // в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ

    /**
     * The controller BlockEntity is only one block, but the animated spinner/fluid BER is
     * rendered across the whole 3x3x6 multiblock. Without the expanded render bounds,
     * Minecraft frustum-culls the BER when the controller block itself leaves the camera
     * frustum, which makes the spinner and fluid disappear at steep viewing angles.
     */
    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        BlockState state = getBlockState();
        if (state.getBlock() instanceof MachineCrystallizerBlock block
                && state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            return block.getStructureHelper().getRenderBoundingBox(
                    worldPosition,
                    state.getValue(HorizontalDirectionalBlock.FACING),
                    1.25D
            );
        }
        return super.getRenderBoundingBox().inflate(3.0D, 6.0D, 3.0D);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.crystallizer");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_INPUT) {
            // РџСЂРёРЅРёРјР°РµРј С‚РѕР»СЊРєРѕ С‚Рѕ, С‡С‚Рѕ РїРѕРґС…РѕРґРёС‚ С…РѕС‚СЏ Р±С‹ РїРѕРґ РѕРґРёРЅ СЂРµС†РµРїС‚ СЃ С‚РµРєСѓС‰РµР№ Р¶РёРґРєРѕСЃС‚СЊСЋ.
            return CrystallizerRecipes.findRecipe(stack, getTankFluidStack()) != null;
        }
        if (slot == SLOT_BATTERY) {
            if (stack.getItem() instanceof ItemCreativeBattery) return true;
            return isEnergyProviderItem(stack);
        }
        if (slot == SLOT_OUTPUT || slot == SLOT_FLUID_OUTPUT) {
            return false;
        }
        if (slot == SLOT_FLUID_INPUT) {
            //? if forge {
            return stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent();
            //?}
            //? if fabric {
            /*return FluidStorage.ITEM.find(stack, null) != null;
            *///?}
        }
        if (slot == SLOT_FLUID_ID) {
            // РџСЂРёРЅРёРјР°РµРј С‚РѕР»СЊРєРѕ РјСѓР»СЊС‚Рё-Р¶РёРґРєРѕСЃС‚РЅС‹Р№ РёРґРµРЅС‚РёС„РёРєР°С‚РѕСЂ.
            return stack.getItem() instanceof IItemFluidIdentifier;
        }
        return true;
    }

    public boolean stillValid(Player player) {
        return !this.isRemoved() && player.distanceToSqr(this.getBlockPos().getCenter()) <= 64.0D;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachineCrystallizerMenu(containerId, playerInventory, this, data);
    }

    //? if < 1.21.1 {
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("tank", tank.writeNBT(new CompoundTag()));
        tag.putInt("progress", progress);
        tag.putInt("duration", duration);
        tag.putBoolean("isOn", isOn);
    }
    //?} else {
    /*@Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {

        super.saveAdditional(tag, registries);
        tag.put("tank", tank.writeNBT(new CompoundTag()));
        tag.putInt("progress", progress);
        tag.putInt("duration", duration);
        tag.putBoolean("isOn", isOn);
    
    }
    *///?}

    //? if < 1.21.1 {
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("tank")) {
            tank.readNBT(tag.getCompound("tank"));
        }
        progress = tag.getInt("progress");
        duration = tag.contains("duration") ? tag.getInt("duration") : DEFAULT_DURATION;
        isOn = tag.getBoolean("isOn");
    }
    //?} else {
    /*@Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {

        super.loadAdditional(tag, registries);
        if (tag.contains("tank")) {
            tank.readNBT(tag.getCompound("tank"));
        }
        progress = tag.getInt("progress");
        duration = tag.contains("duration") ? tag.getInt("duration") : DEFAULT_DURATION;
        isOn = tag.getBoolean("isOn");
    
    }
    *///?}

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && level.isClientSide) {
            ClientSoundBootstrap.updateSound(this, false, null);
        }
    }

    //? if forge {
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            return tankHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        tankHandler.invalidate();
    }
    //?}
}
