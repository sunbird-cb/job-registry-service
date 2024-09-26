package com.tarento.jobservice.util;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ObjectMapperSingleton {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private ObjectMapperSingleton() {
        // Private constructor to prevent instantiation
    }

    public static ObjectMapper getInstance() {
        return objectMapper;
    }
}
