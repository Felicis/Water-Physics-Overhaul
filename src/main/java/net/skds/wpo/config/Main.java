package net.skds.wpo.config;

import java.util.function.Function;

import net.minecraftforge.common.ForgeConfigSpec;
import net.skds.wpo.WPO;
import net.skds.wpo.util.Constants;

public class Main {

    //public final ForgeConfigSpec.BooleanValue slide;
    public final ForgeConfigSpec.IntValue maxSlideDist, maxEqDist, maxBucketDist, maxDisplaceDist;
    public final ForgeConfigSpec.DoubleValue fluidTickRateScaling;

    // public final ForgeConfigSpec.ConfigValue<ArrayList<String>> ss;
    // private final ForgeConfigSpec.IntValue maxFluidLevel;

    public Main(ForgeConfigSpec.Builder innerBuilder) {
        Function<String, ForgeConfigSpec.Builder> builder = name -> innerBuilder.translation(WPO.MOD_ID + ".config." + name);

        innerBuilder.push("General");

        // slide = builder.apply("setSlide").comment("Will fluids slide down from hills").define("setSlide", true);
        maxEqDist = builder.apply("setMaxEqualizeDistance").comment("UwU").defineInRange("setMaxEqualizeDistance", 16, 0, 256);
        maxSlideDist = builder.apply("setMaxSlidingDistance").comment("-_-").defineInRange("setMaxSlidingDistance", 5, 0, 256);
        maxBucketDist = builder.apply("setMaxBucketDistance").comment("^u^").defineInRange("setMaxBucketDistance", 8, 0, Constants.MAX_FLUID_LEVEL);
        maxDisplaceDist = builder.apply("setMaxDisplaceDistance")
                .comment("maximum distance over which fluid will be pushed by block placed/falling and pistons")
                .defineInRange("setMaxDisplaceDistance", 12, 0, 256);
        fluidTickRateScaling = builder.apply("setFluidTickRateScaling")
                .comment("vanilla tick rate is scaled by this factor (vanilla: water=5, lava=30 (lava=10 in nether)")
                .defineInRange("setFluidTickRateScaling", 0.5, 0, 2);
        innerBuilder.pop();
    }
}