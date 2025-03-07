package online.kimino.micro.booking.ui.auth

import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.H1
import com.vaadin.flow.component.html.H2
import com.vaadin.flow.component.html.Paragraph
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.notification.NotificationVariant
import com.vaadin.flow.component.textfield.EmailField
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.router.RouterLink
import com.vaadin.flow.server.auth.AnonymousAllowed
import online.kimino.micro.booking.service.UserService
import online.kimino.micro.booking.ui.component.LanguageSelector

@Route("forgot-password")
@PageTitle("Forgot Password | Booking SaaS")
@AnonymousAllowed
class ForgotPasswordView(
    private val userService: UserService,
    languageSelector: LanguageSelector
) : BaseAuthView(languageSelector) {

    private val email = EmailField(getTranslation("auth.email"))
    private val submitButton = Button(getTranslation("auth.reset.password"))
    private val messageText = Paragraph()
    private val backToLoginLink = RouterLink(getTranslation("auth.go.to.login"), LoginView::class.java)

    init {
        addClassName("forgot-password-view")

        messageText.isVisible = false

        email.isRequired = true
        email.placeholder = "Enter your email address"
        email.width = "100%"

        submitButton.addClickListener { resetPassword() }

        val formLayout = createFormLayout()

        addToForm(
            formLayout,
            H1(getTranslation("app.name")),
            H2(getTranslation("auth.forgot.password")),
            Paragraph("Enter your email address and we'll send you a link to reset your password."),
            email,
            submitButton,
            messageText,
            backToLoginLink
        )
    }

    private fun resetPassword() {
        if (email.value.isNullOrBlank()) {
            Notification.show(getTranslation("validation.required")).apply {
                position = Notification.Position.MIDDLE
                addThemeVariants(NotificationVariant.LUMO_ERROR)
            }
            return
        }

        try {
            val success = userService.initiatePasswordReset(email.value)

            if (success) {
                // Show success message in place of the form
                email.isVisible = false
                submitButton.isVisible = false

                messageText.text =
                    "If an account exists with email ${email.value}, a password reset link has been sent. Please check your email."
                messageText.isVisible = true

                Notification.show(getTranslation("notification.success")).apply {
                    position = Notification.Position.MIDDLE
                    addThemeVariants(NotificationVariant.LUMO_SUCCESS)
                }
            } else {
                // For security reasons, don't indicate if the email was found or not
                messageText.text =
                    "If an account exists with email ${email.value}, a password reset link has been sent. Please check your email."
                messageText.isVisible = true
            }
        } catch (e: Exception) {
            Notification.show("${getTranslation("notification.failed")}: ${e.message}").apply {
                position = Notification.Position.MIDDLE
                addThemeVariants(NotificationVariant.LUMO_ERROR)
            }
        }
    }
}