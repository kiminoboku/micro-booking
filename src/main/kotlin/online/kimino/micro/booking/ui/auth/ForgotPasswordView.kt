package online.kimino.micro.booking.ui.auth

import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.H1
import com.vaadin.flow.component.html.H2
import com.vaadin.flow.component.html.Paragraph
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.notification.NotificationVariant
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.EmailField
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.router.RouterLink
import com.vaadin.flow.server.auth.AnonymousAllowed
import online.kimino.micro.booking.service.UserService

@Route("forgot-password")
@PageTitle("Forgot Password | Booking SaaS")
@AnonymousAllowed
class ForgotPasswordView(private val userService: UserService) : VerticalLayout() {

    private val email = EmailField("Email")
    private val submitButton = Button("Reset Password")
    private val messageText = Paragraph()
    private val backToLoginLink = RouterLink("Back to Login", LoginView::class.java)

    init {
        addClassName("forgot-password-view")
        setSizeFull()

        // Center the content
        justifyContentMode = FlexComponent.JustifyContentMode.CENTER
        alignItems = FlexComponent.Alignment.CENTER

        messageText.isVisible = false

        email.isRequired = true
        email.placeholder = "Enter your email address"
        email.width = "100%"

        submitButton.addClickListener { resetPassword() }

        val formLayout = VerticalLayout()
        formLayout.width = "100%"
        formLayout.maxWidth = "500px"
        formLayout.isPadding = true
        formLayout.isSpacing = true

        formLayout.add(
            H2("Forgot Password"),
            Paragraph("Enter your email address and we'll send you a link to reset your password."),
            email,
            submitButton,
            messageText,
            backToLoginLink
        )

        add(
            H1("Booking SaaS"),
            formLayout
        )
    }

    private fun resetPassword() {
        if (email.value.isNullOrBlank()) {
            Notification.show("Email is required").apply {
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

                Notification.show("Password reset email sent").apply {
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
            Notification.show("Failed to process request: ${e.message}").apply {
                position = Notification.Position.MIDDLE
                addThemeVariants(NotificationVariant.LUMO_ERROR)
            }
        }
    }
}