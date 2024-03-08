package org.opennms.horizon.server.utils;

import org.springframework.stereotype.Component;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.TimeZone;

@Component
public class DateTimeUtil {

    public static final String D_MM_YYYY_HH_MM_SS_SSS="M/dd/yyyy HH:mm:ss.SSS";
    public static String convertAndFormatLongDate(Long longDate,String format) {
        if (longDate == null) {
            throw new IllegalArgumentException("Date cannot be null");
        }

        String dateFormat = format == null ? D_MM_YYYY_HH_MM_SS_SSS : format;
        DateFormat dateFormatter = new SimpleDateFormat(dateFormat);
        Date dateTime= new Date(longDate.longValue());
        return dateFormatter.format(dateTime);
    }
}

