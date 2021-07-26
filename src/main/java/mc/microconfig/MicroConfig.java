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
    /**
     * Takes in a name and creates a file in the config directory as specified by fabric
     * <p>
     * Creates the file if it doesn't exist, reads it into the object, then sync the file with the expected structure
     *
     * @param name       The name of the file, will have ".mcfg" appended
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
     * <p>
     * Creates the file if it doesn't exist, reads it into the object, then sync the file with the expected structure
     *
     * @param configFile The file for for the config file
     * @param configData The data object, must implement ConfigData as a sanity check
     * @return The original object, with fields set to the read values from the config file
     */
    public static <T extends ConfigData> T getOrCreate(File configFile, T configData) {
        boolean didExist = configFile.exists();
        
        // If it doesn't exist, create it for the first time
        if (!didExist) {
            ConfigProcessor processor = new ConfigProcessor(configFile, configData, false);
            processor.createConfigFile();
            processor.end();
        }
        
        // Load it back
        ConfigProcessor processor = new ConfigProcessor(configFile, configData, true);
        processor.loadConfig();
        processor.end();
        
        processor = new ConfigProcessor(configFile, configData, false);
        processor.createConfigFile();
        processor.end();
        return configData;
    }
    
    private static class ConfigProcessor {
        File configFile;
        FileWriter writer;
        int lastIndentation = 0;
        
        ArrayList<ConfigData> parsingStack = new ArrayList<>();
        
        ConfigProcessor(File configFile, ConfigData configData, Boolean append) {
            this.configFile = configFile;
            try {
                writer = new FileWriter(configFile, append);
            } catch (IOException e) {
                e.printStackTrace();
            }
            parsingStack.add(configData);
        }
        
        private void loadConfig() {
            try {
                List<String> lines = Files.readAllLines(configFile.toPath());
                
                // Handle each line
                for (String line : lines) {
                    int orig = line.length();
                    int after = line.replaceFirst(" *", "").length();
                    int newIndent = (orig - after) / 4;
                    
                    int diff = newIndent - lastIndentation;
                    while (diff < 0) {
                        ConfigData save = last();
                        parsingStack.remove(save);
                        if (parsingStack.isEmpty()) {
                            parsingStack.add(save);
                        }
                        diff++;
                    }
    
                    lastIndentation = newIndent;
                    
                    if (!line.trim().startsWith("//") && line.trim().length() > 3) {
                        handleLine(line.trim(), last());
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        private void handleLine(String line, ConfigData configData) {
            if (line.endsWith(":")) {
                String fieldName = line.trim().replace(":", "");
                Field found = fieldFromNameAndClass(last().getClass(), fieldName);
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
            Field field = fieldFromNameAndClass(last().getClass(), fieldName);
            
            // If we found a field, set it
            if (field != null) {
                unpackFieldValuePair(configData, field, value);
            }
        }
        
        private void createConfigFile() {
            createConfigFile(0);
        }
        
        private void createConfigFile(int depth) {
            // Add every field to the file
            for (Field field : last().getClass().getFields()) {
                if (isStandardClassType(field.getType())) {
                    appendDefaultField(writer, last(), field, depth);
                } else {
                    try {
                        appendDefaultField(
                            writer,
                            last(),
                            field,
                            depth
                        );
                        parsingStack.add((ConfigData)field.get(last()));
                        createConfigFile(depth + 1);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
            parsingStack.remove(last());
        }
        
        private void appendDefaultField(FileWriter writer, ConfigData type, Field field, int depth) {
            try {
                // Check if the field has a comment annotation
                Comment annotation = field.getAnnotation(Comment.class);
                
                // If it does, write the comment and mark that we need an extra line
                // This keeps the output overall more clean
                boolean doExtraBreak = false;
                if (annotation != null) {
                    String clean = "//" + annotation.value().replace("\n", "\n" + Collections.nCopies(depth, "    ") + "//");
                    writer
                        .append(String.join("", Collections.nCopies(depth, "    ")))
                        .append(clean)
                        .append("\n");
                    doExtraBreak = true;
                }
                
                // Write the pair and extra line if required
                if (isStandardClassType(field.getType())) {
                    writer
                        .append(String.join("", Collections.nCopies(depth, "    ")))
                        .append(field.getName())
                        .append("=")
                        .append(field.get(type).toString())
                        .append("\n");
                } else {
                    writer
                        .append(String.join("", Collections.nCopies(depth, "    ")))
                        .append(field.getName())
                        .append(":\n");
                }
                if (doExtraBreak && isStandardClassType(field.getType())) {
                    writer.append("\n");
                }
            } catch (IOException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        
        private void unpackFieldValuePair(ConfigData object, Field field, String value) {
            // Fairly self explanatory
            // Only wish I could use switch blocks
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
                if (fieldType.isEnum()) {
                    try {
                        // noinspection unchecked,rawtypes
                        field.set(object, Enum.valueOf((Class<Enum>)fieldType, value));
                    } catch (IllegalArgumentException ignored) {
                    }
                    return;
                }
                throw new UnknownFormatConversionException(field.getName() + " was unable to be deserialized (unsupported type \"" + fieldType + "\")");
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        
        public void end() {
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        private boolean isStandardClassType(Class<?> clazz) {
            return clazz.isPrimitive() || clazz.isEnum() || clazz == String.class;
        }
        
        private ConfigData last() {
            return parsingStack.get(parsingStack.size() - 1);
        }
        
        private Field fieldFromNameAndClass(Class<?> clazz, String name) {
            Field[] possible = clazz.getFields();
            for (Field possibleField : possible) {
                if (possibleField.getName().equals(name)) {
                    return possibleField;
                }
            }
            return null;
        }
    }
}
