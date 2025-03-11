package online.kimino.micro.booking.ui.auth

import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.H1
import com.vaadin.flow.component.html.H2
import com.vaadin.flow.component.html.Paragraph
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.notification.NotificationVariant
import com.vaadin.flow.component.textfield.PasswordField
import com.vaadin.flow.router.*
import com.vaadin.flow.server.auth.AnonymousAllowed
import io.github.oshai.kotlinlogging.KotlinLogging
import online.kimino.micro.booking.service.UserService
import online.kimino.micro.booking.ui.component.LanguageSelector

@Route("reset-password")
@AnonymousAllowed
class PasswordResetView(
    private val userService: UserService,
    languageSelector: LanguageSelector
) : BaseAuthView(languageSelector), HasUrlParameter<String>, HasDynamicTitle {

    private val logger = KotlinLogging.logger {}
    private val newPassword = PasswordField(getTranslation("auth.new.password"))
    private val confirmPassword = PasswordField(getTranslation("auth.confirm.password"))
    private val submitButton = Button(getTranslation("auth.reset.password"))
    private val messageText = Paragraph()
    private val backToLoginLink = RouterLink(getTranslation("auth.go.to.login"), LoginView::class.java)

    private var token: String? = null

    init {
        addClassName("reset-password-view")

        messageText.isVisible = false

        newPassword.isRequired = true
        newPassword.width = "100%"

        confirmPassword.isRequired = true
        confirmPassword.width = "100%"

        submitButton.addClickListener { resetPassword() }

        val formLayout = createFormLayout()

        addToForm(
            formLayout,
            H1(getTranslation("app.name")),
            H2(getTranslation("auth.reset.password")),
            Paragraph("Please enter your new password."),
            newPassword,
            confirmPassword,
            submitButton,
            messageText,
            backToLoginLink
        )
    }

    override fun setParameter(event: BeforeEvent, token: String?) {
        this.token = token

        if (token.isNullOrBlank()) {
            // Token is missing, show error and disable form
            showError(getTranslation("auth.invalid.password.reset.token"))
        }
    }

    private fun resetPassword() {
        if (token.isNullOrBlank()) {
            showError(getTranslation("auth.invalid.password.reset.token"))
            return
        }

        if (newPassword.value.isNullOrBlank() || confirmPassword.value.isNullOrBlank()) {
            Notification.show(getTranslation("validation.required")).apply {
                position = Notification.Position.MIDDLE
                addThemeVariants(NotificationVariant.LUMO_ERROR)
            }
            return
        }

        if (newPassword.value != confirmPassword.value) {
            Notification.show(getTranslation("validation.passwords.match")).apply {
                position = Notification.Position.MIDDLE
                addThemeVariants(NotificationVariant.LUMO_ERROR)
            }
            return
        }

        if (newPassword.value.length < 8) {
            Notification.show(getTranslation("validation.password.length")).apply {
                position = Notification.Position.MIDDLE
                addThemeVariants(NotificationVariant.LUMO_ERROR)
            }
            return
        }

        try {
            val success = userService.resetPassword(token!!, newPassword.value)

            if (success) {
                // Show success message and redirect to login
                showSuccess("Your password has been reset successfully. You can now log in with your new password.")

                // Redirect to login page after 3 seconds
                ui.ifPresent { ui ->
                    ui.page.executeJs("setTimeout(function() { window.location.href = 'login'; }, 3000);")
                }
            } else {
                showError(getTranslation("auth.invalid.password.reset.token"))
            }
        } catch (e: Exception) {
            showError("${getTranslation("notification.failed")}: ${e.message}")
            logger.warn(e, { "Error when resetting password" })
        }
    }

    private fun showError(errorMessage: String) {
        newPassword.isVisible = false
        confirmPassword.isVisible = false
        submitButton.isVisible = false

        messageText.text = errorMessage
        messageText.element.style.set("color", "var(--lumo-error-text-color)")
        messageText.isVisible = true

        Notification.show(errorMessage).apply {
            position = Notification.Position.MIDDLE
            addThemeVariants(NotificationVariant.LUMO_ERROR)
        }
    }

    private fun showSuccess(successMessage: String) {
        newPassword.isVisible = false
        confirmPassword.isVisible = false
        submitButton.isVisible = false

        messageText.text = successMessage
        messageText.element.style.set("color", "var(--lumo-success-text-color)")
        messageText.isVisible = true

        Notification.show(successMessage).apply {
            position = Notification.Position.MIDDLE
            addThemeVariants(NotificationVariant.LUMO_SUCCESS)
        }
    }

    override fun getPageTitle() = "micro-booking :: ${getTranslation("auth.reset.password")}"
}