package zone.rong.bansoukou;

import com.google.common.eventbus.EventBus;
import net.minecraftforge.fml.common.DummyModContainer;
import net.minecraftforge.fml.common.LoadController;
import net.minecraftforge.fml.common.MetadataCollection;
import net.minecraftforge.fml.common.ModMetadata;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

public class BansoukouMod extends DummyModContainer {

    private static ModMetadata loadMetadata(String id, String modName, String version) {
        try (JarFile jar = new JarFile(BansoukouCoreMod.source)) {
            ZipEntry modInfo = jar.getEntry("mcmod.info");
            try (InputStream inputStream = jar.getInputStream(modInfo)) {
                return MetadataCollection.from(inputStream, id).getMetadataForId(id, Collections.emptyMap());
            }
        } catch (IOException e) {
            ModMetadata meta = new ModMetadata();
            meta.modId = id;
            meta.name = modName;
            meta.version = version;
            meta.authorList.add("Rongmario");
            return meta;
        }
    }

    public BansoukouMod() {
        super(loadMetadata(Tags.MOD_ID, Tags.MOD_NAME, Tags.VERSION));
    }

    @Override
    public boolean registerBus(EventBus bus, LoadController controller) {
        bus.register(this);
        return true;
    }

}
