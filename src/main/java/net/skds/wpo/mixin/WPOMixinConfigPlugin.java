//package net.skds.wpo.mixin;
//
//import org.objectweb.asm.tree.ClassNode;
//import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
//import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
//import virtuoel.statement.util.VersionUtils;
//
//import java.util.List;
//import java.util.Set;
//
//public class WPOMixinConfigPlugin implements IMixinConfigPlugin {
//    // from virtuoel:towelette
//    private static final String MIXIN_PACKAGE = "net.skds.wpo.mixin";
//
//    @Override
//    public void onLoad(String mixinPackage)
//    {
//        if (!mixinPackage.startsWith(MIXIN_PACKAGE))
//        {
//            throw new IllegalArgumentException(
//                    String.format("Invalid package: Expected \"%s\", but found \"%s\".", MIXIN_PACKAGE, mixinPackage)
//            );
//        }
//    }
//
//    @Override
//    public String getRefMapperConfig()
//    {
//        return null;
//    }
//
//    @Override
//    public boolean shouldApplyMixin(String targetClassName, String mixinClassName)
//    {
//        if (!mixinClassName.startsWith(MIXIN_PACKAGE))
//        {
//            throw new IllegalArgumentException(
//                    String.format("Invalid package for class \"%s\": Expected \"%s\", but found \"%s\".", targetClassName, MIXIN_PACKAGE, mixinClassName)
//            );
//        }
//
//        if (!VersionUtils.shouldApplyCompatibilityMixin(mixinClassName))
//        {
//            return false;
//        }
//
//        return true;
//    }
//
//    @Override
//    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets)
//    {
//
//    }
//
//    @Override
//    public List<String> getMixins()
//    {
//        return null;
//    }
//
//    @Override
//    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo)
//    {
////        if (mixinClassName.equals(MIXIN_PACKAGE + ".towelette.compat.ToweletteConfigMixin"))
////        {
////            BackwardsCompatibility.addFieldsIfNeeded(targetClass);
////        }
//    }
//
//    @Override
//    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo)
//    {
//
//    }
//}
