package zone.rong.bansoukou;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

public class BansoukouTweaker implements ITweaker {

    @Override
    public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
        final OptionParser parser = new OptionParser();
        parser.allowsUnrecognizedOptions();
        final OptionSpec<String> bansoukouFlag = parser.accepts("bansoukou", "Bansoukou").withOptionalArg();
        final OptionSet argsSet = parser.parse(args.toArray(new String[0]));
        if (argsSet.has(bansoukouFlag)) {
            return;
        }
        String classPath = ManagementFactory.getRuntimeMXBean().getClassPath();
        String javaPath = System.getProperty("java.home");
        if (!javaPath.endsWith("bin")) {
            javaPath = javaPath + File.separatorChar + "bin";
        }
        // String existingCommands = System.getProperty("sun.java.command");
        List<String> commands = new ArrayList<>();
        commands.add(javaPath); // Start of the command - invokes the java install that is running the current instance
        commands.add("-cp");
        commands.add('"' + classPath + '"'); // Add in existing classpaths (TODO: configurable)
        commands.addAll(ManagementFactory.getRuntimeMXBean().getInputArguments()); // Any java args (TODO: configurable)
        commands.add("zone.rong.bansoukou.BansoukouMain"); // Main class (TODO: configurable)
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
        commands.add(System.getProperty("sun.java.command")); // Any program arguments, mostly for dev env
        commands.add("--bansoukou"); // Add unique arg to tell Bansoukou its a Bansoukou-spawned process
        commands.add("--parentpid");
        commands.add(ManagementFactory.getRuntimeMXBean().getName().split("@")[0]); // Cries in Java 9
        File userDir = new File(System.getProperty("user.dir"));
        ProcessBuilder builder = new ProcessBuilder()
                .directory(userDir)
                .command(commands)
                .inheritIO();
        // builder.start();
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

}
