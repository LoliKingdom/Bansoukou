package zone.rong.bansoukou;

import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraftforge.fml.common.launcher.FMLTweaker;
import net.minecraftforge.fml.relauncher.CoreModManager;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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

    private static void rerunModLoading() {
        try {
            Method discoverCoreMods = CoreModManager.class.getDeclaredMethod("discoverCoreMods", File.class, LaunchClassLoader.class);
            discoverCoreMods.setAccessible(true);
            discoverCoreMods.invoke(null, Bansoukou.HOME, Launch.classLoader);
        } catch (Throwable t) {
            throw new RuntimeException("Unable to load mods with Bansoukou patches...", t);
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
            rerunModLoading();
        }
        this.tweaker.injectCascadingTweak(tweakClassName);
    }

}
