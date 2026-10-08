package com.hbm_m.item.liquids;

import com.hbm_m.item.ITooltipProvider;
import java.util.List;

import dev.architectury.fluid.FluidStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.HbmFluidRegistry;
import com.hbm_m.blockentity.machines.MachineFluidTankBlockEntity;
import com.hbm_m.interfaces.IItemControlReceiver;
import com.hbm_m.interfaces.IItemFluidIdentifier;
import com.hbm_m.interfaces.IMultiblockPart;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.platform.PlatformHooks;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * Universal fluid identifier. Has two slots (primary/secondary) for fluid types.
 * RMB: swap primary and secondary. Shift+RMB in air: open GUI to select fluids.
 * Shift+RMB on a fluid duct: paint that fluid onto the entire connected duct network (same block type).
 */
public class FluidIdentifierItem extends Item implements IItemFluidIdentifier, IItemControlReceiver, ITooltipProvider {

    private static final String NBT_FLUID1 = "fluid1";
    private static final String NBT_FLUID2 = "fluid2";

    public FluidIdentifierItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    /**
     * Tanks/Faesser/BAT9000 (Original MachineFluidTank/BlockFluidBarrel/MachineBigAssTank9000, geschlichener Zweig):
     * die Bloecke geben geschlichen PASS zurueck, die Sorte stellt der Identifikator hier. Rohre laufen ueber
     * {@link com.hbm_m.api.fluids.PipeTypeChanger#onIdentifier} im Block (dank {@link #doesSneakBypassUse}).
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();

        MachineFluidTankBlockEntity tankBE = findTankEntity(level, pos);
        if (tankBE != null) {
            if (tankBE.hasExploded) {
                return InteractionResult.PASS;
            }
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            tankBE.setFilterFromIdentifier(stack);
            Fluid type = getType(level, pos, stack);
            player.displayClientMessage(Component.literal("Changed type to ").withStyle(ChatFormatting.YELLOW)
                    .append(getFluidDisplayName(type)).append(Component.literal("!")), false);
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }

    //? if forge || neoforge {
    /** Original {@code doesSneakBypassUse}: auch geschlichen bekommt der Block den Rechtsklick. */
    @Override
    public boolean doesSneakBypassUse(ItemStack stack, net.minecraft.world.level.LevelReader level, BlockPos pos, Player player) {
        return true;
    }
    //?}

