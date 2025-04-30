package com.ceiba.cubillos.libreria.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DateUtils {

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * @param startDate
     * @param businessDaysToAdd
     * @return
     */
    public static LocalDate addBusinessDaysSkippingWeekends(LocalDate startDate, int businessDaysToAdd) {
        if (businessDaysToAdd <= 0) {
            return startDate;
        }

        LocalDate resultDate = startDate;
        int daysAdded = 0;
        while (daysAdded < businessDaysToAdd) {
            resultDate = resultDate.plusDays(1);
            if (!(resultDate.getDayOfWeek() == DayOfWeek.SATURDAY
                    || resultDate.getDayOfWeek() == DayOfWeek.SUNDAY)) {
                daysAdded++;
            }
        }
        return resultDate;
    }

    public static String formatLocalDate(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.format(DATE_FORMATTER);
    }
}
