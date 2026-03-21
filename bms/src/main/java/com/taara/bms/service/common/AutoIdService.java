package com.taara.bms.service.common;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

@Service
public class AutoIdService {

    private final JdbcClient jdbcClient;

    public AutoIdService(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public String next(AutoIdSequence sequence) {
        Long value = jdbcClient.sql("select nextval('" + sequence.sequenceName() + "')")
                .query(Long.class)
                .single();
        return "%s-%03d".formatted(sequence.prefix(), value);
    }
}
