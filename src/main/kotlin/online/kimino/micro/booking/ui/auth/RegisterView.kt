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
import com.vaadin.flow.router.HasDynamicTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.router.RouterLink
import com.vaadin.flow.server.auth.AnonymousAllowed
import online.kimino.micro.booking.entity.User
import online.kimino.micro.booking.service.TranslationProvider
import online.kimino.micro.booking.service.UserService
import org.springframework.beans.factory.annotation.Autowired

@Route("register")
@AnonymousAllowed
class RegisterView(
    @Autowired private val userService: UserService,
    @Autowired private val translationProvider: TranslationProvider
) : VerticalLayout(), HasDynamicTitle {

    private val firstName = TextField(translationProvider.getTranslation("auth.first.name"))
    private val lastName = TextField(translationProvider.getTranslation("auth.last.name"))
    private val email = EmailField(translationProvider.getTranslation("auth.email"))
    private val password = PasswordField(translationProvider.getTranslation("auth.password"))
    private val confirmPassword = PasswordField(translationProvider.getTranslation("auth.confirm.password"))
    private val phoneNumber = TextField(translationProvider.getTranslation("auth.phone.optional"))

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
            H1(translationProvider.getTranslation("app.name")),
            H2(translationProvider.getTranslation("auth.create.account")),
            form,
            RouterLink(
                translationProvider.getTranslation("auth.already.account"),
                LoginView::class.java
            )
        )
    }

    private fun configureFields() {
        firstName.isRequired = true
        lastName.isRequired = true
        email.isRequired = true
        password.isRequired = true
        confirmPassword.isRequired = true

        binder.forField(firstName)
            .asRequired(translationProvider.getTranslation("validation.required"))
            .bind("firstName")

        binder.forField(lastName)
            .asRequired(translationProvider.getTranslation("validation.required"))
            .bind("lastName")

        binder.forField(email)
            .asRequired(translationProvider.getTranslation("validation.required"))
            .withValidator(
                EmailValidator(translationProvider.getTranslation("validation.email"))
            )
            .bind("email")

        binder.forField(password)
            .asRequired(translationProvider.getTranslation("validation.required"))
            .withValidator(
                { it.length >= 8 },
                translationProvider.getTranslation("validation.password.length")
            )
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
            Button(translationProvider.getTranslation("auth.register")) { register() }
        )

        return form
    }

    private fun register() {
        if (password.value != confirmPassword.value) {
            Notification.show(translationProvider.getTranslation("validation.passwords.match"))
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

            Notification.show(
                translationProvider.getTranslation("notification.registration.success")
            )

            // Redirect to login page
            ui.ifPresent { ui -> ui.navigate(LoginView::class.java) }

        } catch (e: Exception) {
            Notification.show(
                "${translationProvider.getTranslation("notification.registration.failed")}: ${e.message}"
            )
        }
    }

    override fun getPageTitle() = "micro-booking :: ${translationProvider.getTranslation("auth.register")}"
}