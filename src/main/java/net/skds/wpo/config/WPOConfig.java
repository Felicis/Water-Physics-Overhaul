package net.skds.wpo.config;

import java.nio.file.Paths;

import net.minecraftforge.fml.config.ModConfig;
import net.skds.wpo.WPO;
import org.apache.commons.lang3.tuple.Pair;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;

public class WPOConfig {

//    public static final ClientConfig CLIENT;
//    private static final ForgeConfigSpec CLIENT_SPEC;
//    public static final CommonConfig COMMON;
//    private static final ForgeConfigSpec COMMON_SPEC;
    public static final Main SERVER;
    private static final ForgeConfigSpec SERVER_SPEC;

    static {
//        Pair<MainConfig, ForgeConfigSpec> cl = new ForgeConfigSpec.Builder().configure(ClientConfig::new);
//        CLIENT = cl.getLeft();
//        CLIENT_SPEC = cl.getRight();

//        Pair<MainConfig, ForgeConfigSpec> cm = new ForgeConfigSpec.Builder().configure(MainConfig::new);
//        COMMON = cm.getLeft();
//        COMMON_SPEC = cm.getRight();

        Pair<Main, ForgeConfigSpec> sv = new ForgeConfigSpec.Builder().configure(Main::new);
        SERVER = sv.getLeft();
        SERVER_SPEC = sv.getRight();
    }

    public static void init() {
//        Paths.get(System.getProperty("user.dir"), "config", WPO.MOD_ID).toFile().mkdir();
//        ModLoadingContext.get().registerConfig(Type.CLIENT, CLIENT_SPEC, WPO.MOD_ID + "/client.toml");
//        ModLoadingContext.get().registerConfig(Type.COMMON, COMMON_SPEC, WPO.MOD_ID + "/common.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, SERVER_SPEC, WPO.MOD_ID + "/server.toml");
    }
}