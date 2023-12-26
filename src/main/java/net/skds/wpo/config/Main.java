package net.skds.wpo.config;

import java.util.function.Function;
import net.minecraftforge.common.ForgeConfigSpec;

public class Main {
    public final ForgeConfigSpec.IntValue maxSlideDist;
    public final ForgeConfigSpec.IntValue maxEqDist;
    public final ForgeConfigSpec.IntValue maxBucketDist;
    public final ForgeConfigSpec.IntValue setMaxWaterY;

    public Main(ForgeConfigSpec.Builder innerBuilder) {
        Function<String, ForgeConfigSpec.Builder> builder = (name) -> {
            return innerBuilder.translation("wpo.config." + name);
        };
        innerBuilder.push("General");
        this.setMaxWaterY = ((ForgeConfigSpec.Builder)builder.apply("setMaxWaterY")).comment("Set how much water cooling will disappear after").defineInRange("setMaxWaterY",0, Integer.MIN_VALUE,Integer.MAX_VALUE);
        this.maxEqDist = ((ForgeConfigSpec.Builder)builder.apply("setMaxEqualizeDistance")).comment("the distance over which water levels will equalize").defineInRange("setMaxEqualizeDistance", 16, 0, 256);
        this.maxSlideDist = ((ForgeConfigSpec.Builder)builder.apply("setMaxSlidingDistance")).comment("the maximum distance water will slide to reach lower ground").defineInRange("setMaxSlidingDistance", 5, 0, 256);
        this.maxBucketDist = ((ForgeConfigSpec.Builder)builder.apply("setMaxBucketDistance")).comment("Maximum horizontal bucket reach from click location (for water packet pickup)").defineInRange("setMaxBucketDistance", 8, 0, 8);
        innerBuilder.pop();
    }
}
