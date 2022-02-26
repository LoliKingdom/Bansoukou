package zone.rong.bansoukou;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import net.minecraft.launchwrapper.Launch;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;

public class BansoukouConfig {

    public static BansoukouConfig instance;

    static {
        File rootFolder = Launch.minecraftHome == null ? new File(".") : Launch.minecraftHome;
        File bansoukouRoot = new File(rootFolder, "bansoukou");
        File configFile = new File(bansoukouRoot, "config.json");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            if (configFile.createNewFile()) { //File does not exist, save to file
                String json = gson.toJson(new JsonParser().parse(gson.toJson(BansoukouConfig.class)));
                try (PrintWriter out = new PrintWriter(configFile)) {
                    out.println(json);
                }
            } else { // File exists, load from file
                instance = gson.fromJson(new String(Files.readAllBytes(configFile.toPath())), BansoukouConfig.class);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



}
