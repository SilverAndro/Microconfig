# Microconfig

---

A tiny config library, packaged for fabric

## Installing
```
repositories {
  ...
  exclusiveContent {
        forRepository {
            maven {
                name = "SilverAndro Maven"
                url = uri("https://maven.silverandro.dev")
            }
        }
        filter {
            includeGroup("dev.silverandro")
        }
    }
}
```

```
dependencies {
  ...
  modImplementation 'dev.silverandro:microconfig:<VERSION>'
  include 'dev.silverandro:microconfig:<VERSION>'
}
```

## Usage
1. Make a class implement `ConfigData`
2. Use `ConfigClass config = MicroConfig.getOrCreate("config_file_name", new ConfigClass())` wherever

### Supported data types

- Boolean
- Character
- Byte
- Short
- Integer
- Long
- Float
- Double
- String
- Enum
- Nested Classes

Everything must be a top level field, public, and be one of those data types.

#### Special Rules:
- Nested classes must be `static` and implement `ConfigData`
- Comments are supported through the `@Comment` annotation, and use `\n` for extra lines