    @Nullable
    private static MachineFluidTankBlockEntity findTankEntity(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof MachineFluidTankBlockEntity tank) {
            return tank;
        }
        if (be instanceof IMultiblockPart part) {
            BlockPos controllerPos = part.getControllerPos();
            if (controllerPos != null) {
                BlockEntity ctrlBE = level.getBlockEntity(controllerPos);
                if (ctrlBE instanceof MachineFluidTankBlockEntity tank) {
                    return tank;
                }
            }
        }
        return null;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty()) return InteractionResultHolder.pass(stack);

        if (player.isShiftKeyDown()) {
            // Изолируем вызов GUI, чтобы сервер не видел клиентских классов
            if (level.isClientSide) {
                ClientProxy.openGUI(player);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        } else {
            if (level.isClientSide) {
                ClientProxy.showSwapActiveTypeToast(stack);
            }
            // Swap primary and secondary on server
            if (!level.isClientSide) {
                Fluid primary = getType(stack, true);
                Fluid secondary = getType(stack, false);
                setType(stack, secondary, true);
                setType(stack, primary, false);
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.25F, 1.25F);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
    }

    //? if forge || neoforge {
    /** Original getContainerItem = Kopie: bleibt beim Handwerk im Raster (Rezepte fuer Kanister-Etiketten u. a.). */
    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        return stack.copy();
    }
    //?}

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        // Original ItemFluidIDMulti: alle vier Zeilen ohne Farbcode (grau)
        tooltip.add(Component.translatable(getDescriptionId() + ".info").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("   ").append(getFluidDisplayName(getType(stack, true)).copy().withStyle(ChatFormatting.GRAY)));
        
        tooltip.add(Component.translatable(getDescriptionId() + ".info2").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("   ").append(getFluidDisplayName(getType(stack, false)).copy().withStyle(ChatFormatting.GRAY)));
    }

    private static Component getFluidDisplayName(Fluid fluid) {
        if (fluid == null
                || fluid == net.minecraft.world.level.material.Fluids.EMPTY
                || fluid == ModFluids.NONE.getSource()) {
            return Component.translatable("fluid.hbm_m.none");
        }
        //? if forge {
        return Component.translatable(fluid.getFluidType().getDescriptionId());
         //?} else {
        /*ResourceLocation key = BuiltInRegistries.FLUID.getKey(fluid);
        // Стандартное соглашение об именовании: "fluid.modid.fluid_name"
                return Component.translatable("fluid." + key.getNamespace() + "." + key.getPath());
        *///?}
    }

    @Override
    public Fluid getType(Level level, BlockPos pos, ItemStack stack) {
        return getType(stack, true);
    }

    @Override
    public void receiveControl(ItemStack stack, CompoundTag data) {
        if (data.contains(NBT_FLUID1)) {
            setType(stack, data.getString(NBT_FLUID1), true);
        }
        if (data.contains(NBT_FLUID2)) {
            setType(stack, data.getString(NBT_FLUID2), false);
        }
    }

    public static Fluid getType(ItemStack stack, boolean primary) {
        String name = getTypeName(stack, primary);
        if (name == null || name.isEmpty() || "none".equals(name)) {
            return ModFluids.NONE.getSource();
        }
        ModFluids.FluidEntry entry = ModFluids.getEntry(name);
        return entry != null ? entry.getSource() : net.minecraft.world.level.material.Fluids.EMPTY;
    }

    /**
     * Резолвит первичный тип идентификатора для задания типа цистерны.
     * Жидкость найдена → возвращает её; первичный не задан / "none" / "empty" → {@link ModFluids#NONE};
     * не FluidIdentifierItem → null.
     */
    @Nullable
    public static Fluid resolvePrimaryForTank(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof FluidIdentifierItem)) {
            return null;
        }
        Fluid t = getType(stack, true);
        if (t != null && t != Fluids.EMPTY) {
            return t;
        }
        return ModFluids.NONE.getSource();
    }

    public static String getTypeName(ItemStack stack, boolean primary) {
        CompoundTag tag = PlatformHooks.getItemTag(stack);
        if (tag == null) return "none";
        String key = primary ? NBT_FLUID1 : NBT_FLUID2;
        return tag.getString(key);
    }

    public static void setType(ItemStack stack, Fluid fluid, boolean primary) {
        setType(stack, fluid != null ? HbmFluidRegistry.getFluidName(fluid) : "none", primary);
    }

    public static void setType(ItemStack stack, String fluidName, boolean primary) {
        PlatformHooks.putString(stack, primary ? NBT_FLUID1 : NBT_FLUID2, fluidName != null ? fluidName : "none");
    }

    /** Returns tint color for primary fluid (for ItemColor). */
    public static int getTintColor(ItemStack stack) {
        Fluid f = getType(stack, true);
        if (f == null || f == net.minecraft.world.level.material.Fluids.EMPTY) return 0xFFFFFF;
        return HbmFluidRegistry.getTintColor(f);
    }

    // ВНУТРЕННИЙ КЛАСС ДЛЯ ИЗОЛЯЦИИ КЛИЕНТСКОГО КОДА ОТ СЕРВЕРА
    private static class ClientProxy {
        public static void openGUI(Player player) {
            net.minecraft.client.Minecraft.getInstance().setScreen(new com.hbm_m.inventory.gui.GUIFluidIdentifier(player));
        }

        /** После свопа активным становится бывший вторичный тип — показываем его в тосте. */
        public static void showSwapActiveTypeToast(ItemStack stack) {
            Fluid newActive = getType(stack, false);
            Component fluidLine = getFluidDisplayName(newActive);
            // Original: PlayerInformPacket(secondary.getConditionalName(), 7, 3000) - nur der Name
            com.hbm_m.client.overlay.OverlayInfoToast.show(
                    fluidLine,
                    60,
                    com.hbm_m.client.overlay.OverlayInfoToast.ID_FLUID_IDENTIFIER_SWAP);
        }
    }
}