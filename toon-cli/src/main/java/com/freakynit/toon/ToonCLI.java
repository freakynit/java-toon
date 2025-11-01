package com.freakynit.toon;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.cli.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ToonCLI {
    private static final String VERSION = "0.9.0-beta1";

    public static void main(String[] args) {
        Options options = new Options();
        options.addOption("h", "help", false, "Show help message");
        options.addOption("v", "version", false, "Show version");

        CommandLineParser parser = new DefaultParser();

        try {
            CommandLine cmd = parser.parse(options, args, true);

            if (cmd.hasOption("version")) {
                System.out.println("TOON LLM Java v" + VERSION);
                return;
            }

            if (cmd.hasOption("help") || args.length == 0) {
                printHelp();
                return;
            }

            String command = args[0];
            String[] commandArgs = new String[args.length - 1];
            System.arraycopy(args, 1, commandArgs, 0, args.length - 1);

            if ("encode".equals(command)) {
                handleEncode(commandArgs);
            } else if ("decode".equals(command)) {
                handleDecode(commandArgs);
            } else {
                System.err.println("Unknown command: " + command);
                printHelp();
                System.exit(1);
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void handleEncode(String[] args) throws Exception {
        Options options = new Options();
        options.addOption("o", "output", true, "Output file");
        options.addOption("i", "indent", true, "Indentation spaces (default: 2)");
        options.addOption("d", "delimiter", true, "Array delimiter (default: ,)");
        options.addOption("m", "marker", true, "Length marker prefix (default: none)");
        options.addOption("h", "help", false, "Show help");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption("help")) {
            printEncodeHelp();
            return;
        }

        ToonConfig config = new ToonConfig();
        if (cmd.hasOption("indent")) {
            config.setIndent(Integer.parseInt(cmd.getOptionValue("indent")));
        }
        if (cmd.hasOption("delimiter")) {
            config.setDelimiter(cmd.getOptionValue("delimiter"));
        }
        if (cmd.hasOption("marker")) {
            config.setLengthMarker(cmd.getOptionValue("marker"));
        }

        String input;
        String[] remaining = cmd.getArgs();
        if (remaining.length > 0) {
            input = new String(Files.readAllBytes(Paths.get(remaining[0])));
        } else {
            input = readStdin();
        }

        Gson gson = new Gson();
        Object data = gson.fromJson(input, Object.class);

        String encoded = Toon.encode(data, config);

        if (cmd.hasOption("output")) {
            Files.write(Paths.get(cmd.getOptionValue("output")), encoded.getBytes());
        } else {
            System.out.println(encoded);
        }
    }

    private static void handleDecode(String[] args) throws Exception {
        Options options = new Options();
        options.addOption("o", "output", true, "Output file");
        options.addOption("p", "pretty", false, "Pretty print JSON");
        options.addOption("h", "help", false, "Show help");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption("help")) {
            printDecodeHelp();
            return;
        }

        String input;
        String[] remaining = cmd.getArgs();
        if (remaining.length > 0) {
            input = new String(Files.readAllBytes(Paths.get(remaining[0])));
        } else {
            input = readStdin();
        }

        Object decoded = Toon.decode(input);

        Gson gson;
        if (cmd.hasOption("pretty")) {
            gson = new GsonBuilder().setPrettyPrinting().create();
        } else {
            gson = new Gson();
        }

        String json = gson.toJson(decoded);

        if (cmd.hasOption("output")) {
            Files.write(Paths.get(cmd.getOptionValue("output")), json.getBytes());
        } else {
            System.out.println(json);
        }
    }

    private static String readStdin() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append('\n');
        }
        return sb.toString();
    }

    private static void printHelp() {
        System.out.println("TOON LLM - Token-Oriented Object Notation");
        System.out.println("make sure build first using `mvn clean package`");
        System.out.println("\nUsage: java -jar toon-cli/target/toon-cli-1.0.1.jar <command> [options]");
        System.out.println("\nCommands:");
        System.out.println("  encode    Encode JSON to TOON format");
        System.out.println("  decode    Decode TOON to JSON format");
        System.out.println("\nOptions:");
        System.out.println("  -h, --help       Show help message");
        System.out.println("  -v, --version    Show version");
        System.out.println("\nExamples:");
        System.out.println("  java -jar toon-cli/target/toon-cli-1.0.1.jar encode input.json -o output.toon");
        System.out.println("  java -jar toon-cli/target/toon-cli-1.0.1.jar decode input.toon --pretty");
        System.out.println("  echo '{\"name\":\"Alice\"}' | java -jar toon-cli/target/toon-cli-1.0.1.jar encode");
    }

    private static void printEncodeHelp() {
        System.out.println("Encode JSON to TOON format");
        System.out.println("make sure build first using `mvn clean package`");
        System.out.println("\nUsage: java -jar toon-cli/target/toon-cli-1.0.1.jar encode [file] [options]");
        System.out.println("\nOptions:");
        System.out.println("  -o, --output <file>       Output file (default: stdout)");
        System.out.println("  -i, --indent <n>          Indentation spaces (default: 2)");
        System.out.println("  -d, --delimiter <char>    Array delimiter (default: ,)");
        System.out.println("  -m, --marker <prefix>     Length marker prefix (default: none)");
        System.out.println("  -h, --help                Show this help");
    }

    private static void printDecodeHelp() {
        System.out.println("Decode TOON to JSON format");
        System.out.println("make sure build first using `mvn clean package`");
        System.out.println("\nUsage: java -jar toon-cli/target/toon-cli-1.0.1.jar decode [file] [options]");
        System.out.println("\nOptions:");
        System.out.println("  -o, --output <file>    Output file (default: stdout)");
        System.out.println("  -p, --pretty           Pretty print JSON");
        System.out.println("  -h, --help             Show this help");
    }
}
