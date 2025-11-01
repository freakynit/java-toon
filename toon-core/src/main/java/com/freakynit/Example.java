package com.freakynit;

import com.freakynit.toon.Toon;
import com.freakynit.toon.ToonConfig;

import java.util.*;

public class Example {
    public static void main(String[] args) {
        // Create data
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("username", "Alice");
        data.put("age", 30);
        data.put("tags", Arrays.asList("python", "coding", "llm"));
        data.put("active", true);

        List<Map<String, Object>> invoices = new ArrayList<>();
        invoices.add(createInvoice(1, 250.75, false));
        invoices.add(createInvoice(2, 125.00, true));
        data.put("invoices", invoices);

        // Encode to TOON
        String encoded = Toon.encode(data);
        System.out.println(encoded);

        // Decode from TOON
        Object decoded = Toon.decode(encoded);

        // With custom configuration
        ToonConfig config = new ToonConfig();
        config.setDelimiter("|");
        config.setIndent(4);
        config.setLengthMarker("#");

        String customEncoded = Toon.encode(data, config);
    }

    private static Map<String, Object> createInvoice(int id, double amount, boolean paid) {
        Map<String, Object> invoice = new LinkedHashMap<>();
        invoice.put("id", id);
        invoice.put("amount", amount);
        invoice.put("paid", paid);
        return invoice;
    }
}
