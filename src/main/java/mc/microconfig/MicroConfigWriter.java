package mc.microconfig;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static mc.microconfig.MicroConfigCommon.couldHandle;
import static mc.microconfig.MicroConfigCommon.isStandardClassType;

class MicroConfigWriter {
    private final ArrayList<ConfigData> parsingStack = new ArrayList<>();
    private final FileWriter writer;
    private final List<MicroConfigTypeHandler<?>> handlers;
    
    MicroConfigWriter(FileWriter writer, ConfigData data, List<MicroConfigTypeHandler<?>> handlers) throws IOException {
        this.writer = writer;
        this.handlers = handlers;
        parsingStack.add(data);
        createConfigFile(0);
        writer.close();
    }
    
    private void createConfigFile(int depth) throws IOException {
        for (Field field : last().getClass().getFields()) {
            Class<?> type = field.getType();
            if (isStandardClassType(type)) {
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
            } else if (MicroConfigCommon.couldHandle(type, handlers)) {
                //noinspection unchecked
                MicroConfigTypeHandler<Object> handler = (MicroConfigTypeHandler<Object>)MicroConfigCommon.findHandler(type, handlers);
                writeHandled(field, depth, handler);
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
    
    private void writeHandled(Field field, int depth, MicroConfigTypeHandler<Object> handler) throws IOException {
        try {
            String indent = String.join("", Collections.nCopies(depth, "    "));
            boolean needsExtraLine = writeComment(field, indent);
            
            writer
                .append(indent)
                .append(field.getName())
                .append("=").append(handler.write(field.get(last())))
                .append("\n");
            
            if (needsExtraLine) {
                writer.append(indent).append("\n");
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            System.exit(-1);
        }
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
                    if (couldHandle(o.getClass(), handlers)) {
                        //noinspection unchecked
                        MicroConfigTypeHandler<Object> handler = (MicroConfigTypeHandler<Object>)MicroConfigCommon.findHandler(o.getClass(), handlers);
                        writer.append(nextIndent).append(handler.write(o)).append("\n");
                    } else {
                        writer.append(nextIndent).append(o.toString()).append("\n");
                    }
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
