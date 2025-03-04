package online.kimino.micro.booking.ui.auth

import com.vaadin.flow.component.html.H1
import com.vaadin.flow.component.login.LoginForm
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.*
import com.vaadin.flow.server.auth.AnonymousAllowed

@Route("login")
@PageTitle("Login | Booking SaaS")
@AnonymousAllowed
class LoginView : VerticalLayout(), BeforeEnterObserver {

    private val loginForm = LoginForm()

    init {
        addClassName("login-view")
        setSizeFull()

        // Center the content
        justifyContentMode = FlexComponent.JustifyContentMode.CENTER
        alignItems = FlexComponent.Alignment.CENTER

        loginForm.action = "login"

        add(
            H1("Booking SaaS"),
            loginForm,
            RouterLink("Register", RegisterView::class.java),
            RouterLink("Forgot Password", ForgotPasswordView::class.java)
        )
    }

    override fun beforeEnter(event: BeforeEnterEvent) {
        // Redirect to default view if user is already logged in
        if (event.location.queryParameters.parameters.containsKey("error")) {
            loginForm.isError = true
            Notification.show("Invalid username or password.")
        }
    }
}