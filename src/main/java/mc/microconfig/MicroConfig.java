package mc.microconfig;

import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class MicroConfig {
    final List<MicroConfigTypeHandler<?>> handlers;
    
    public MicroConfig(List<MicroConfigTypeHandler<?>> handlers) {
        this.handlers = handlers;
    }
    
    /**
     * Takes in a file name and creates a file in the config directory as specified by fabric
     * <p>
     * Creates the file if it doesn't exist, reads it into the object, then sync the file with the expected structure
     *
     * @param name       The name of the file, will have ".mcfg" appended automatically
     * @param configData The data object, must implement ConfigData as a sanity check
     * @return The originally passed object, with fields set to the values read from the config file
     */
    @SuppressWarnings("unused")
    public static <T extends ConfigData> T getOrCreate(String name, T configData) {
        // Get the expected file
        File configFile = FabricLoader.getInstance().getConfigDir().resolve(name + ".mcfg").toFile();
        // Actually Make it
        return getOrCreate(configFile, configData);
    }
    
    /**
     * Works with any file, doesn't depend on FabricLoader
     * <p>
     * Creates the file if it doesn't exist, reads it into the object, then sync the file with the expected structure
     *
     * @param configFile The file for the config file
     * @param configData The data object, must implement ConfigData as a sanity check
     * @return The original object, with fields set to the read values from the config file
     */
    public static <T extends ConfigData> T getOrCreate(File configFile, T configData) {
        return new MicroConfig(Collections.emptyList()).getOrCreateWithHandlers(configFile, configData);
    }
    
    /**
     * Takes in a file name and creates a file in the config directory as specified by fabric
     * <p>
     * Creates the file if it doesn't exist, reads it into the object, then sync the file with the expected structure
     * <p>
     * Also uses type handlers
     *
     * @param name       The name of the file, will have ".mcfg" appended automatically
     * @param configData The data object, must implement ConfigData as a sanity check
     * @return The originally passed object, with fields set to the values read from the config file
     */
    @SuppressWarnings("unused")
    public <T extends ConfigData> T getOrCreateWithHandlers(String name, T configData) {
        // Get the expected file
        File configFile = FabricLoader.getInstance().getConfigDir().resolve(name + ".mcfg").toFile();
        // Actually Make it
        return getOrCreateWithHandlers(configFile, configData);
    }
    
    /**
     * Works with any file, doesn't depend on FabricLoader
     * <p>
     * Creates the file if it doesn't exist, reads it into the object, then sync the file with the expected structure
     * <p>
     * Also uses type handlers
     *
     * @param configFile The file for the config file
     * @param configData The data object, must implement ConfigData as a sanity check
     * @return The original object, with fields set to the read values from the config file
     */
    public <T extends ConfigData> T getOrCreateWithHandlers(File configFile, T configData) {
        try {
            boolean didExist = configFile.exists();
            
            // If it doesn't exist, create it for the first time
            if (!didExist) {
                new MicroConfigWriter(new FileWriter(configFile, false), configData, handlers);
                // Since nothing could have been updated, the file is up-to-date already
                // Few cycles saved :p
                return configData;
            }
            
            // Load it back
            new MicroConfigReader(configFile, configData, handlers);
            
            // Write it to update with possibly new values/structure
            new MicroConfigWriter(new FileWriter(configFile, false), configData, handlers);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return configData;
    }
}
