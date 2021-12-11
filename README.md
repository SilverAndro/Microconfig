# Microconfig

---

A tiny config library, packaged for fabric

### Usage
1. Make a class implement `ConfigData`
2. Use `ConfigClass config = MicroConfig.getOrCreate("config_file_name", new ConfigClass())` wherever

### Supported data types

- Boolean
- Int
- Float
- Double
- String
- Nested Classes

Everything must be a top level field, public, and be one of those data types, nested classes should be `static` and implement `ConfigData` as well.

Comments are supported through the `@Comment` annotation, if you need a linebreak use `\n`

### Installing
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
