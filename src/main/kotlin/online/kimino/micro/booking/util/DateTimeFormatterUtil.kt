package online.kimino.micro.booking.util

import org.springframework.context.i18n.LocaleContextHolder
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

/**
 * Utility class for formatting dates and times according to the current locale
 */
object DateTimeFormatterUtil {

    /**
     * Format a date and time with the specified pattern and locale
     */
    fun format(
        dateTime: LocalDateTime,
        pattern: String = "yyyy-MM-dd HH:mm",
        locale: Locale = LocaleContextHolder.getLocale()
    ): String {
        val formatter = DateTimeFormatter.ofPattern(pattern, locale)
        return dateTime.format(formatter)
    }

    /**
     * Format a date with the specified pattern and locale
     */
    fun format(
        date: LocalDate,
        pattern: String = "yyyy-MM-dd",
        locale: Locale = LocaleContextHolder.getLocale()
    ): String {
        val formatter = DateTimeFormatter.ofPattern(pattern, locale)
        return date.format(formatter)
    }

    /**
     * Format a date and time using a predefined style (SHORT, MEDIUM, LONG, FULL)
     */
    fun formatStyled(
        dateTime: LocalDateTime,
        dateStyle: FormatStyle = FormatStyle.MEDIUM,
        timeStyle: FormatStyle = FormatStyle.SHORT,
        locale: Locale = LocaleContextHolder.getLocale()
    ): String {
        val dateFormatter = DateTimeFormatter
            .ofLocalizedDate(dateStyle)
            .withLocale(locale)

        val timeFormatter = DateTimeFormatter
            .ofLocalizedTime(timeStyle)
            .withLocale(locale)

        return "${dateTime.format(dateFormatter)} ${dateTime.format(timeFormatter)}"
    }

    /**
     * Format a date using a predefined style (SHORT, MEDIUM, LONG, FULL)
     */
    fun formatStyled(
        date: LocalDate,
        style: FormatStyle = FormatStyle.MEDIUM,
        locale: Locale = LocaleContextHolder.getLocale()
    ): String {
        val formatter = DateTimeFormatter
            .ofLocalizedDate(style)
            .withLocale(locale)

        return date.format(formatter)
    }
}