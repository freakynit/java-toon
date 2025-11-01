package com.freakynit.toon;

public class Toon {
    public static String encode(Object data) {
        return new ToonEncoder().encode(data);
    }

    public static String encode(Object data, ToonConfig config) {
        return new ToonEncoder(config).encode(data);
    }

    public static Object decode(String toon) {
        return new ToonDecoder().decode(toon);
    }

    public static Object decode(String toon, ToonConfig config) {
        return new ToonDecoder(config).decode(toon);
    }
}
