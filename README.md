# java-toon

A Java implementation of the [TOON (Token-Oriented Object Notation)](https://github.com/davidpirogov/toon-llm) specification - an LLM-optimized data serialization format designed to minimize token consumption in large language model interactions.

## Overview

TOON reduces token usage by over 60% compared to JSON through intelligent formatting strategies, streamlined syntax, and adaptive array representations. This Java library provides both a core library for programmatic use and a CLI tool for command-line operations.

A Python implementation is also available at [python-toon](https://github.com/xaviviro/python-toon).

## Project Structure

This is a multi-module Maven project:

- **toon-core**: Core library for encoding/decoding TOON format
- **toon-cli**: Command-line interface wrapper

## Installation

This library is not yet published to Maven Central. To use it, clone the repository and install locally:

```bash
git clone https://github.com/freakynit/java-toon.git
cd java-toon
mvn clean install
```

Then add the dependency to your project:

```xml
<dependency>
    <groupId>com.freakynit</groupId>
    <artifactId>toon-core</artifactId>
    <version>1.0.1</version>
</dependency>
```

## Library Usage

### Basic Encoding and Decoding

```java
import com.freakynit.toon.Toon;
import java.util.*;

// Create data
Map<String, Object> data = new LinkedHashMap<>();
data.put("username", "Alice");
data.put("age", 30);
data.put("tags", Arrays.asList("python", "coding", "llm"));
data.put("active", true);

// Encode to TOON
String encoded = Toon.encode(data);
System.out.println(encoded);

// Decode from TOON
Object decoded = Toon.decode(encoded);
```

### Custom Configuration

```java
import com.freakynit.toon.ToonConfig;

// Create custom configuration
ToonConfig config = new ToonConfig();
config.setDelimiter("|");        // Use pipe instead of comma
config.setIndent(4);              // Use 4 spaces for indentation
config.setLengthMarker("#");      // Add length markers to arrays

// Encode with custom config
String customEncoded = Toon.encode(data, config);

// Decode with custom config
Object customDecoded = Toon.decode(customEncoded, config);
```

### Working with Complex Data

```java
// Create nested data structure
Map<String, Object> user = new LinkedHashMap<>();
user.put("username", "Alice");
user.put("age", 30);

List<Map<String, Object>> invoices = new ArrayList<>();
invoices.add(Map.of("id", 1, "amount", 250.75, "paid", false));
invoices.add(Map.of("id", 2, "amount", 125.00, "paid", true));
user.put("invoices", invoices);

// Encode - TOON automatically uses tabular format for homogeneous object arrays
String encoded = Toon.encode(user);
```

## CLI Usage

After installation, build the shaded JAR:

```bash
mvn clean package
```

The executable JAR will be located at `toon-cli/target/toon-cli-1.0.1.jar`.

### Encode JSON to TOON

```bash
# From file
java -jar toon-cli/target/toon-cli-1.0.1.jar encode input.json -o output.toon

# From stdin
echo '{"name":"Alice","age":30}' | java -jar toon-cli/target/toon-cli-1.0.1.jar encode

# With custom options
java -jar toon-cli/target/toon-cli-1.0.1.jar encode input.json -i 4 -d "|" -m "#"
```

### Decode TOON to JSON

```bash
# From file with pretty printing
java -jar toon-cli/target/toon-cli-1.0.1.jar decode input.toon --pretty

# From stdin to file
cat input.toon | java -jar toon-cli/target/toon-cli-1.0.1.jar decode -o output.json
```

### CLI Options

**Encode options:**
- `-o, --output <file>`: Output file (default: stdout)
- `-i, --indent <n>`: Indentation spaces (default: 2)
- `-d, --delimiter <char>`: Array delimiter (default: ,)
- `-m, --marker <prefix>`: Length marker prefix (default: none)

**Decode options:**
- `-o, --output <file>`: Output file (default: stdout)
- `-p, --pretty`: Pretty print JSON output

## Features

- **Three array format strategies**: Inline, tabular, or list layouts automatically selected for optimal token efficiency
- **Smart string quoting**: Only quotes strings when necessary
- **Type support**: Handles primitives, strings, numbers, booleans, dates, maps, and lists
- **Configurable formatting**: Customizable delimiters, indentation, and length markers
- **Zero dependencies** (core library)
- **Java 11+** compatible

## Requirements

- Java 11 or higher
- Maven 3.6+

## License

MIT License - see [LICENSE](LICENSE) file for details.

## Specification

This implementation follows the [TOON specification](https://github.com/davidpirogov/toon-llm/tree/main/specification).

## Author

[@freakynit](https://github.com/freakynit)
