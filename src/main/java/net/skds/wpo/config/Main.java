package net.skds.wpo.config;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import net.minecraft.block.Block;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.registries.ForgeRegistries;
import net.skds.wpo.WPO;
import net.skds.wpo.fluidphysics.Constants;

public class Main {
    //public final ForgeConfigSpec.BooleanValue slide;
    public final ForgeConfigSpec.IntValue maxSlideDist, maxEqDist, maxBucketDist, maxDisplaceDist;
    public final ForgeConfigSpec.DoubleValue fluidTickRateScaling;

    public final ForgeConfigSpec.BooleanValue vanillaFluidlogging, vanillaFluidMixing, useCustomLists;
    private final ForgeConfigSpec.ConfigValue<String> fluidloggableBlockListString, notFluidloggableBlockListString,
            destroyedByFluidsBlockListString, notDestroyedByFluidsBlockListString;
    public List<Block> fluidloggableBlockList, notFluidloggableBlockList, destroyedByFluidsBlockList, notDestroyedByFluidsBlockList;

    public int getMaxDist() {
//        return Integer.max(Integer.max(maxSlideDist.get(), maxEqDist.get()), Integer.max(maxBucketDist.get(), maxDisplaceDist.get()));
        // not using displace (block place/fall + piston), because they immediately invalidate graph
        // for displace compute graph on the fly to reduce cache usage
        return Integer.max(Integer.max(maxSlideDist.get(), maxEqDist.get()), maxBucketDist.get());
    }

    // public final ForgeConfigSpec.ConfigValue<ArrayList<String>> ss;
    // private final ForgeConfigSpec.IntValue maxFluidLevel;

    public Main(ForgeConfigSpec.Builder innerBuilder) {
        Function<String, ForgeConfigSpec.Builder> builder = name -> innerBuilder.translation(WPO.MOD_ID + ".config." + name);

        innerBuilder.push("General");
        // slide = builder.apply("setSlide").comment("Will fluids slide down from hills").define("setSlide", true);
        maxEqDist = builder.apply("setMaxEqualizeDistance").comment("UwU").defineInRange("setMaxEqualizeDistance", 2, 0, 256);
        maxSlideDist = builder.apply("setMaxSlidingDistance").comment("-_-").defineInRange("setMaxSlidingDistance", 5, 0, 256);
        maxBucketDist = builder.apply("setMaxBucketDistance").comment("^u^").defineInRange("setMaxBucketDistance", 8, 0, Constants.MAX_FLUID_LEVEL);
        maxDisplaceDist = builder.apply("setMaxDisplaceDistance")
                .comment("maximum distance over which fluid will be pushed by block placed/falling and pistons")
                .defineInRange("setMaxDisplaceDistance", 12, 0, 256);
        fluidTickRateScaling = builder.apply("setFluidTickRateScaling")
                .comment("vanilla tick rate is scaled by this factor (vanilla: water=5, lava=30 (lava=10 in nether)")
                .defineInRange("setFluidTickRateScaling", 0.5, 0, 2);
        innerBuilder.pop();

        innerBuilder.push("Default fluidlogging and fluid mixing behaviour");
        vanillaFluidlogging = builder.apply("vanillaFluidlogging").comment("true: vanilla behaviour, false: overhauled behaviour")
                .define("vanillaFluidlogging", true);
        vanillaFluidMixing = builder.apply("vanillaFluidMixing").comment("true: vanilla behaviour, false: overhauled behaviour")
                .define("vanillaFluidMixing", true);
        innerBuilder.pop();

        innerBuilder.push("Custom behaviour (overrides default behaviour if active. also are applied in order and can override each other)");
        useCustomLists = builder.apply("useCustomLists").comment("true: custom lists override default behaviour, false: custom lists ignored")
                .define("useCustomLists", true);
        // fluidloggable/destroyed by fluids whitelist & blacklist (overrides internal settings)
        fluidloggableBlockListString = builder.apply("fluidloggableBlocksList")
                .comment("List of all block registry names, that should be fluidloggable (whitelist)")
                .define("fluidloggableBlocksList", "");
        notFluidloggableBlockListString = builder.apply("notFluidloggable")
                .comment("List of all block registry names, that should NOT be fluidloggable (blacklist, overrides whitelist)")
                .define("notFluidloggable", "");
        destroyedByFluidsBlockListString = builder.apply("destroyedByFluids")
                .comment("List of all block registry names, that should be destroyed by fluids (whitelist)")
                .define("destroyedByFluids", "");
        notDestroyedByFluidsBlockListString = builder.apply("notDestroyedByFluids")
                .comment("List of all block registry names, that should NOT be destroyed by fluids (blacklist, overrides whitelist)")
                .define("notDestroyedByFluids", "");
        innerBuilder.pop();
    }

    public void cacheBlockListConfigs() {
        // TODO use hashsets?
        fluidloggableBlockList = parseList(fluidloggableBlockListString);
        notFluidloggableBlockList = parseList(notFluidloggableBlockListString);
        destroyedByFluidsBlockList = parseList(destroyedByFluidsBlockListString);
        notDestroyedByFluidsBlockList = parseList(notDestroyedByFluidsBlockListString);
    }

    private static List<Block> parseList(ForgeConfigSpec.ConfigValue<String> blockListString) {
        ArrayList<Block> list = new ArrayList<>();
        // remove all whitespace and quotes (") and split on comma
        // if string empty "", adds minecraft:air (!)
        for (String s : blockListString.get().replaceAll("\\s","").replace("\"", "").split(",")) {
            try {
                list.add(ForgeRegistries.BLOCKS.getValue(new ResourceLocation(s)));
            } catch (Exception e) {
                // skip invalid config & show error
                WPO.LOGGER.error("CONFIG ERROR: block \"" + s + "\" in config \"" + blockListString.getPath() + "\" could not be found! Skipping block.", e);
            }
        }
        return list;
    }
}