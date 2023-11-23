package zone.rong.bansoukou;

import com.google.common.eventbus.EventBus;
import net.minecraftforge.common.ForgeVersion;
import net.minecraftforge.fml.common.DummyModContainer;
import net.minecraftforge.fml.common.LoadController;
import net.minecraftforge.fml.common.ModMetadata;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@IFMLLoadingPlugin.Name("-Bansoukou-")
@IFMLLoadingPlugin.MCVersion(ForgeVersion.mcVersion)
@IFMLLoadingPlugin.SortingIndex(Integer.MIN_VALUE)
public class BansoukouCoreMod implements IFMLLoadingPlugin {

    public static final Logger LOGGER = LogManager.getLogger("Bansoukou");
    public static final Map<String, byte[]> PATCHED_CLASSES = new ConcurrentHashMap<>();

    public BansoukouCoreMod() {
        LOGGER.info("Ikimasu!");
        try {
            Path root = Paths.get("bansoukou");
            Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (file.toString().endsWith("class")) {
                        String className = root.relativize(file).toString().replace(FileSystems.getDefault().getSeparator(), ".");
                        className = className.substring(0, className.length() - ".class".length());
                        ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
                        Files.copy(file, byteOutput);
                        PATCHED_CLASSES.put(className, byteOutput.toByteArray());
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            LOGGER.catching(e);
        }
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[]{"zone.rong.bansoukou.BansoukouTransformer"};
    }

    @Override
    public String getModContainerClass() {
        return "zone.rong.bansoukou.BansoukouCoreMod$Container";
    }

    @Nullable
    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }

    public static class Container extends DummyModContainer {

        public Container() {
            super(new ModMetadata());
            ModMetadata meta = this.getMetadata();
            meta.modId = "bansoukou";
            meta.name = "Bansoukou";
            meta.description = "A simple coremod that streamlines patching of mods.";
            meta.version = "4.3.1";
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
