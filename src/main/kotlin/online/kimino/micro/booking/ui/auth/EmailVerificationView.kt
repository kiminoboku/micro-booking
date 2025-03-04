package online.kimino.micro.booking.ui.auth

import com.vaadin.flow.component.html.H1
import com.vaadin.flow.component.html.H2
import com.vaadin.flow.component.html.Paragraph
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.notification.NotificationVariant
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.*
import com.vaadin.flow.server.auth.AnonymousAllowed
import online.kimino.micro.booking.service.UserService

@Route("verify")
@PageTitle("Verify Email | Booking SaaS")
@AnonymousAllowed
class EmailVerificationView(private val userService: UserService) : VerticalLayout(), HasUrlParameter<String> {

    private val statusText = Paragraph()
    private val loginLink = RouterLink("Go to Login", LoginView::class.java)

    init {
        addClassName("email-verification-view")
        setSizeFull()

        // Center the content
        justifyContentMode = FlexComponent.JustifyContentMode.CENTER
        alignItems = FlexComponent.Alignment.CENTER

        val contentLayout = VerticalLayout()
        contentLayout.width = "100%"
        contentLayout.maxWidth = "500px"
        contentLayout.isPadding = true
        contentLayout.isSpacing = true
        contentLayout.defaultHorizontalComponentAlignment = FlexComponent.Alignment.CENTER

        contentLayout.add(
            H2("Email Verification"),
            statusText,
            loginLink
        )

        loginLink.isVisible = false

        add(
            H1("Booking SaaS"),
            contentLayout
        )
    }

    override fun setParameter(event: BeforeEvent, token: String?) {
        if (token.isNullOrBlank()) {
            showError("Invalid verification link. The token is missing.")
            return
        }

        try {
            val success = userService.verifyUser(token)

            if (success) {
                showSuccess("Your email has been successfully verified. You can now log in to your account.")
            } else {
                showError("Invalid or expired verification link. Please request a new verification email.")
            }
        } catch (e: Exception) {
            showError("Failed to verify email: ${e.message}")
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
}