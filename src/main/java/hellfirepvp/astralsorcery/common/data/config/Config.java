package hellfirepvp.astralsorcery.common.data.config;

import net.minecraftforge.common.config.Configuration;

import java.io.File;

/**
 * This class is part of the Astral Sorcery Mod
 * The complete source code for this mod can be found on github.
 * Class: Config
 * Created by HellFirePvP
 * Date: 07.05.2016 / 01:14
 */
public class Config {

    private static Configuration latestConfig;

    public static boolean stopOnIllegalState = true;
    public static int crystalDensity = 40;
    public static int marbleAmount = 3, marbleVeinSize = 12;

    private Config() {}

    public static void load(File file) {
        latestConfig = new Configuration(file);
        latestConfig.load();
        loadData();
        latestConfig.save();
    }

    private static void loadData() {
        stopOnIllegalState = latestConfig.getBoolean("stopOnIllegalState", "general", Boolean.TRUE, "If this is set to 'true' the server or client will exit the game with a crash in case it encounters a state that might lead to severe issues but doesn't actually crash the server/client. If this is set to 'false' it will only print a warning in the console.");

        //rand(crystalDensity) == 0 chance per chunk.
        crystalDensity = latestConfig.getInt("crystalDensity", "worldgen", 5, 0, 40, "Defines how frequently rock-crystals will spawn underground. The lower the number, the more frequent crystals will spawn.");
        marbleAmount = latestConfig.getInt("generateMarbleAmount", "worldgen", 3, 0, 32, "Defines how many marble veins are generated per chunk.");
        marbleVeinSize = latestConfig.getInt("generateMarbleVeinSize", "worldgen", 12, 1, 32, "Defines how big generated marble veins are.");
    }

    public static void save() {
        latestConfig.save();
    }

}