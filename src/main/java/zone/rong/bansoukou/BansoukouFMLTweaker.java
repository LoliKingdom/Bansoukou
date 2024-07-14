package zone.rong.bansoukou;

import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraftforge.fml.common.launcher.FMLTweaker;
import net.minecraftforge.fml.relauncher.CoreModManager;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;

public class BansoukouFMLTweaker extends FMLTweaker {

    static void replace() {
        try {
            Field coreModManager$tweaker = CoreModManager.class.getDeclaredField("tweaker");
            coreModManager$tweaker.setAccessible(true);

            coreModManager$tweaker.set(null, new BansoukouFMLTweaker((FMLTweaker) coreModManager$tweaker.get(null)));
        } catch (Throwable t) {
            throw new RuntimeException("Cannot replace FMLTweaker instance", t);
        }
    }

    private final FMLTweaker tweaker;

    private boolean ran = false;

    BansoukouFMLTweaker(FMLTweaker tweaker) {
        super(); // Runs setSecurityManager once again
        this.tweaker = tweaker;
    }

    @Override
    public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
        this.tweaker.acceptOptions(args, gameDir, assetsDir, profile);
    }

    @Override
    public void injectIntoClassLoader(LaunchClassLoader classLoader) {
        this.tweaker.injectIntoClassLoader(classLoader);
    }

    @Override
    public String getLaunchTarget() {
        return this.tweaker.getLaunchTarget();
    }

    @Override
    public String[] getLaunchArguments() {
        return this.tweaker.getLaunchArguments();
    }

    @Override
    public File getGameDir() {
        return this.tweaker.getGameDir();
    }

    @Override
    public void injectCascadingTweak(String tweakClassName) {
        if (!this.ran && "net.minecraftforge.fml.common.launcher.FMLDeobfTweaker".equals(tweakClassName)) {
            this.ran = true;
            // Re-run discoverCoreMods
            Bansoukou.rerunModLoading();
        }
        this.tweaker.injectCascadingTweak(tweakClassName);
    }

}
