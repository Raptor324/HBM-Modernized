package com.hbm_m.client.render.implementations;

import com.hbm_m.blockentity.machines.MobileLaunchPadBlockEntity;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Zeichnet die geladene Rakete im Startrahmen der mobilen Rampe.
 *
 * <p>Im Modell des TEL sitzt an dieser Stelle nichts mehr - die aufgerichtete Rakete ist beim
 * Konvertieren herausgeschnitten worden, damit man einer geladenen Rampe ansieht, was drin steckt.
 */
//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public class MobileLaunchPadMissileRenderer extends LaunchPadMissileRenderer {

    public MobileLaunchPadMissileRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    /**
     * Der Fusspunkt wandert zwischen Ladewanne und Startschuh: liegend ruht die Rakete hoeher als
     * aufgerichtet, weil ihr Heck beim Aufrichten in den Rahmen sinkt.
     */
    @Override
    protected Vec3 missileOffset(com.hbm_m.blockentity.machines.LaunchPadBaseBlockEntity be, float partialTicks) {
        double height = MobileLaunchPadBlockEntity.ERECTOR_REST;
        if (be instanceof MobileLaunchPadBlockEntity pad) {
            height = Mth.lerp(pad.getErectorProgress(partialTicks),
                    MobileLaunchPadBlockEntity.ERECTOR_REST, MobileLaunchPadBlockEntity.ERECTOR_BASE);
        }
        return new Vec3(0.0D, height, -MobileLaunchPadBlockEntity.ERECTOR_FORWARD);
    }

    /**
     * Eingefahren liegt die Rakete waagerecht in der Ladewanne und zeigt mit der Spitze zur
     * Kabine, aufgerichtet steht sie senkrecht. Dazwischen wird linear gedreht - der Fortschritt
     * kommt aus der BlockEntity und ist dort schon zwischen zwei Ticks interpoliert.
     */
    @Override
    protected float missilePitch(com.hbm_m.blockentity.machines.LaunchPadBaseBlockEntity be, float partialTicks) {
        if (be instanceof MobileLaunchPadBlockEntity pad) {
            return (1.0F - pad.getErectorProgress(partialTicks)) * 90.0F;
        }
        return 0.0F;
    }
}
