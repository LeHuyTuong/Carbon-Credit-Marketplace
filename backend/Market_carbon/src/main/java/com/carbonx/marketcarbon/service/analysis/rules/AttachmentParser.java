package com.carbonx.marketcarbon.service.analysis.rules;

import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AttachmentParser {

    // Giả sử CSV có header: period,total_energy,license_plate
    public List<Map<String,Object>> parseCsv(String path) {
        List<Map<String,Object>> rows = new ArrayList<>();
        try {
            var lines = Files.readAllLines(Path.of(path));
            if (lines.isEmpty()) return rows;
            String[] headers = lines.get(0).split(",", -1);
            for (int i=1; i<lines.size(); i++){
                String[] parts = lines.get(i).split(",", -1);
                Map<String,Object> row = new HashMap<>();
                for (int c=0; c<headers.length && c<parts.length; c++){
                    String key = headers[c].trim();
                    String val = parts[c].trim();
                    if ("total_energy".equalsIgnoreCase(key)) {
                        try { row.put("total_energy", Double.valueOf(val)); }
                        catch (Exception e){ row.put("total_energy", null); }
                    } else {
                        row.put(key, val);
                    }
                }
                rows.add(row);
            }
            return rows;
        } catch (Exception e){
            throw new RuntimeException("Parse CSV error: "+e.getMessage(), e);
        }
    }
}
