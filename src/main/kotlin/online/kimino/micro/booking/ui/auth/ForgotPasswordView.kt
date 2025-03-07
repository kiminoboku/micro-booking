package online.kimino.micro.booking.ui.auth

import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.H1
import com.vaadin.flow.component.html.H2
import com.vaadin.flow.component.html.Paragraph
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.notification.NotificationVariant
import com.vaadin.flow.component.textfield.EmailField
import com.vaadin.flow.router.HasDynamicTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.router.RouterLink
import com.vaadin.flow.server.auth.AnonymousAllowed
import online.kimino.micro.booking.service.UserService
import online.kimino.micro.booking.ui.component.LanguageSelector

@Route("forgot-password")
@AnonymousAllowed
class ForgotPasswordView(
    private val userService: UserService,
    languageSelector: LanguageSelector
) : BaseAuthView(languageSelector), HasDynamicTitle {

    private val email = EmailField(getTranslation("auth.email"))
    private val submitButton = Button(getTranslation("auth.reset.password"))
    private val messageText = Paragraph()
    private val backToLoginLink = RouterLink(getTranslation("auth.go.to.login"), LoginView::class.java)

    init {
        addClassName("forgot-password-view")

        messageText.isVisible = false

        email.isRequired = true
        email.placeholder = getTranslation("auth.email")
        email.width = "100%"

        submitButton.addClickListener { resetPassword() }

        val formLayout = createFormLayout()

        addToForm(
            formLayout,
            H1(getTranslation("app.name")),
            H2(getTranslation("auth.forgot.password")),
            Paragraph(getTranslation("auth.forgot.password.instruction")),
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

                messageText.text = getTranslation("auth.reset.email.sent", arrayOf(email.value))
                messageText.isVisible = true

                Notification.show(getTranslation("notification.success")).apply {
                    position = Notification.Position.MIDDLE
                    addThemeVariants(NotificationVariant.LUMO_SUCCESS)
                }
            } else {
                // For security reasons, don't indicate if the email was found or not
                messageText.text = getTranslation("auth.reset.email.sent", arrayOf(email.value))
                messageText.isVisible = true
            }
        } catch (e: Exception) {
            Notification.show("${getTranslation("notification.failed")}: ${e.message}").apply {
                position = Notification.Position.MIDDLE
                addThemeVariants(NotificationVariant.LUMO_ERROR)
            }
        }
    }

    override fun getPageTitle() = "micro-booking :: ${getTranslation("auth.forgot.password")}"
}