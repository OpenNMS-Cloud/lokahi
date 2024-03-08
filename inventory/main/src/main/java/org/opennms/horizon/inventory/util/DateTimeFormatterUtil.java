package org.opennms.horizon.inventory.util;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class DateTimeFormatterUtil {

    public static final String DMMYYYYHHMMSSXXX="M/dd/yyyy HH:mm:ssxxx";
    public static String formatLocalDateTime(LocalDateTime date,String format) {
        if (date == null) {
            throw new IllegalArgumentException("Date cannot be null");
        }
       String dateFormatter = format == null ? DMMYYYYHHMMSSXXX : format;
        return date.format(DateTimeFormatter
            .ofPattern(dateFormatter));
    }
}
