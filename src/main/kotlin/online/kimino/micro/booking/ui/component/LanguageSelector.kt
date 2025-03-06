package online.kimino.micro.booking.ui.component

import com.vaadin.flow.component.combobox.ComboBox
import com.vaadin.flow.component.icon.Icon
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.server.VaadinSession
import com.vaadin.flow.spring.annotation.UIScope
import org.springframework.stereotype.Component
import java.util.*

@Component
@UIScope
class LanguageSelector : HorizontalLayout() {

    private val languageSelect: ComboBox<Locale> = ComboBox()

    // List of supported locales
    private val supportedLocales = listOf(
        Locale.ENGLISH,
        Locale.of("pl")
    )

    init {
        this.setWidthFull()
        this.justifyContentMode = FlexComponent.JustifyContentMode.END

        languageSelect.setItems(supportedLocales)
        languageSelect.setItemLabelGenerator { getDisplayLanguage(it) }
        languageSelect.prefixComponent = Icon(VaadinIcon.GLOBE)
        languageSelect.width = "150px"
        languageSelect.value = VaadinSession.getCurrent().locale ?: Locale.ENGLISH

        languageSelect.addValueChangeListener { event ->
            if (event.value != null) {
                VaadinSession.getCurrent().locale = event.value
                // Refresh the page to apply the new locale
                com.vaadin.flow.component.UI.getCurrent().page.reload()
            }
        }

        add(languageSelect)
    }

    private fun getDisplayLanguage(locale: Locale): String {
        return locale.getDisplayLanguage(locale).capitalize()
    }

    private fun String.capitalize(): String {
        return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }
}