package dev.silverandro.microconfig;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class MicroConfigCommon {
    static Set<Class<?>> getHandledTypes() {
        Set<Class<?>> ret = new HashSet<>();
        ret.add(Boolean.class);
        ret.add(Character.class);
        ret.add(Byte.class);
        ret.add(Short.class);
        ret.add(Integer.class);
        ret.add(Long.class);
        ret.add(Float.class);
        ret.add(Double.class);
        ret.add(Void.class);
        ret.add(String.class);
        return ret;
    }
    
    /**
     * Small utility method to quickly find a field by name on a class
     *
     * @param clazz The class to do the lookup on
     * @param name  The name of the field to find
     * @return The field if one exists by that name, or null
     */
    static Field getClassField(Class<?> clazz, String name) {
        try {
            return clazz.getField(name);
        } catch (NoSuchFieldException e) {
            return null;
        }
    }
    
    /**
     * Utility method for if a class can be serial-ed directly
     *
     * @param clazz The class type to evaluate
     * @return If the class is a primitive, enum, or String
     */
    static boolean isStandardClassType(Class<?> clazz) {
        return clazz.isPrimitive() || clazz.isEnum() || clazz == String.class;
    }
}
