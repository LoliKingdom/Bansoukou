package zone.rong.bansoukou;

import com.google.common.annotations.VisibleForTesting;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.common.ForgeVersion;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.*;
import java.util.Map;
import java.util.zip.ZipException;

@IFMLLoadingPlugin.MCVersion(ForgeVersion.mcVersion)
@IFMLLoadingPlugin.SortingIndex(Integer.MIN_VALUE)
@IFMLLoadingPlugin.Name("Bansoukou")
public class BansoukouCoreMod implements IFMLLoadingPlugin {

    public static final Logger LOGGER = LogManager.getLogger("Bansoukou");

    public BansoukouCoreMod() throws IOException {
        LOGGER.info("Ikimasu!");
        File bansoukouFolder = new File(Launch.minecraftHome, "bansoukou");
        if (!bansoukouFolder.mkdir()) {
            LOGGER.info("Nothing to patch at the moment!");
            return;
        }
        File[] patchRoot = bansoukouFolder.listFiles();
        if (!bansoukouFolder.exists() || patchRoot == null) {
            LOGGER.info("No patches found. Continuing with mod loading.");
            return;
        }
        File mods = new File(Launch.minecraftHome, "mods");
        final Map<File, File> jars = new Object2ObjectOpenHashMap<>(patchRoot.length);
        for (File file : patchRoot) {
            String fileName = file.getName().concat(".jar");
            Path path = new File(mods, fileName).toPath();
            LOGGER.warn(path);
            if (Files.exists(path)) {
                File pathFile = path.toFile();
                try (RandomAccessFile raf = new RandomAccessFile(pathFile, "r")) {
                    int start = raf.readInt();
                    if (start == 0x504B0304 || start == 0x504B0506 || start == 0x504B0708) {
                        LOGGER.info("{} was found in the mods folder. Later to be patched.", fileName);
                        jars.put(file, pathFile);
                    } else {
                        throw LOGGER.throwing(new ZipException(fileName + " -> exists in the mods folder but isn't a valid jar file! Report to the pack author!"));
                    }
                }
            } else {
                throw LOGGER.throwing(new FileNotFoundException(fileName + " -> does not exist in mods folder! Perhaps the mod's name has changed? Report to the pack author!"));
            }
        }
        for (Map.Entry<File, File> entry : jars.entrySet()) {
            File root = entry.getKey();
            File zip = entry.getValue();
            try (FileSystem fs = FileSystems.newFileSystem(zip.toPath(), null)) {
                Files.walk(root.toPath())
                        .map(Path::toFile)
                        .filter(f -> !f.isDirectory())
                        .forEach(f -> {
                            Path currentPath = fs.getPath(root.toURI().relativize(f.toURI()).toString());
                            try {
                                work(f, currentPath);
                            } catch (IOException e) {
                                if (e instanceof NoSuchFileException) {
                                    try {
                                        Files.createDirectories(currentPath);
                                        work(f, currentPath);
                                    } catch (IOException e2) {
                                        e2.printStackTrace();
                                    }
                                } else {
                                    e.printStackTrace();
                                }
                            }
                        });
            }
        }
    }

    private void work(File f, Path currentPath) throws IOException {
        if (f.length() <= 0) {
            LOGGER.warn("Removing {}...", currentPath);
            Files.deleteIfExists(currentPath);
        } else {
            LOGGER.warn("Patching {}...", currentPath);
            Files.copy(f.toPath(), currentPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Deprecated
    @VisibleForTesting
    public void kek() {
        String command = "jar uf " + "AcademyCraft-1.1.3.jar" + " cunny.py";
        try {
            Runtime.getRuntime().exec(command, null, new File(Launch.minecraftHome, "mods"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String[] getASMTransformerClass() {
        return null;
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

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

}
