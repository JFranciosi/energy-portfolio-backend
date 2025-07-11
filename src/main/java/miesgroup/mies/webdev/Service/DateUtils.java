package miesgroup.mies.webdev.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;
import java.util.Locale;

public class DateUtils {

    // Metodo per ottenere il mese come stringa
    public static String getMonthFromDate(Date date) {
        if (date == null) {
            throw new IllegalArgumentException("La data non può essere null");
        }
        // Converte Date in LocalDate
        LocalDate localDate = date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        // Restituisce il mese come stringa
        return localDate.getMonth().toString(); // Esempio: "JANUARY"
    }

    public static String getMonthFromDateLocalized(Date date) {
        LocalDate localDate = date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        return localDate.getMonth().getDisplayName(TextStyle.FULL, Locale.ITALIAN); // Esempio: "gennaio"
    }

    // Metodo per ottenere il mese come numero (1-12)
    public static int getMonthNumberFromDate(Date date) {
        if (date == null) {
            throw new IllegalArgumentException("La data non può essere null");
        }
        // Converte Date in LocalDate
        LocalDate localDate = date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        // Restituisce il mese come numero
        return localDate.getMonthValue(); // Esempio: 1 (Gennaio)
    }

    public static LocalDate getPreviousMonday(LocalDate date) {
        return date.minusWeeks(1).with(DayOfWeek.MONDAY);
    }

    public static LocalDate getPreviousFriday(LocalDate date) {
        return date.with(TemporalAdjusters.previous(DayOfWeek.FRIDAY));
    }

    public static LocalDate getFirstBusinessDayOfPreviousMonth(LocalDate date) {
        // Trova il primo giorno del mese precedente
        LocalDate firstDay = date.minusMonths(1).with(TemporalAdjusters.firstDayOfMonth());

        // Se è sabato, spostalo a lunedì
        if (firstDay.getDayOfWeek() == DayOfWeek.SATURDAY) {
            return firstDay.plusDays(2);
        }
        // Se è domenica, spostalo a lunedì
        else if (firstDay.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return firstDay.plusDays(1);
        }
        return firstDay;
    }

    public static LocalDate getLastBusinessDayOfPreviousMonth(LocalDate date) {
        // Trova l'ultimo giorno del mese precedente
        LocalDate lastDay = date.minusMonths(1).with(TemporalAdjusters.lastDayOfMonth());

        // Se è sabato, torna indietro a venerdì
        if (lastDay.getDayOfWeek() == DayOfWeek.SATURDAY) {
            return lastDay.minusDays(1);
        }
        // Se è domenica, torna indietro a venerdì
        else if (lastDay.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return lastDay.minusDays(2);
        }
        return lastDay;
    }

    public static String getMonthNumber(String mese) {
        if (mese == null || mese.isEmpty()) {
            throw new IllegalArgumentException("Il mese non può essere null o vuoto");
        }
        // Converte il mese in maiuscolo per uniformità
        String meseUpper = mese.toUpperCase(Locale.ITALIAN);
        switch (meseUpper) {
            case "GENNAIO":
                return "01";
            case "FEBBRAIO":
                return "02";
            case "MARZO":
                return "03";
            case "APRILE":
                return "04";
            case "MAGGIO":
                return "05";
            case "GIUGNO":
                return "06";
            case "LUGLIO":
                return "07";
            case "AGOSTO":
                return "08";
            case "SETTEMBRE":
                return "09";
            case "OTTOBRE":
                return "10";
            case "NOVEMBRE":
                return "11";
            case "DICEMBRE":
                return "12";
            default:
                throw new IllegalArgumentException("Mese non valido: " + mese);
        }
    }
}
