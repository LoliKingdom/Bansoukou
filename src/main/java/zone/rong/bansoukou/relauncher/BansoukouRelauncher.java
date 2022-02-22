package zone.rong.bansoukou.relauncher;

import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.fml.common.launcher.FMLTweaker;
import org.apache.commons.io.FileUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

public class BansoukouRelauncher {

    public static void run() {
        try {
            relieve();
            spawn();
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }

    private static void relieve() throws ReflectiveOperationException {
        System.out.println("Prior to relieving: " + FileUtils.byteCountToDisplaySize(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
        Field field;
        Field[] fields;
        // Relieving FMLTweaker
        fields = FMLTweaker.class.getDeclaredFields();
        FMLTweaker tweaker = (FMLTweaker) ((List<ITweaker>) Launch.blackboard.get("Tweaks")).get(0);
        for (int i = 0; i < fields.length; i++) {
            field = fields[i];
            field.setAccessible(true);
            if (Modifier.isStatic(field.getModifiers())) {
                field.set(null, null);
            } else {
                field.set(tweaker, null);
            }
        }
        // Would not need to relieve CoremodTweaker.class since that is a dev environment only bootstrapper
        // Relieving Launch.class
        // Not going to relieve Launch.DEFAULT_TWEAK since it is a literal String
        Launch.minecraftHome = null;
        Launch.classLoader = null;
        Launch.blackboard = null;
        // Relieving LaunchClassLoader
        Thread.currentThread().setContextClassLoader(null);
        // Force GC
        System.gc();
        System.out.println("After relieving: " + FileUtils.byteCountToDisplaySize(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
    }

    private static void spawn() {

    }

}
