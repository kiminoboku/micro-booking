package online.kimino.micro.booking.util

import org.springframework.context.i18n.LocaleContextHolder
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.*

/**
 * Utility class for formatting numbers and currencies according to the current locale
 */
object NumberFormatterUtil {

    /**
     * Format a number according to the specified locale
     */
    fun formatNumber(
        number: Number,
        minFractionDigits: Int = 0,
        maxFractionDigits: Int = 2,
        groupingUsed: Boolean = true,
        locale: Locale = LocaleContextHolder.getLocale()
    ): String {
        val formatter = NumberFormat.getNumberInstance(locale)
        formatter.minimumFractionDigits = minFractionDigits
        formatter.maximumFractionDigits = maxFractionDigits
        formatter.isGroupingUsed = groupingUsed
        return formatter.format(number)
    }

    /**
     * Format a currency amount according to the specified locale and currency code
     */
    fun formatCurrency(
        amount: BigDecimal,
        currencyCode: String = "USD",
        locale: Locale = LocaleContextHolder.getLocale()
    ): String {
        val formatter = NumberFormat.getCurrencyInstance(locale)
        val currency = Currency.getInstance(currencyCode)
        formatter.currency = currency

        // Scale the amount to match the currency's default fraction digits
        val scaledAmount = amount.setScale(currency.defaultFractionDigits, RoundingMode.HALF_UP)
        return formatter.format(scaledAmount)
    }

    /**
     * Format a percentage according to the specified locale
     */
    fun formatPercent(
        value: Number,
        minFractionDigits: Int = 0,
        maxFractionDigits: Int = 2,
        locale: Locale = LocaleContextHolder.getLocale()
    ): String {
        val formatter = NumberFormat.getPercentInstance(locale)
        formatter.minimumFractionDigits = minFractionDigits
        formatter.maximumFractionDigits = maxFractionDigits
        return formatter.format(value)
    }
}