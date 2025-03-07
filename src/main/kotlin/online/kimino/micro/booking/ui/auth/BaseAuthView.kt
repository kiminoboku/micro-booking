package online.kimino.micro.booking.ui.auth

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import online.kimino.micro.booking.ui.component.LanguageSelector

/**
 * Base class for all authentication-related views.
 * Provides a consistent layout with language selection functionality.
 */
abstract class BaseAuthView(languageSelector: LanguageSelector) : VerticalLayout() {

    init {
        setSizeFull()
        isSpacing = false
        isPadding = false

        // Center the content
        justifyContentMode = FlexComponent.JustifyContentMode.CENTER
        alignItems = FlexComponent.Alignment.CENTER

        // Create a header layout with the language selector
        val headerLayout = HorizontalLayout()
        headerLayout.setWidthFull()
        headerLayout.justifyContentMode = FlexComponent.JustifyContentMode.END
        headerLayout.isPadding = true
        headerLayout.height = "60px"
        headerLayout.add(languageSelector)

        // Add the header to the main layout
        add(headerLayout)
    }

    /**
     * Helper method to create a centered form layout
     */
    protected fun createFormLayout(): VerticalLayout {
        val formLayout = VerticalLayout()
        formLayout.width = "100%"
        formLayout.maxWidth = "500px"
        formLayout.isPadding = true
        formLayout.isSpacing = true
        formLayout.defaultHorizontalComponentAlignment = FlexComponent.Alignment.CENTER

        return formLayout
    }

    /**
     * Helper method to add components to the form layout
     */
    protected fun addToForm(formLayout: VerticalLayout, vararg components: Component) {
        formLayout.add(*components)
        add(formLayout)
    }
}