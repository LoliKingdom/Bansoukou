package zone.rong.bansoukou;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class PatcherTest {

    private static byte[] bytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    private static void writeZip(Path file, Map<String, byte[]> entries) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(file))) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                zos.putNextEntry(new ZipEntry(entry.getKey()));
                zos.write(entry.getValue());
                zos.closeEntry();
            }
        }
    }

    // Original mod: two patchable files, one untouched nested file, a manifest and signature files.
    private static Map<String, byte[]> originalContents() {
        Map<String, byte[]> m = new LinkedHashMap<>();
        m.put("a.txt", bytes("A"));
        m.put("b.txt", bytes("B-old"));
        m.put("dir/keep.txt", bytes("keep"));
        m.put("META-INF/MANIFEST.MF", bytes("Manifest-Version: 1.0\n"));
        m.put("META-INF/FOO.SF", bytes("signature"));
        m.put("META-INF/FOO.RSA", bytes("signature"));
        return m;
    }

    // Patch: delete a.txt (empty entry), overwrite b.txt, add c.txt.
    private static Map<String, byte[]> patchContents() {
        Map<String, byte[]> m = new LinkedHashMap<>();
        m.put("a.txt", new byte[0]); // delete marker
        m.put("b.txt", bytes("B-new"));
        m.put("c.txt", bytes("C"));
        return m;
    }

    private static String read(Path jar, String entry) throws IOException {
        try (FileSystem fs = FileSystems.newFileSystem(jar, null)) {
            Path p = fs.getPath(entry);
            return Files.exists(p) ? new String(Files.readAllBytes(p), StandardCharsets.UTF_8) : null;
        }
    }

    private static void assertPatched(Path cache) throws IOException {
        Assertions.assertNull(read(cache, "a.txt"), "a.txt should be deleted by empty patch entry");
        Assertions.assertEquals("B-new", read(cache, "b.txt"), "b.txt should be overwritten");
        Assertions.assertEquals("C", read(cache, "c.txt"), "c.txt should be added");
        Assertions.assertEquals("keep", read(cache, "dir/keep.txt"), "untouched file should survive");
        Assertions.assertNull(read(cache, "META-INF/FOO.SF"), ".SF signature should be stripped");
        Assertions.assertNull(read(cache, "META-INF/FOO.RSA"), ".RSA signature should be stripped");
        Assertions.assertNotNull(read(cache, "META-INF/MANIFEST.MF"), "manifest should survive");
    }

    @Test
    void zipPatch(@TempDir Path dir) throws IOException {
        Path original = dir.resolve("mod.jar");
        Path patch = dir.resolve("patch.zip");
        Path cache = dir.resolve("cache.jar");
        writeZip(original, originalContents());
        writeZip(patch, patchContents());
        Bansoukou.patchJar(original, patch, cache);
        assertPatched(cache);
    }

    @Test
    void jarPatch(@TempDir Path dir) throws IOException {
        Path original = dir.resolve("mod.jar");
        Path patch = dir.resolve("patch.jar");
        Path cache = dir.resolve("cache.jar");
        writeZip(original, originalContents());
        writeZip(patch, patchContents());
        Bansoukou.patchJar(original, patch, cache);
        assertPatched(cache);
    }

    @Test
    void directoryPatch(@TempDir Path dir) throws IOException {
        Path original = dir.resolve("mod.jar");
        writeZip(original, originalContents());

        Path patchDir = dir.resolve("patch");
        Files.createDirectories(patchDir.resolve("dir"));
        Files.write(patchDir.resolve("a.txt"), new byte[0]); // delete marker
        Files.write(patchDir.resolve("b.txt"), bytes("B-new"));
        Files.write(patchDir.resolve("c.txt"), bytes("C"));

        Path cache = dir.resolve("cache.jar");
        Bansoukou.patchJar(original, patchDir, cache);
        assertPatched(cache);
    }

    @Test
    void needsPatchingByModifiedTime(@TempDir Path dir) throws IOException {
        Path original = dir.resolve("mod.jar");
        Path patch = dir.resolve("patch.zip");
        Path cache = dir.resolve("cache.jar");
        writeZip(original, originalContents());
        writeZip(patch, patchContents());

        Assertions.assertTrue(Bansoukou.needsPatching(patch, cache), "missing cache => needs patching");

        Bansoukou.patchJar(original, patch, cache);
        Files.setLastModifiedTime(cache, FileTime.fromMillis(Files.getLastModifiedTime(patch).toMillis() + 5000L));
        Assertions.assertFalse(Bansoukou.needsPatching(patch, cache), "cache newer than patch => up to date");

        Files.setLastModifiedTime(patch, FileTime.fromMillis(Files.getLastModifiedTime(cache).toMillis() + 5000L));
        Assertions.assertTrue(Bansoukou.needsPatching(patch, cache), "patch newer than cache => needs patching");
    }

    @Test
    void needsPatchingWhenCacheCorrupted(@TempDir Path dir) throws IOException {
        Path original = dir.resolve("mod.jar");
        Path patch = dir.resolve("patch.zip");
        Path cache = dir.resolve("cache.jar");
        writeZip(original, originalContents());
        writeZip(patch, patchContents());

        Bansoukou.patchJar(original, patch, cache);
        Files.setLastModifiedTime(cache, FileTime.fromMillis(Files.getLastModifiedTime(patch).toMillis() + 5000L));
        Assertions.assertFalse(Bansoukou.needsPatching(patch, cache), "intact cache => up to date");

        Files.write(cache, new byte[0]); // Truncated jar, as left behind by an interrupted patch
        Files.setLastModifiedTime(cache, FileTime.fromMillis(Files.getLastModifiedTime(patch).toMillis() + 5000L));
        Assertions.assertTrue(Bansoukou.needsPatching(patch, cache), "corrupted cache => needs patching");
    }

    @Test
    void failedPatchLeavesNoCacheBehind(@TempDir Path dir) throws IOException {
        Path original = dir.resolve("mod.jar");
        Path patch = dir.resolve("patch.zip");
        Path cache = dir.resolve("cache.jar");
        writeZip(original, originalContents());
        Files.write(patch, bytes("not a zip"));

        Assertions.assertThrows(IOException.class, () -> Bansoukou.patchJar(original, patch, cache));
        Assertions.assertFalse(Files.exists(cache.resolveSibling(cache.getFileName() + ".tmp")), "no temporary jar left behind");
    }
}
