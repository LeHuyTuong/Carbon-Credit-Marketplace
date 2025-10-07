package com.carbonx.marketcarbon.utils;

import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class CsvUtils {
    private static final CsvMapper mapper = new CsvMapper();
    //read a CSV for a given POJO class
    public static <T> List<T> read(Class<T> clazz, InputStream inputStream) throws IOException {
        CsvSchema schema = mapper.schemaFor(clazz)
                .withHeader()
                .withColumnReordering(true);
        ObjectReader reader = mapper.readerFor(clazz).with(schema);
        return reader.<T>readValues(inputStream).readAll();
    }
}
