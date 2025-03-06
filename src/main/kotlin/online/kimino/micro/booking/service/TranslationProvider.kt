package online.kimino.micro.booking.service

import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.stereotype.Service
import java.text.MessageFormat
import java.util.*

@Service
class TranslationProvider(private val messageSource: MessageSource) {

    /**
     * Get a translated message by key
     */
    fun getTranslation(key: String, locale: Locale = LocaleContextHolder.getLocale()): String {
        return try {
            messageSource.getMessage(key, null, locale)
        } catch (e: Exception) {
            // If the key is not found, return the key itself
            key
        }
    }

    /**
     * Get a translated message with parameter substitution
     */
    fun getTranslation(key: String, params: Array<Any>, locale: Locale = LocaleContextHolder.getLocale()): String {
        return try {
            messageSource.getMessage(key, params, locale)
        } catch (e: Exception) {
            // If the key is not found, try to format the key with params
            try {
                MessageFormat.format(key, *params)
            } catch (e: Exception) {
                key
            }
        }
    }
}