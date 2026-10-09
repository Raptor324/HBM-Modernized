package com.hbm_m.item.machine;

import com.hbm_m.platform.StackNbt;

import java.util.List;

import com.hbm_m.blockentity.machines.MachineReactorResearchBlockEntity;
import com.hbm_m.blockentity.machines.UniversalMachinePartBlockEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import org.jetbrains.annotations.Nullable;

/**
 * 1:1-Port von {@code ItemReactorSensor} (1.7.10): mit einem Rechtsklick auf einen
 * Forschungsreaktor merkt sich der Fuehler dessen Position im NBT
 * ({@code x}/{@code y}/{@code z}). Das Reaktorsteuerpult liest sie aus - siehe
 * {@code TileEntityReactorControl.establishLink()}.
 */
public class ItemReactorSensor extends Item {

    public static final String NBT_X = "x";
    public static final String NBT_Y = "y";
    public static final String NBT_Z = "z";

    public ItemReactorSensor(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        BlockPos core = resolveReactorCore(level, pos);
        if (core == null) {
            return InteractionResult.PASS;
        }

        net.minecraft.world.entity.player.Player player = context.getPlayer();

        if (!level.isClientSide()) {
            if (player != null) {
                // Original: "[Reaktorfuehler] Position set!" (Name/Klammern dunkeltuerkis, Text gruen)
                player.sendSystemMessage(Component.literal("[").withStyle(ChatFormatting.DARK_AQUA)
                        .append(Component.translatable(this.getDescriptionId()).withStyle(ChatFormatting.DARK_AQUA))
                        .append(Component.literal("] ").withStyle(ChatFormatting.DARK_AQUA))
                        .append(Component.literal("Position set!").withStyle(ChatFormatting.GREEN)));
            }

            CompoundTag tag = StackNbt.orCreate(context.getItemInHand());
            tag.putInt(NBT_X, core.getX());
            tag.putInt(NBT_Y, core.getY());
            tag.putInt(NBT_Z, core.getZ());

            if (player != null)
                level.playSound(null, player.getX(), player.getY(), player.getZ(), com.hbm_m.sound.ModSounds.TOOL_TECH_BOOP.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
        }

        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /**
     * Loest die Kernposition eines Forschungsreaktors auf - auch dann, wenn auf ein Bauteil des
     * Turms geklickt wurde. Das entspricht dem {@code findCore(...)} des Originals.
     */
    @Nullable
    public static BlockPos resolveReactorCore(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);

        if (be instanceof MachineReactorResearchBlockEntity) {
            return pos.immutable();
        }

        if (be instanceof UniversalMachinePartBlockEntity part) {
            BlockPos controller = part.getControllerPos();
            if (controller != null
                    && level.getBlockEntity(controller) instanceof MachineReactorResearchBlockEntity) {
                return controller.immutable();
            }
        }

        return null;
    }

    /** Gebundene Position, oder {@code null}, wenn der Fuehler noch nicht gesetzt wurde. */
    @Nullable
    public static BlockPos getBoundPos(ItemStack stack) {
        CompoundTag tag = StackNbt.read(stack);
        if (tag == null || !tag.contains(NBT_X)) return null;
        return new BlockPos(tag.getInt(NBT_X), tag.getInt(NBT_Y), tag.getInt(NBT_Z));
    }

    @Override
    //? if < 1.21.1 {
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
    //?} else {
    /*public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext hbmTooltipCtx, List<Component> tooltip, TooltipFlag flag) {
    *///?}
        BlockPos bound = getBoundPos(stack);

        // 1:1 Original ItemReactorSensor.addInformation: drei graue Koordinatenzeilen bzw. Hinweis
        if (bound != null) {
            tooltip.add(Component.literal("x: " + bound.getX()).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("y: " + bound.getY()).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("z: " + bound.getZ()).withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.literal("No reactor selected!").withStyle(ChatFormatting.GRAY));
        }
    }
}
