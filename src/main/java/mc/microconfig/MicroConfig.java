package mc.microconfig;

import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UnknownFormatConversionException;

public class MicroConfig {
    private static final ArrayList<ConfigData> parsingStack = new ArrayList<>();
    private static FileWriter writer = null;
    private static File configFile = null;
    
    //region Public API
    
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
     * @param configFile The file for for the config file
     * @param configData The data object, must implement ConfigData as a sanity check
     * @return The original object, with fields set to the read values from the config file
     */
    public static <T extends ConfigData> T getOrCreate(File configFile, T configData) {
        boolean didExist = configFile.exists();
        MicroConfig.configFile = configFile;
        parsingStack.clear();
        
        // If it doesn't exist, create it for the first time
        if (!didExist) {
            makeWriter(false, configData);
            createConfigFile(0);
            close();
        }
        
        // Load it back
        makeWriter(didExist, configData);
        try {
            loadConfig();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        close();
        
        makeWriter(false, configData);
        createConfigFile(0);
        close();
        
        return configData;
    }
    //endregion
    
    //region Reading methods
    private static void loadConfig() throws IOException {
        List<String> lines = Files.readAllLines(configFile.toPath());
        
        int lastIndentation = 0;
        
        // Handle each line
        for (String line : lines) {
            int orig = line.length();
            int after = line.replaceFirst(" *", "").length();
            int newIndent = (orig - after) / 4;
    
            for(int diff = newIndent - lastIndentation; diff < 0; ++diff) {
                ConfigData save = last();
                parsingStack.remove(save);
                if (parsingStack.isEmpty()) {
                    parsingStack.add(save);
                }
            }
            
            lastIndentation = newIndent;
            
            if (!line.trim().startsWith("//") && line.trim().length() > 3) {
                handleLine(line.trim(), last());
            }
        }
    }
    
    private static void handleLine(String line, ConfigData configData) {
        if (line.endsWith(":")) {
            String fieldName = line.trim().replace(":", "");
            Field found = getClassField(last().getClass(), fieldName);
            try {
                if (found != null) {
                    parsingStack.add((ConfigData)found.get(last()));
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            return;
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
        Field field = getClassField(last().getClass(), fieldName);
        
        // If we found a field, set it
        if (field != null) {
            unpackFieldValuePair(configData, field, value);
        }
    }
    
    private static void unpackFieldValuePair(ConfigData object, Field field, String value) {
        try {
            if (int.class.equals(field.getType())) {
                field.set(object, Integer.parseInt(value));
            } else if (float.class.equals(field.getType())) {
                field.set(object, Float.parseFloat(value));
            } else if (double.class.equals(field.getType())) {
                field.set(object, Double.parseDouble(value));
            } else if (boolean.class.equals(field.getType())) {
                field.set(object, Boolean.parseBoolean(value));
            } else if (String.class.equals(field.getType())) {
                field.set(object, value);
            } else if (field.isEnumConstant()) {
                //noinspection unchecked,rawtypes
                field.set(object, Enum.valueOf((Class<Enum>)field.getType(), value));
            } else {
                throw new UnknownFormatConversionException(field.getName() + " is unsupported type \"" + field.getType() + "\")");
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
    //endregion
    
    //region Writing Methods
    private static void createConfigFile(int depth) {
        for (Field field : last().getClass().getFields()) {
            if (isStandardClassType(field.getType())) {
                appendDefaultField(last(), field, depth);
            } else {
                try {
                    appendDefaultField(last(), field, depth);
                    parsingStack.add((ConfigData)field.get(last()));
                    createConfigFile(depth + 1);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        parsingStack.remove(last());
    }
    
    private static void appendDefaultField(ConfigData type, Field field, int depth) {
        try {
            // Check if the field has a comment annotation
            Comment annotation = field.getAnnotation(Comment.class);
            
            // If it does, write the comment and mark that we need an extra line
            // This keeps the output overall more clean
            boolean doExtraBreak = false;
            String indent = String.join("", Collections.nCopies(depth, "    "));
            if (annotation != null) {
                String clean = "//" + annotation.value().replace(
                    "\n",
                    "\n" + indent + "//"
                );
                writer.append(indent)
                    .append(clean)
                    .append("\n");
                doExtraBreak = true;
            }
            
            // Write the pair and extra line if required
            writer.append(indent).append(field.getName());
            if (isStandardClassType(field.getType())) {
                writer.append("=")
                    .append(field.get(type).toString());
            } else {
                writer.append(":");
            }
            writer.append("\n");
            
            if (doExtraBreak && isStandardClassType(field.getType())) {
                writer.append(indent).append("\n");
            }
        } catch (IOException | IllegalAccessException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
    
    /**
     * Utility method for if a class can be serial-ed directly
     *
     * @param clazz The class type to evaluate
     * @return If the class is a primitive, enum, or String
     */
    private static boolean isStandardClassType(Class<?> clazz) {
        return clazz.isPrimitive() || clazz.isEnum() || clazz == String.class;
    }
    //endregion
    
    //region Utility Methods
    
    private static void makeWriter(boolean append, ConfigData data) {
        try {
            writer = new FileWriter(configFile, append);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        parsingStack.add(data);
    }
    
    private static void close() {
        try {
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
    
    /**
     * @return The last entry in parsingStack
     */
    private static ConfigData last() {
        return parsingStack.get(parsingStack.size() - 1);
    }
    
    /**
     * Small utility method to quickly find a field by name on a class
     *
     * @param clazz The class to do the lookup on
     * @param name  The name of the field to find
     * @return The field if one exists by that name, or null
     */
    private static Field getClassField(Class<?> clazz, String name) {
        try {
            return clazz.getField(name);
        } catch (NoSuchFieldException e) {
            return null;
        }
    }
    //endregion
}
