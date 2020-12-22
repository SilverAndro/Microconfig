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
    public static <T extends ConfigData> T getOrCreate(String name, T type) {
        File configFile = FabricLoader.getInstance().getConfigDir().resolve(name + ".mcfg").toFile();
        if (!configFile.exists()) {
            createConfigFile(configFile, type);
        }
        loadConfig(configFile, type);
        return type;
    }
    
    private static void loadConfig(File configFile, ConfigData type) {
        ArrayList<Field> unsetFields = new ArrayList<>(Arrays.asList(type.getClass().getDeclaredFields()));
        
        try {
            Scanner scanner = new Scanner(configFile);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] split = line.split("=");
                
                String fieldName = split[0].replace(" ", "");
                String value = split[1];
                
                Field field = null;
                for (Field possible : type.getClass().getDeclaredFields()) {
                    if (possible.getName().equals(fieldName)) {
                        field = possible;
                    }
                }
                unsetFields.remove(field);
                
                while (value.startsWith(" ")) {
                    value = value.substring(1);
                }
                
                assert field != null;
                unpackFieldValuePair(type, field, value);
            }
            
            for (Field field : unsetFields) {
                FileWriter writer = new FileWriter(configFile, true);
                appendDefaultField(writer, type, field);
                writer.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private static void createConfigFile(File configFile, ConfigData type) {
        try {
            if (configFile.createNewFile()) {
                FileWriter writer = new FileWriter(configFile);
                for (Field field : type.getClass().getDeclaredFields()) {
                    appendDefaultField(writer, type, field);
                }
                writer.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private static void appendDefaultField(FileWriter writer, ConfigData type, Field field) {
        try {
            writer.append(field.getName()).append("=").append(field.get(type).toString()).append("\n");
        } catch (IOException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }
    
    private static void unpackFieldValuePair(ConfigData type, Field field, String value) {
        try {
            Class<?> fieldType = field.getType();
            if (fieldType == int.class) {
                field.set(type, Integer.parseInt(value));
                return;
            }
            if (fieldType == float.class) {
                field.set(type, Float.parseFloat(value));
                return;
            }
            if (fieldType == double.class) {
                field.set(type, Double.parseDouble(value));
                return;
            }
            if (fieldType == String.class) {
                field.set(type, value);
                return;
            }
            if (fieldType == boolean.class) {
                field.set(type, Boolean.parseBoolean(value));
                return;
            }
            throw new UnknownFormatConversionException(field.getName() + " was unable to be deserialized (unsupported type \"" + fieldType + "\")");
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
