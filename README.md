# Microconfig

---

A tiny config library, packaged for fabric

## Installing
```
repositories {
  ...
  maven { url 'https://jitpack.io' }
}
```

```
dependencies {
  ...
  modImplementation 'com.github.P03W:Microconfig:<VERSION>'
  include 'com.github.P03W:Microconfig:<VERSION>'
}
```

## Usage
1. Make a class implement `ConfigData`
2. Use `ConfigClass config = MicroConfig.getOrCreate("config_file_name", new ConfigClass())` wherever

### Supported data types

- Boolean
- Int
- Float
- Double
- String
- Custom Handled types
- ArrayList of the above types
- Nested Classes

Everything must be a top level field, public, and be one of those data types.

#### Special Rules:
- Nested classes must be `static` and implement `ConfigData`
- `ArrayList`s must be annotated with `@TypeProvider` which tells microconfig what type it contains
- Comments are supported through the `@Comment` annotation, and use `\n` for extra lines

#### Handling custom types

To handle custom types, you need to create a class (or more) that implements `MicroConfigTypeHandler` and annotate it with `@TypeProvider` to tell micro config what type it handles.
You then change your code slightly, instead calling `new MicroConfig` and passing a `List` of all handlers.

**The handler will only ever receive a single line, so you cannot use newlines in your output, or it will break.**

Then, call `getOrCreateWithHandlers` to read/write the config using the passed handlers.

## Why does the source not match the bytecode? (2.2.0 and above)

This is because the library is run through proguard to compress it significantly (roughly 6kb removed).
All public API is left intact so it shouldn't cause any issues.
If you find a meaningful problem with this, please open an issue
