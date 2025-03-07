package online.kimino.micro.booking.ui.auth

import com.vaadin.flow.component.html.H1
import com.vaadin.flow.component.login.LoginForm
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.notification.NotificationVariant
import com.vaadin.flow.router.*
import com.vaadin.flow.server.auth.AnonymousAllowed
import online.kimino.micro.booking.ui.component.LanguageSelector

@Route("login")
@PageTitle("Login | Booking SaaS")
@AnonymousAllowed
class LoginView(languageSelector: LanguageSelector) : BaseAuthView(languageSelector), BeforeEnterObserver {

    private val loginForm = LoginForm()

    init {
        addClassName("login-view")

        loginForm.isForgotPasswordButtonVisible = false
        loginForm.action = "login"

        val formLayout = createFormLayout()
        addToForm(
            formLayout,
            H1(getTranslation("app.name")),
            loginForm,
            RouterLink(getTranslation("auth.register"), RegisterView::class.java),
            RouterLink(getTranslation("auth.forgot.password"), ForgotPasswordView::class.java)
        )
    }

    override fun beforeEnter(event: BeforeEnterEvent) {
        if (event.location.queryParameters.parameters.containsKey("error")) {
            loginForm.isError = true
            Notification.show(getTranslation("auth.invalid.credentials")).apply {
                position = Notification.Position.MIDDLE
                addThemeVariants(NotificationVariant.LUMO_ERROR)
            }
        }
    }
}