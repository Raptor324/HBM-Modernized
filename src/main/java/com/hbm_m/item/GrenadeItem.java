package com.hbm_m.item;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import java.util.List;

import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.grenades.GrenadeProjectileEntity;
import com.hbm_m.entity.grenades.GrenadeType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;

public class GrenadeItem extends Item {
    
    private final GrenadeType grenadeType;
    private final RegistryObject<EntityType<GrenadeProjectileEntity>> entityType;

    public GrenadeItem(Properties properties, GrenadeType grenadeType, RegistryObject<EntityType<GrenadeProjectileEntity>> entityType) {
        super(properties);
        this.grenadeType = grenadeType;
        this.entityType = entityType;
    }
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        //  Универсальная подсказка на основе типа гранаты
        String behaviorText;

        switch (grenadeType) {
            case SMART -> behaviorText = "Детонирует при прямом попадании в сущность, запас отскоков: 3";
            case FIRE -> behaviorText = "Распространяет огонь после детонации, запас отскоков: 3";
            case SLIME -> behaviorText = "Теряет меньше скорости при контакте с поверхностями, запас отскоков: 4";
            case STANDARD -> behaviorText = "Слабый взрыв, запас отскоков: 3";
            case HE -> behaviorText = "Стандартный взрыв, запас отскоков: 3";
            default -> behaviorText = "Кидайте и взрывайте!";
        }

        // БЕЗ пробела в начале и пустых строк
        tooltip.add(Component.literal(behaviorText)
                .withStyle(ChatFormatting.YELLOW));

        //  ОТДЕЛЬНАЯ строка снизу
        tooltip.add(Component.literal("Детонирует после последнего отскока")
                .withStyle(ChatFormatting.GRAY));
    }


    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.SNOWBALL_THROW, SoundSource.NEUTRAL, 
            0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        
        if (!level.isClientSide) {
            GrenadeProjectileEntity grenade = new GrenadeProjectileEntity(
                entityType.get(), level, player, grenadeType
            );
            grenade.setItem(itemstack);
            grenade.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 1.0F);
            level.addFreshEntity(grenade);
        }
        
        player.awardStat(Stats.ITEM_USED.get(this));
        if (!player.getAbilities().instabuild) {
            itemstack.shrink(1);
        }
        
        return InteractionResultHolder.sidedSuccess(itemstack, level.isClientSide());
    }
}
