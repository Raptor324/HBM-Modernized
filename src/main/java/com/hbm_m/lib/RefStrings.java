package com.hbm_m.lib;

import net.minecraft.resources.ResourceLocation;

public class RefStrings {
	public static final String MODID = "hbm_m";
	public static final String NAME = "Hbm's Nuclear Tech Mod Modernized";
	//HBM's Beta Naming Convention:
	//V T (X)
	//V -> next release version
	//T -> build type
	//X -> days since 10/10/10
	public static final String CLIENTSIDE = "com.hbm_m.main.ClientProxy";
	public static final String SERVERSIDE = "com.hbm_m.main.ServerProxy";
	
	public static ResourceLocation resourceLocation(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
	}
}
