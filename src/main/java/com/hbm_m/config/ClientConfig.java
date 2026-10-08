package com.hbm_m.config;

/**
 * 1:1 {@code com.hbm.config.ClientConfig} (hbmClient.json), soweit nicht schon in {@link ModClothConfig}
 * (GUN_*, RENDER_REEDS, RENDER_REBAR_*, TOOL_HUD_INDICATOR_*). Statisch an {@code ConfigSchema} gebunden,
 * landet also in client.json und im Konfig-Menue. NEI_HIDE_SECRETS und SHOW_BLOCK_META_OVERLAY entfallen.
 */
public final class ClientConfig {

    private ClientConfig() { }

    public static int geigerOffsetHorizontal = 0;
    public static int geigerOffsetVertical = 0;
    public static int infoOffsetHorizontal = 0;
    public static int infoOffsetVertical = 0;
    /** 0 = oben links, 1 = oben rechts, 2 = rechts vom Fadenkreuz, 3 = links vom Fadenkreuz. */
    public static int infoPosition = 0;
    public static boolean itemTooltipShowOredict = true;
    /** Custom-Nuke-Tooltip (TileEntityNukeCustom.entries), siehe ClientModEvents.handleItemTooltip. */
    public static boolean itemTooltipShowCustomNuke = true;
    public static boolean mainMenuWackySplashes = true;
    /** DODD-RBMK-Diagnose; sperrt wie im Original den ganzen Look-Overlay-Hook (BlockLookOverlayHud, RBMKDiagnosticOverlay). */
    public static boolean doddRbmkDiagnostic = true;
    public static boolean renderCableHang = true;
    public static boolean nukeHudFlash = true;
    public static boolean nukeHudShake = true;
    public static boolean coolingTowerParticles = true;
    public static int renderHeliostatBeamLimit = 250;
    public static boolean badgesHud = true;
}
