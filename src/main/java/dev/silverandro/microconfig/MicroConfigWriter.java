package dev.silverandro.microconfig;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;

class MicroConfigWriter {
    private final ArrayList<ConfigData> parsingStack = new ArrayList<>();
    private final FileWriter writer;
    
    MicroConfigWriter(FileWriter writer, ConfigData data) throws IOException {
        this.writer = writer;
        parsingStack.add(data);
        createConfigFile(0);
        writer.close();
    }
    
    private void createConfigFile(int depth) throws IOException {
        for (Field field : last().getClass().getFields()) {
            Class<?> type = field.getType();
            if (MicroConfigCommon.isStandardClassType(type)) {
                appendStandardField(field, depth);
            } else if (type == ArrayList.class) {
                appendArrayListField(field, depth);
            } else if (ConfigData.class.isAssignableFrom(type)) {
                try {
                    appendNestedClass(field, depth);
                    parsingStack.add((ConfigData)field.get(last()));
                    createConfigFile(depth + 1);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            } else {
                throw new IllegalStateException("Don't know how to handle field " + field.getName() + " with type " + type);
            }
        }
        parsingStack.remove(last());
    }
    
    /**
     * Writes the comment if appropriate and returns if a line break is needed
     */
    private boolean writeComment(Field field, String indent) throws IOException {
        // Check if the field has a comment annotation
        Comment annotation = field.getAnnotation(Comment.class);
        
        // If it does, write the comment and mark that we need an extra line
        // This keeps the output overall more clean
        boolean doExtraBreak = false;
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
        
        return doExtraBreak;
    }
    
    private void appendStandardField(Field field, int depth) throws IOException {
        try {
            String indent = String.join("", Collections.nCopies(depth, "    "));
            boolean needsExtraLine = writeComment(field, indent);
            
            // Write the pair and extra line if required
            writer
                .append(indent)
                .append(field.getName())
                .append("=").append(field.get(last()).toString())
                .append("\n");
            
            if (needsExtraLine) {
                writer.append(indent).append("\n");
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
    
    private void appendArrayListField(Field field, int depth) throws IOException {
        try {
            String indent = String.join("", Collections.nCopies(depth, "    "));
            String nextIndent = String.join("", Collections.nCopies(depth + 1, "    "));
            writeComment(field, indent);
            writer
                .append(indent)
                .append(field.getName())
                .append("[\n");
            
            ArrayList<?> arr = (ArrayList<?>)field.get(last());
            arr.forEach(o -> {
                try {
                    writer.append(nextIndent).append(o.toString()).append("\n");
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            });
            
            writer
                .append(indent)
                .append("]\n");
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
    
    private void appendNestedClass(Field field, int depth) throws IOException {
        String indent = String.join("", Collections.nCopies(depth, "    "));
        writeComment(field, indent);
        writer
            .append(indent)
            .append(field.getName())
            .append(":\n");
    }
    
    private ConfigData last() {
        return parsingStack.get(parsingStack.size() - 1);
    }
}
