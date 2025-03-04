package online.kimino.micro.booking.ui.auth

import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.H1
import com.vaadin.flow.component.html.H2
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.EmailField
import com.vaadin.flow.component.textfield.PasswordField
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.binder.BeanValidationBinder
import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.data.validator.EmailValidator
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.router.RouterLink
import com.vaadin.flow.server.auth.AnonymousAllowed
import online.kimino.micro.booking.entity.User
import online.kimino.micro.booking.service.UserService
import org.springframework.beans.factory.annotation.Autowired

@Route("register")
@PageTitle("Register | Booking SaaS")
@AnonymousAllowed
class RegisterView(@Autowired private val userService: UserService) : VerticalLayout() {

    private val firstName = TextField("First Name")
    private val lastName = TextField("Last Name")
    private val email = EmailField("Email")
    private val password = PasswordField("Password")
    private val confirmPassword = PasswordField("Confirm Password")
    private val phoneNumber = TextField("Phone Number (Optional)")

    private val binder: Binder<User> = BeanValidationBinder(User::class.java)

    init {
        addClassName("register-view")
        setSizeFull()

        // Center the content
        justifyContentMode = FlexComponent.JustifyContentMode.CENTER
        alignItems = FlexComponent.Alignment.CENTER

        configureFields()

        val form = createForm()

        add(
            H1("Booking SaaS"),
            H2("Create an Account"),
            form,
            RouterLink("Already have an account? Login", LoginView::class.java)
        )
    }

    private fun configureFields() {
        firstName.isRequired = true
        lastName.isRequired = true
        email.isRequired = true
        password.isRequired = true
        confirmPassword.isRequired = true

        binder.forField(firstName)
            .asRequired("First name is required")
            .bind("firstName")

        binder.forField(lastName)
            .asRequired("Last name is required")
            .bind("lastName")

        binder.forField(email)
            .asRequired("Email is required")
            .withValidator(EmailValidator("Please enter a valid email address"))
            .bind("email")

        binder.forField(password)
            .asRequired("Password is required")
            .withValidator({ it.length >= 8 }, "Password must be at least 8 characters long")
            .bind("password")

        binder.forField(phoneNumber)
            .bind("phoneNumber")
    }

    private fun createForm(): VerticalLayout {
        val form = VerticalLayout()
        form.width = "100%"
        form.maxWidth = "500px"
        form.isPadding = true
        form.isSpacing = true

        form.add(
            firstName,
            lastName,
            email,
            password,
            confirmPassword,
            phoneNumber,
            Button("Register") { register() }
        )

        return form
    }

    private fun register() {
        if (password.value != confirmPassword.value) {
            Notification.show("Passwords do not match")
            return
        }

        try {
            val user = User(
                email = email.value,
                password = password.value,
                firstName = firstName.value,
                lastName = lastName.value,
                phoneNumber = phoneNumber.value.takeIf { it.isNotBlank() }
            )

            userService.createUser(user)

            Notification.show("Registration successful! Please check your email to verify your account.")

            // Redirect to login page
            ui.ifPresent { ui -> ui.navigate(LoginView::class.java) }

        } catch (e: Exception) {
            Notification.show("Registration failed: ${e.message}")
        }
    }
}