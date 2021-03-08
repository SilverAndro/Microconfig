package mc.microconfig;

import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Scanner;
import java.util.UnknownFormatConversionException;

public class MicroConfig {
    /**
     * Takes in a name and creates a file in the config directory as specified by fabric
     *
     * Creates the file if it doesn't exist, reads it into the object, then sync the file with the expected structure
     *
     * @param name The name of the file, will have ".mcfg" appended
     * @param configData The data object, must implement ConfigData as a sanity check
     * @return The original object, with fields set to the read values from the config file
     */
    @SuppressWarnings("unused")
    public static <T extends ConfigData> T getOrCreate(String name, T configData) {
        // Get the expected file
        File configFile = FabricLoader.getInstance().getConfigDir().resolve(name + ".mcfg").toFile();
        return getOrCreate(configFile, configData);
    }
    
    /**
     * Works with any file, doesn't depend on FabricLoader
     *
     * Creates the file if it doesn't exist, reads it into the object, then sync the file with the expected structure
     *
     * @param configFile The file for for the config file
     * @param configData The data object, must implement ConfigData as a sanity check
     * @return The original object, with fields set to the read values from the config file
     */
    public static <T extends ConfigData> T getOrCreate(File configFile, T configData) {
        // If it doesn't exist, create it for the first time
        if (!configFile.exists()) {
            createConfigFile(configFile, configData);
        }
        
        // Load it back
        loadConfig(configFile, configData);
        
        // Try to overwrite the whole file again but with the loaded object
        // This makes sure the file is always in sync with the expected values
        try {
            FileWriter writer = new FileWriter(configFile, false);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            System.err.println("Failed to delete config file " + configFile + " so it can be automatically updated");
        }
        createConfigFile(configFile, configData);
        return configData;
    }
    
    private static void loadConfig(File configFile, ConfigData configData) {
        try {
            // Scan in the file
            Scanner scanner = new Scanner(configFile);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                // Ignore very short lines or comments
                // 3 characters because the minimum meaningful line is "a=1" or similar
                if (line.startsWith("//") || line.length() <= 3) {
                    continue;
                }

                String fieldName;
                String value;
                try {
                    // Attempt to parse the line
                    String[] split = line.split("=");

                    fieldName = split[0].replace(" ", "");
                    value = split[1].replaceFirst(" *", "");
                } catch (IndexOutOfBoundsException e) {
                    throw new UnknownFormatConversionException(
                            "Unable to read config line \"" +
                                    line +
                                    "\" for file " +
                                    configFile.getName()
                    );
                }
                
                // Find the field that matches the expected name
                Field field = null;
                for (Field possible : configData.getClass().getFields()) {
                    if (possible.getName().equals(fieldName)) {
                        field = possible;
                        break;
                    }
                }
                
                // If we found a field, set it
                if (field != null) {
                    unpackFieldValuePair(configData, field, value);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private static void createConfigFile(File configFile, ConfigData object) {
        try {
            // Add every field to the file
            FileWriter writer = new FileWriter(configFile);
            for (Field field : object.getClass().getFields()) {
                appendDefaultField(writer, object, field);
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private static void appendDefaultField(FileWriter writer, ConfigData type, Field field) {
        try {
            // Check if the field has a comment annotation
            Comment annotation = field.getAnnotation(Comment.class);
            
            // If it does, write the comment and mark that we need an extra line
            // This keeps the output overall more clean
            boolean doExtraBreak = false;
            if (annotation != null) {
                String clean = "//" + annotation.value().replace("\n", "\n//");
                writer.append(clean).append("\n");
                doExtraBreak = true;
            }
            
            // Write the pair and extra line if required
            writer.append(field.getName()).append("=").append(field.get(type).toString()).append("\n");
            if (doExtraBreak) {
                writer.append("\n");
            }
        } catch (IOException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }
    
    private static void unpackFieldValuePair(ConfigData object, Field field, String value) {
        // Fairly self explanatory
        // Only with I could use switch blocks
        try {
            Class<?> fieldType = field.getType();
            if (fieldType == int.class) {
                field.set(object, Integer.parseInt(value));
                return;
            }
            if (fieldType == float.class) {
                field.set(object, Float.parseFloat(value));
                return;
            }
            if (fieldType == double.class) {
                field.set(object, Double.parseDouble(value));
                return;
            }
            if (fieldType == String.class) {
                field.set(object, value);
                return;
            }
            if (fieldType == boolean.class) {
                field.set(object, Boolean.parseBoolean(value));
                return;
            }
            throw new UnknownFormatConversionException(field.getName() + " was unable to be deserialized (unsupported type \"" + fieldType + "\")");
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
