package mc.microconfig;

public interface MicroConfigTypeHandler<T> {
    T parse(String line);
    String write(T value);
}
