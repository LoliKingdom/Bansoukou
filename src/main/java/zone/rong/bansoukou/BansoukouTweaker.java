package zone.rong.bansoukou;

import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraftforge.fml.exit.QualifiedExit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.RollingRandomAccessFileAppender;
import org.apache.logging.log4j.core.appender.rolling.RollingRandomAccessFileManager;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.JarFile;

public class BansoukouTweaker implements ITweaker {

    public static final Logger LOGGER = LogManager.getLogger("Bansoukou");

    private static final boolean debug = true;

    private static ArrayList loaderList;
    private static Field loaderField;

    // Special thanks to http://management-platform.blogspot.com/2009/01/classloaders-keeping-jar-files-open.html
    // This is a debugging method
    // It checks how many jars are loaded in the URLClassPath loaders before Bansoukou does any work
    // Any jar loaded here means they have already been acted on in some way
    // E.g. class loaded or ready to have its classes searched and loaded (coremods)
    // If there are any coremods, we close the JarFile and set the field to null
    // This allows the ClassLoader to open a new JarFile to read from
    // This has to be done as ZipInputStream is an oneway operation (can't read bytes reversely)
    static void openedJars() {
        try {
            Class<URLClassLoader> clazz = URLClassLoader.class;
            Field ucp = clazz.getDeclaredField("ucp");
            ucp.setAccessible(true);
            Object urlClassPath = ucp.get(Launch.classLoader);
            Field loaders = urlClassPath.getClass().getDeclaredField("loaders");
            loaders.setAccessible(true);
            loaderList = (ArrayList) loaders.get(urlClassPath);
            if (debug) {
                for (Object jarLoader : loaderList) {
                    try {
                        if (loaderField == null) {
                            loaderField = jarLoader.getClass().getDeclaredField("jar");
                            loaderField.setAccessible(true);
                        }
                        Object jarFile = loaderField.get(jarLoader);
                        LOGGER.warn(((JarFile) jarFile).getName());
                    } catch (Throwable ignored) { }
                }
            }
        } catch (Throwable ignored) { }
    }

    public BansoukouTweaker() throws IOException {
        LOGGER.info("Ikimasu!");
        /*
        openedJars();
        try {
            for (Object jarLoader : loaderList) {
                try {
                    JarFile jarFile = (JarFile) loaderField.get(jarLoader);
                    if (jarFile.getName().endsWith("EntityCulling-1.12.2-4.1.5.jar")) {
                        Path currentPath = Paths.get(jarFile.getName());
                        Path copyPath = Paths.get(currentPath.getParent().toString(), "EntityCulling-1.12.2-4.1.5.bansoukou");
                        jarFile.close();
                        loaderField.set(jarLoader, null);
                        Files.copy(currentPath, copyPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (Throwable ignored) { }
            }
        } catch (Exception ignored) { }
         */
    }

    /**
     * The fun begins.
     */
    @Override
    public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
        if (args.get(args.size() - 3).equals("--bansoukou")) {
            return;
        }
        String classPath = getClassPaths();
        String javaPath = System.getProperty("java.home");
        if (!javaPath.endsWith("bin")) {
            javaPath = javaPath + File.separatorChar + "bin";
        }
        javaPath = javaPath + File.separatorChar + (System.console() == null ? "javaw.exe" : "java.exe");
        String existingCommands = System.getProperty("sun.java.command");
        List<String> commands = new ArrayList<>();
        commands.add(javaPath); // Start of the command - invokes the java install that is running the current instance
        commands.add("-cp");
        commands.add('"' + classPath + '"'); // Add in existing classpaths (TODO: configurable)
        commands.addAll(ManagementFactory.getRuntimeMXBean().getInputArguments()); // Any java args (TODO: configurable)
        commands.add("zone.rong.bansoukou.BansoukouStart"); // Main class (TODO: configurable)
        commands.addAll(args); // Some leftover arguments that isn't parsed
        commands.add("--version"); // Versioning
        commands.add(profile);
        commands.add("--assetsDir"); // Assets directory
        commands.add(assetsDir.toString());

        commands.add("--tweakClass");
        commands.add("net.minecraftforge.fml.common.launcher.FMLTweaker"); // Main (first) Tweaker (TODO: configurable)

        /*
        for (String keys : ((List<String>) Launch.blackboard.getOrDefault("TweakClasses", Collections.emptyList()))) {
            if (keys.equals("net.minecraftforge.fml.common.launcher.FMLInjectionAndSortingTweaker")) {
                continue;
            }
            commands.add("--tweakClass"); // Add any tweak classes, preferably in order
            commands.add(keys);
        }
         */

        commands.add(existingCommands); // Any program arguments, mostly for dev env

        commands.add("--bansoukou"); // Add unique arg to tell Bansoukou its a Bansoukou-spawned process

        commands.add("--parentpid");
        commands.add(ManagementFactory.getRuntimeMXBean().getName().split("@")[0]); // Cries in Java 9

        File userDir = new File(System.getProperty("user.dir"));
        ProcessBuilder builder = new ProcessBuilder()
                .directory(userDir)
                .command(commands)
                .inheritIO();
        try {
            org.apache.logging.log4j.core.Logger loggerImpl = (org.apache.logging.log4j.core.Logger) LOGGER;
            RollingRandomAccessFileAppender appender = (RollingRandomAccessFileAppender) loggerImpl.getAppenders().get("File");
            RollingRandomAccessFileManager manager = appender.getManager();
            manager.closeOutputStream();
            appender = (RollingRandomAccessFileAppender) loggerImpl.getAppenders().get("DebugFile");
            manager = appender.getManager();
            manager.closeOutputStream();
            LogManager.shutdown();
            Files.delete(Paths.get(Launch.minecraftHome.toString(), "logs", "latest.log"));
            Files.delete(Paths.get(Launch.minecraftHome.toString(), "logs", "debug.log"));
            LOGGER.info("Reloading loggers...");
            Process process = builder.start();
            process.waitFor(); // TODO: try to free memory as much as possible in the current process
        } catch (Exception e) {
            e.printStackTrace();
        }
        QualifiedExit.exit(0);
    }

    @Override
    public void injectIntoClassLoader(LaunchClassLoader classLoader) {

    }

    @Override
    public String getLaunchTarget() {
        return null;
    }

    @Override
    public String[] getLaunchArguments() {
        return new String[0];
    }

    private String getClassPaths() {
        String CP = ManagementFactory.getRuntimeMXBean().getClassPath();
        CP += ";" + Launch.minecraftHome + File.separator + "mods" + File.separator + "." + File.separator + "bansoukou-3.0.jar"; // TODO
        return CP;
    }
}
