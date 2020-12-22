# Microconfig

---

A tiny config library, packaged for fabric

### Usage
1. Make a class extend ConfigData
2. Use `ConfigClass config = MicroConfig.getOrCreate("config_file_name", new ConfigClass())` wherever

### Supported data types

- Boolean
- Int
- Float
- Double
- String

Everything must be a top level field, public, and be one of those data types

No, comments arent supported (Idk maybe in the future)