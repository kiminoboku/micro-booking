package online.kimino.micro.booking.ui.auth

import com.vaadin.flow.component.html.H1
import com.vaadin.flow.component.html.H2
import com.vaadin.flow.component.html.Paragraph
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.notification.NotificationVariant
import com.vaadin.flow.router.*
import com.vaadin.flow.server.auth.AnonymousAllowed
import online.kimino.micro.booking.service.UserService
import online.kimino.micro.booking.ui.component.LanguageSelector

@Route("verify")
@AnonymousAllowed
class EmailVerificationView(
    private val userService: UserService,
    languageSelector: LanguageSelector
) : BaseAuthView(languageSelector), HasUrlParameter<String>, HasDynamicTitle {

    private val statusText = Paragraph()
    private val loginLink = RouterLink(getTranslation("auth.go.to.login"), LoginView::class.java)

    init {
        addClassName("email-verification-view")

        val formLayout = createFormLayout()

        loginLink.isVisible = false

        addToForm(
            formLayout,
            H1(getTranslation("app.name")),
            H2(getTranslation("auth.email.verification")),
            statusText,
            loginLink
        )
    }

    override fun setParameter(event: BeforeEvent, token: String?) {
        if (token.isNullOrBlank()) {
            showError(getTranslation("verification.token.missing"))
            return
        }

        try {
            val success = userService.verifyUser(token)

            if (success) {
                showSuccess(getTranslation("verification.success"))
            } else {
                showError(getTranslation("verification.invalid"))
            }
        } catch (e: Exception) {
            showError(getTranslation("verification.failed", e.message))
        }
    }

    private fun showError(errorMessage: String) {
        statusText.text = errorMessage
        statusText.element.style.set("color", "var(--lumo-error-text-color)")
        loginLink.isVisible = true

        Notification.show(errorMessage).apply {
            position = Notification.Position.MIDDLE
            addThemeVariants(NotificationVariant.LUMO_ERROR)
        }
    }

    private fun showSuccess(successMessage: String) {
        statusText.text = successMessage
        statusText.element.style.set("color", "var(--lumo-success-text-color)")
        loginLink.isVisible = true

        Notification.show(successMessage).apply {
            position = Notification.Position.MIDDLE
            addThemeVariants(NotificationVariant.LUMO_SUCCESS)
        }
    }

    override fun getPageTitle() = "micro-booking :: ${getTranslation("auth.email.verification")}"
}