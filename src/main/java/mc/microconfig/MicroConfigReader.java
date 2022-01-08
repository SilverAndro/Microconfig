package mc.microconfig;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UnknownFormatConversionException;

import static mc.microconfig.MicroConfigCommon.getClassField;

class MicroConfigReader {
    private final ArrayList<ConfigData> parsingStack = new ArrayList<>();
    private final File configFile;
    private final List<MicroConfigTypeHandler<?>> handlers;
    
    enum State {
        Normal,
        Array
    }
    
    private State state = State.Normal;
    private ArrayList<Object> currentArray = null;
    private Class<?> arrayType = null;
    
    public MicroConfigReader(File configFile, ConfigData data, List<MicroConfigTypeHandler<?>> handlers) throws IOException {
        this.configFile = configFile;
        this.handlers = handlers;
        parsingStack.add(data);
        loadConfig();
    }
    
    private <T extends Class<?>> Object convert(String value, T expected) {
        if (int.class.equals(expected)) {
            return Integer.parseInt(value);
        } else if (float.class.equals(expected)) {
            return Float.parseFloat(value);
        } else if (double.class.equals(expected)) {
            return Double.parseDouble(value);
        } else if (boolean.class.equals(expected)) {
            return Boolean.parseBoolean(value);
        } else if (MicroConfigCommon.getHandledTypes().contains(expected)) {
            return value;
        } else if (expected.isEnum()) {
            //noinspection unchecked,rawtypes
            return Enum.valueOf((Class<Enum>)expected, value);
        } else if (MicroConfigCommon.couldHandle(expected, handlers)) {
            //noinspection unchecked
            MicroConfigTypeHandler<Object> handler = (MicroConfigTypeHandler<Object>)MicroConfigCommon.findHandler(expected, handlers);
            return handler.parse(value);
        } else {
            throw new UnknownFormatConversionException("Unsupported type \"" + expected + "\"");
        }
    }
    
    private void unpackFieldValuePair(ConfigData object, Field field, String value) {
        try {
            field.set(object, convert(value, field.getType()));
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
    
    private void loadConfig() throws IOException {
        List<String> lines = Files.readAllLines(configFile.toPath());
        
        int lastIndentation = 0;
        
        // Handle each line
        for (String line : lines) {
            int orig = line.length();
            int after = line.replaceFirst(" *", "").length();
            int newIndent = (orig - after) / 4;
            
            for (int diff = newIndent - lastIndentation; diff < 0; ++diff) {
                ConfigData save = last();
                parsingStack.remove(save);
                if (parsingStack.isEmpty()) {
                    parsingStack.add(save);
                }
            }
            
            lastIndentation = newIndent;
            
            if (!line.trim().startsWith("//") && line.trim().length() > 0) {
                handleLine(line.trim(), last());
            }
        }
    }
    
    private void handleLine(String line, ConfigData configData) {
        if (state == State.Array) {
            if (line.startsWith("]")) {
                currentArray = null;
                state = State.Normal;
                return;
            }
    
            currentArray.add(convert(line, arrayType));
            return;
        }
        
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
        
        if (line.endsWith("[")) {
            String fieldName = line.trim().replace("[", "");
            Field found = getClassField(last().getClass(), fieldName);
            try {
                if (found != null) {
                    TypeProvider typeProvider = found.getAnnotation(TypeProvider.class);
                    if (typeProvider == null) {
                        throw new IllegalStateException("Cannot handle ArrayList " + found + " without a type annotated with @TypeProvider");
                    }
                    //noinspection unchecked
                    ArrayList<Object> arrayList = (ArrayList<Object>)found.get(last());
                    arrayList.clear();
                    
                    currentArray = arrayList;
                    arrayType = typeProvider.value();
                    state = State.Array;
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
    
    private ConfigData last() {
        return parsingStack.get(parsingStack.size() - 1);
    }
}
