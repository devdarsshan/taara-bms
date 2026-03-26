package com.taara.bms.config;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class SecurityErrorWriter {

    private static final Logger log = LoggerFactory.getLogger(SecurityErrorWriter.class);
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public void write(HttpServletResponse response, HttpStatus status, String code, String message) throws IOException {
        if (response.isCommitted()) {
            log.warn("Skipping security error write because response is already committed. status={}, code={}", status, code);
            return;
        }

        response.resetBuffer();
        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(buildJson(status, code, message));
        response.getWriter().flush();
    }

    private String buildJson(HttpStatus status, String code, String message) {
        return "{"
                + "\"timestamp\":\"" + escape(LocalDateTime.now().format(TIMESTAMP_FORMATTER)) + "\","
                + "\"status\":" + status.value() + ","
                + "\"error\":\"" + escape(status.getReasonPhrase()) + "\","
                + "\"code\":\"" + escape(code) + "\","
                + "\"message\":\"" + escape(message == null ? "Authentication failed" : message) + "\","
                + "\"details\":{}"
                + "}";
    }

    private String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }
}
