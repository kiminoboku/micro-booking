package online.kimino.micro.booking.ui.auth

import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.H1
import com.vaadin.flow.component.html.H2
import com.vaadin.flow.component.html.Paragraph
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.notification.NotificationVariant
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.PasswordField
import com.vaadin.flow.router.*
import com.vaadin.flow.server.auth.AnonymousAllowed
import online.kimino.micro.booking.service.UserService

@Route("reset-password")
@PageTitle("Reset Password | Booking SaaS")
@AnonymousAllowed
class PasswordResetView(private val userService: UserService) : VerticalLayout(), HasUrlParameter<String> {

    private val newPassword = PasswordField("New Password")
    private val confirmPassword = PasswordField("Confirm Password")
    private val submitButton = Button("Reset Password")
    private val messageText = Paragraph()
    private val backToLoginLink = RouterLink("Back to Login", LoginView::class.java)

    private var token: String? = null

    init {
        addClassName("reset-password-view")
        setSizeFull()

        // Center the content
        justifyContentMode = FlexComponent.JustifyContentMode.CENTER
        alignItems = FlexComponent.Alignment.CENTER

        messageText.isVisible = false

        newPassword.isRequired = true
        newPassword.width = "100%"

        confirmPassword.isRequired = true
        confirmPassword.width = "100%"

        submitButton.addClickListener { resetPassword() }

        val formLayout = VerticalLayout()
        formLayout.width = "100%"
        formLayout.maxWidth = "500px"
        formLayout.isPadding = true
        formLayout.isSpacing = true

        formLayout.add(
            H2("Reset Password"),
            Paragraph("Please enter your new password."),
            newPassword,
            confirmPassword,
            submitButton,
            messageText,
            backToLoginLink
        )

        add(
            H1("Booking SaaS"),
            formLayout
        )
    }

    override fun setParameter(event: BeforeEvent, token: String?) {
        this.token = token

        if (token.isNullOrBlank()) {
            // Token is missing, show error and disable form
            showError("Invalid or missing reset token. Please request a new password reset link.")
        }
    }

    private fun resetPassword() {
        if (token.isNullOrBlank()) {
            showError("Invalid reset token. Please request a new password reset link.")
            return
        }

        if (newPassword.value.isNullOrBlank() || confirmPassword.value.isNullOrBlank()) {
            Notification.show("Both password fields are required").apply {
                position = Notification.Position.MIDDLE
                addThemeVariants(NotificationVariant.LUMO_ERROR)
            }
            return
        }

        if (newPassword.value != confirmPassword.value) {
            Notification.show("Passwords do not match").apply {
                position = Notification.Position.MIDDLE
                addThemeVariants(NotificationVariant.LUMO_ERROR)
            }
            return
        }

        if (newPassword.value.length < 8) {
            Notification.show("Password must be at least 8 characters long").apply {
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
                showError("Invalid or expired token. Please request a new password reset link.")
            }
        } catch (e: Exception) {
            showError("Failed to reset password: ${e.message}")
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
}