package com.hbm_m.client.loader.dae;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.IOException;
import java.io.InputStream;
//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}

/** Opens resources from the client resource manager. */
@OnlyIn(Dist.CLIENT)
public final class MinecraftResourceResolver {

    private MinecraftResourceResolver() { }

    public static InputStream open(ResourceLocation resource) throws IOException {
        try {
            Resource res = Minecraft.getInstance().getResourceManager().getResourceOrThrow(resource);
            return res.open();
        } catch(IOException e) {
            ResourceLocation daePath = resource.withSuffix(".dae");
            if(!daePath.equals(resource)) {
                return Minecraft.getInstance().getResourceManager().getResourceOrThrow(daePath).open();
            }
            throw e;
        }
    }
}
