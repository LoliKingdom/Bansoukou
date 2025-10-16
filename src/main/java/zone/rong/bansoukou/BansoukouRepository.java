package zone.rong.bansoukou;

import net.minecraftforge.fml.relauncher.libraries.Artifact;
import net.minecraftforge.fml.relauncher.libraries.Repository;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

class BansoukouRepository extends Repository { // LinkRepository

    private static File location() {
        URL path = BansoukouRepository.class.getProtectionDomain().getCodeSource().getLocation();
        URI resolvedPath;
        try {
            URLConnection connection = path.openConnection();
            if (connection instanceof JarURLConnection) {
                resolvedPath = ((JarURLConnection) connection).getJarFileURL().toURI();
            } else {
                resolvedPath = path.toURI();
            }
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException("Unable to obtain Bansoukou's source", e);
        }
        return new File(resolvedPath);
    }

    private final Repository memory;
    private final Map<Path, Path> patches;

    BansoukouRepository(Repository memory, File root, Map<Path, Path> patches) throws IOException {
        super(root);
        this.memory = memory;
        this.patches = patches;
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
        File bansoukouFile = location();
        if (bansoukouFile.isFile()) {
            list.remove(bansoukouFile);
        }
        list.replaceAll(file -> {
            Path patched = this.patches.get(file.toPath().toAbsolutePath());
            if (patched == null) {
                return file;
            }
            return patched.toFile();
        });
    }

}
