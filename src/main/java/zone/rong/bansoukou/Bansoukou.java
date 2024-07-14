package zone.rong.bansoukou;

import com.google.common.eventbus.EventBus;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraftforge.fml.common.DummyModContainer;
import net.minecraftforge.fml.common.LoadController;
import net.minecraftforge.fml.common.ModMetadata;
import net.minecraftforge.fml.relauncher.CoreModManager;
import net.minecraftforge.fml.relauncher.FMLSecurityManager;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sun.misc.Unsafe;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@IFMLLoadingPlugin.Name(Tags.MOD_NAME)
public class Bansoukou implements IFMLLoadingPlugin {

    public static final Path BANSOUKOU_DIRECTORY = Launch.minecraftHome.toPath().resolve(Tags.MOD_ID);
    public static final Path CACHE_BANSOUKOU_DIRECTORY = Launch.minecraftHome.toPath().resolve("cache/" + Tags.MOD_ID);

    static final Unsafe UNSAFE = unsafe();
    static final Logger LOGGER = LogManager.getLogger(Tags.MOD_NAME);
    static final Map<Path, Path> MOD_TO_PATCH = new HashMap<>();

    private static final Path MOD_DIRECTORY = Launch.minecraftHome.toPath().resolve("mods");

    static void rerunModLoading() {
        try {
            Method discoverCoreMods = CoreModManager.class.getDeclaredMethod("discoverCoreMods", File.class, LaunchClassLoader.class);
            discoverCoreMods.setAccessible(true);
            discoverCoreMods.invoke(null, Launch.minecraftHome, Launch.classLoader);
        } catch (Throwable t) {
            throw new RuntimeException("Unable to load mods with Bansoukou patches...", t);
        }
    }

    private static Unsafe unsafe() {
        try {
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            return (Unsafe) unsafeField.get(null);
        } catch (Exception e) {
            throw new RuntimeException("Exception occurred while getting Unsafe instance", e);
        }
    }

    private static boolean needsPatching(Path patchJar, Path cacheJar) throws IOException {
        if (!Files.exists(cacheJar)) {
            return true;
        }
        return Files.getLastModifiedTime(patchJar).compareTo(Files.getLastModifiedTime(cacheJar)) > 0;
    }

    private static void patchJar(Path originalJar, Path patchJar, Path cacheJar) throws IOException {
        Files.copy(originalJar, cacheJar, StandardCopyOption.REPLACE_EXISTING);

        try (ZipFile patchZipFile = new ZipFile(patchJar.toFile());
             FileSystem jarFs = FileSystems.newFileSystem(cacheJar, null)) {
            Enumeration<? extends ZipEntry> entries = patchZipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory()) {
                    Path targetPath = jarFs.getPath(entry.getName());
                    if (targetPath.getParent() != null) {
                        Files.createDirectories(targetPath.getParent());
                    }
                    Files.copy(patchZipFile.getInputStream(entry), targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    public Bansoukou() {
        if (!Files.exists(BANSOUKOU_DIRECTORY) || !Files.isDirectory(BANSOUKOU_DIRECTORY)) {
            LOGGER.warn("Bansoukou folder not found, skipping.");
            return;
        }

        try {
            Files.createDirectories(CACHE_BANSOUKOU_DIRECTORY);
        } catch (IOException e) {
            throw new RuntimeException("Unable to create Bansoukou's cache directory!", e);
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(BANSOUKOU_DIRECTORY, "{*.jar,*.zip}")) {
            for (Path patchFile : stream) {
                Path relativePath = BANSOUKOU_DIRECTORY.relativize(patchFile);
                String patchName = patchFile.getFileName().toString();
                String jarName = patchName.substring(0, patchName.length() - 4) + ".jar";
                Path jarNamePath = relativePath.resolveSibling(jarName);
                Path originalJar = MOD_DIRECTORY.resolve(jarNamePath);
                Path cachedJar = CACHE_BANSOUKOU_DIRECTORY.resolve(jarNamePath);
                if (Files.exists(originalJar)) {
                    if (needsPatching(patchFile, cachedJar)) {
                        patchJar(originalJar, patchFile, cachedJar);
                        LOGGER.info("Patching and caching {}", jarName);
                    } else {
                        LOGGER.info("{} is up to date.", jarName);
                    }
                    MOD_TO_PATCH.put(originalJar.toAbsolutePath(), cachedJar);
                } else {
                    LOGGER.info("Patch found for {}, but mod file is not present, skipping.", jarName);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to gather bansoukou patches", e);
        }

        if (!MOD_TO_PATCH.isEmpty()) {
            BansoukouModList.replace();
            BansoukouSecurityManager.replace();
            BansoukouFMLTweaker.replace();
            if (System.getSecurityManager().getClass() == FMLSecurityManager.class) {
                // Re-install SecurityManager
                BansoukouSecurityManager.replace();
            }
        }

    }

    @Override
    public String[] getASMTransformerClass() {
        return null;
    }

    @Override
    public String getModContainerClass() {
        return "zone.rong.bansoukou.Bansoukou$Container";
    }

    @Nullable
    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) { }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }

    public static class Container extends DummyModContainer {

        public Container() {
            super(new ModMetadata());
            ModMetadata meta = this.getMetadata();
            meta.modId = Tags.MOD_ID;
            meta.name = Tags.MOD_NAME;
            meta.description = Tags.MOD_DESCRIPTION;
            meta.version = Tags.VERSION;
            meta.logoFile = "/icon.png";
            meta.authorList.add("Rongmario");
        }

        @Override
        public boolean registerBus(EventBus bus, LoadController controller) {
            bus.register(this);
            return true;
        }

    }

}
