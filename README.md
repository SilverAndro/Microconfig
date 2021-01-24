# Microconfig

---

A tiny config library, packaged for fabric

### Usage
1. Make a class implement ConfigData
2. Use `ConfigClass config = MicroConfig.getOrCreate("config_file_name", new ConfigClass())` wherever

### Supported data types

- Boolean
- Int
- Float
- Double
- String

Everything must be a top level field, public, and be one of those data types

Comments are supported through the `@Comment` annotation, if you need a linebreak use \n

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
  modImplementation 'com.github.P03W:Microconfig:1.0.5'
  include 'com.github.P03W:Microconfig:1.0.5'
}
```
