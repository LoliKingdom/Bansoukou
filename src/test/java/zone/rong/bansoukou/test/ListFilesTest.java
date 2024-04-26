package zone.rong.bansoukou.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ListFilesTest {

    private static final char[] ILLEGAL_CHARACTERS = new char[] { '/', '\\', '<', '>', '?', '%', ' ', '*', '|', ':', '"' };

    @Test
    public void test(@TempDir Path directory) throws IOException {
        List<Path> files = new ArrayList<>();
        outer: for (char i = 32; i < 127; i++) { // 33 ~ 126 for non control-ASCII characters
            for (char illegalCharacter : ILLEGAL_CHARACTERS) {
                if (i == illegalCharacter) {
                    continue outer;
                }
            }
            Path resolved = directory.resolve(i + "bansoukou.jar");
            files.add(resolved);
            Files.createFile(resolved);
        }
        Assertions.assertFalse(files.isEmpty());
        File directoryAsFile = directory.toFile();
        for (File file : directoryAsFile.listFiles()) {
            System.out.println(file);
        }
    }

}
