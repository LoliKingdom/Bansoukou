package zone.rong.bansoukou;

import net.minecraftforge.fml.relauncher.libraries.Artifact;
import net.minecraftforge.fml.relauncher.libraries.Repository;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;

class BansoukouRepository extends Repository { // LinkRepository

    private static final File BANSOUKOU_FILE;

    static {
        try {
            BANSOUKOU_FILE = new File(BansoukouRepository.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException("Unable to obtain Bansoukou's source", e);
        }
    }

    private final Repository memory;

    BansoukouRepository(Repository memory, File root) throws IOException {
        super(root);
        this.memory = memory;
    }

    @Override
    public Artifact resolve(Artifact artifact) {
        return this.memory.resolve(artifact);
    }

    @Override
    public File getFile(String path) {
        return this.memory.getFile(path);
    }

    @Override
    public File archive(Artifact artifact, File file, byte[] manifest) {
        return this.memory.archive(artifact, file, manifest);
    }

    @Override
    public void filterLegacy(List<File> list) {
        if (BANSOUKOU_FILE.isFile()) {
            list.remove(BANSOUKOU_FILE);
        }
        list.replaceAll(file -> {
            Path patched = Bansoukou.MOD_TO_PATCH.get(file.toPath().toAbsolutePath());
            if (patched == null) {
                return file;
            }
            return patched.toFile();
        });
    }

}
