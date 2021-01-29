package mc.microconfig;

import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.UnknownFormatConversionException;

public class MicroConfig {
    public static <T extends ConfigData> T getOrCreate(String name, T configData) {
        File configFile = FabricLoader.getInstance().getConfigDir().resolve(name + ".mcfg").toFile();
        if (!configFile.exists()) {
            createConfigFile(configFile, configData);
        }
        loadConfig(configFile, configData);
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
        ArrayList<Field> unsetFields = new ArrayList<>(Arrays.asList(configData.getClass().getDeclaredFields()));
        
        try {
            Scanner scanner = new Scanner(configFile);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.startsWith("//") || line.length() <= 3) {
                    continue;
                }

                String fieldName;
                String value;
                try {
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
                
                Field field = null;
                for (Field possible : configData.getClass().getFields()) {
                    if (possible.getName().equals(fieldName)) {
                        field = possible;
                    }
                }
                unsetFields.remove(field);
                
                while (value.startsWith(" ")) {
                    value = value.substring(1);
                }
                
                if (field != null) {
                    unpackFieldValuePair(configData, field, value);
                }
            }
            
            for (Field field : unsetFields) {
                FileWriter writer = new FileWriter(configFile, true);
                appendDefaultField(writer, configData, field);
                writer.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private static void createConfigFile(File configFile, ConfigData object) {
        try {
            FileWriter writer = new FileWriter(configFile);
            for (Field field : object.getClass().getDeclaredFields()) {
                appendDefaultField(writer, object, field);
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private static void appendDefaultField(FileWriter writer, ConfigData type, Field field) {
        try {
            Comment annotation = field.getAnnotation(Comment.class);
            
            boolean doExtraBreak = false;
            if (annotation != null) {
                String clean = "//" + annotation.value().replace("\n", "\n//");
                writer.append(clean).append("\n");
                doExtraBreak = true;
            }
            writer.append(field.getName()).append("=").append(field.get(type).toString()).append("\n");
            if (doExtraBreak) {
                writer.append("\n");
            }
        } catch (IOException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }
    
    private static void unpackFieldValuePair(ConfigData object, Field field, String value) {
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
