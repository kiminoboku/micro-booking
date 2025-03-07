package online.kimino.micro.booking.config

import org.springframework.context.MessageSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.web.servlet.LocaleResolver
import org.springframework.web.servlet.i18n.SessionLocaleResolver
import java.util.*

@Configuration
class MessageConfig {

    @Bean
    fun messageSource(): MessageSource {
        val messageSource = ResourceBundleMessageSource()
        messageSource.setBasenames("vaadin-i18n/translations")
        messageSource.setDefaultEncoding("UTF-8")
        return messageSource
    }

    @Bean
    fun localeResolver(): LocaleResolver {
        val localeResolver = SessionLocaleResolver()
        localeResolver.setDefaultLocale(Locale.ENGLISH)
        return localeResolver
    }
}