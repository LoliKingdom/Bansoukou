package zone.rong.bansoukou;

import net.minecraft.launchwrapper.IClassTransformer;

/**
 * @author youyihj
 */
public class BansoukouTransformer implements IClassTransformer {

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        return BansoukouCoreMod.PATCHED_CLASSES.getOrDefault(transformedName, basicClass);
    }
}
