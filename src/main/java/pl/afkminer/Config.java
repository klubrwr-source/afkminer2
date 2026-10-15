package pl.afkminer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("afkminer.json");

    public static Config INSTANCE = new Config();

    public boolean enabled = false;
    public boolean keepRunningUnfocused = true;   // kopanie dziala gdy okno gry jest w tle
    public boolean swingArm = true;               // animacja machania reka
    public boolean stopOnLowDurability = true;
    public int minDurability = 10;                // stop gdy narzedzie ma <= tyle wytrzymalosci
    public boolean stopOnLowHealth = true;
    public int minHealth = 6;                     // w polowkach serc (6 = 3 serca)
    public boolean stopOnDamage = false;          // stop gdy dostaniesz obrazenia
    public boolean stopOnFullInventory = false;
    public boolean showMessages = true;

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                Config c = GSON.fromJson(Files.readString(FILE), Config.class);
                if (c != null) INSTANCE = c;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        try {
            Files.writeString(FILE, GSON.toJson(INSTANCE));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
