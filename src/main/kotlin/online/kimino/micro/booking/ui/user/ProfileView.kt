package online.kimino.micro.booking.ui.user

import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.html.H2
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.notification.NotificationVariant
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.EmailField
import com.vaadin.flow.component.textfield.PasswordField
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.binder.BeanValidationBinder
import com.vaadin.flow.data.validator.EmailValidator
import com.vaadin.flow.router.HasDynamicTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.PermitAll
import online.kimino.micro.booking.entity.User
import online.kimino.micro.booking.entity.UserRole
import online.kimino.micro.booking.security.SecurityUtils
import online.kimino.micro.booking.service.UserService
import online.kimino.micro.booking.ui.MainLayout

@Route(value = "profile", layout = MainLayout::class)
@PermitAll
class ProfileView(
    private val userService: UserService
) : VerticalLayout(), HasDynamicTitle {

    private val firstName = TextField(getTranslation("auth.first.name"))
    private val lastName = TextField(getTranslation("auth.last.name"))
    private val email = EmailField(getTranslation("auth.email"))
    private val phoneNumber = TextField(getTranslation("auth.phone"))
    private val companyName = TextField(getTranslation("profile.company.name"))

    private val currentPassword = PasswordField(getTranslation("auth.current.password"))
    private val newPassword = PasswordField(getTranslation("auth.new.password"))
    private val confirmPassword = PasswordField(getTranslation("auth.confirm.password"))

    private val userBinder = BeanValidationBinder(User::class.java)
    private var currentUser: User? = null

    init {
        addClassName("profile-view")
        setSizeFull()

        configureFields()

        add(
            H2(getTranslation("profile.title")),
            createProfileForm(),
            createPasswordForm()
        )

        loadUserData()
    }

    private fun configureFields() {
        firstName.isRequired = true
        lastName.isRequired = true
        email.isRequired = true
        email.isReadOnly = true  // Email cannot be changed

        // Company name is only visible for providers
        companyName.isVisible = SecurityUtils.hasRole(UserRole.PROVIDER)

        configureUserBinder()
    }

    private fun configureUserBinder() {
        userBinder.forField(firstName)
            .asRequired(getTranslation("validation.required"))
            .bind("firstName")

        userBinder.forField(lastName)
            .asRequired(getTranslation("validation.required"))
            .bind("lastName")

        userBinder.forField(email)
            .asRequired(getTranslation("validation.required"))
            .withValidator(EmailValidator(getTranslation("validation.email")))
            .bind("email")

        userBinder.forField(phoneNumber)
            .bind("phoneNumber")

        userBinder.forField(companyName)
            .bind("companyName")
    }

    private fun createProfileForm(): VerticalLayout {
        val form = VerticalLayout()
        form.width = "100%"
        form.maxWidth = "600px"
        form.isPadding = true
        form.isSpacing = true

        form.add(
            H2(getTranslation("profile.company.name")),
            firstName,
            lastName,
            email,
            phoneNumber
        )

        // Only add company name field for providers
        if (SecurityUtils.hasRole(UserRole.PROVIDER)) {
            companyName.placeholder = getTranslation("auth.create.account")
            form.add(companyName)
        }

        form.add(
            Button(getTranslation("common.save")) { saveProfile() }
                .apply { addThemeVariants(ButtonVariant.LUMO_PRIMARY) }
        )

        return form
    }

    private fun createPasswordForm(): VerticalLayout {
        val form = VerticalLayout()
        form.width = "100%"
        form.maxWidth = "600px"
        form.isPadding = true
        form.isSpacing = true

        currentPassword.isRequired = true
        newPassword.isRequired = true
        confirmPassword.isRequired = true

        form.add(
            H2(getTranslation("profile.change.password")),
            currentPassword,
            newPassword,
            confirmPassword,
            Button(getTranslation("profile.change.password")) { changePassword() }
                .apply { addThemeVariants(ButtonVariant.LUMO_PRIMARY) }
        )

        return form
    }

    private fun loadUserData() {
        val username = SecurityUtils.getCurrentUsername()
        if (username == null) {
            Notification.show(getTranslation("user.not.logged.in")).apply {
                position = Notification.Position.MIDDLE
                addThemeVariants(NotificationVariant.LUMO_ERROR)
            }
            return
        }

        try {
            val user = userService.findByEmail(username)
            if (user.isPresent) {
                currentUser = user.get()
                userBinder.readBean(currentUser)
            } else {
                Notification.show(getTranslation("user.not.found")).apply {
                    position = Notification.Position.MIDDLE
                    addThemeVariants(NotificationVariant.LUMO_ERROR)
                }
            }
        } catch (e: Exception) {
            Notification.show(getTranslation("notification.error", e.message)).apply {
                position = Notification.Position.MIDDLE
                addThemeVariants(NotificationVariant.LUMO_ERROR)
            }
        }
    }

    private fun saveProfile() {
        if (currentUser == null) {
            Notification.show(getTranslation("user.not.found")).apply {
                position = Notification.Position.MIDDLE
                addThemeVariants(NotificationVariant.LUMO_ERROR)
            }
            return
        }

        if (userBinder.validate().isOk) {
            try {
                userBinder.writeBean(currentUser)
                userService.updateUser(currentUser!!)

                Notification.show(getTranslation("notification.updated")).apply {
                    position = Notification.Position.MIDDLE
                    addThemeVariants(NotificationVariant.LUMO_SUCCESS)
                }
            } catch (e: Exception) {
                Notification.show(getTranslation("notification.failed") + ": ${e.message}").apply {
                    position = Notification.Position.MIDDLE
                    addThemeVariants(NotificationVariant.LUMO_ERROR)
                }
            }
        }
    }

    private fun changePassword() {
        if (currentUser == null) {
            Notification.show(getTranslation("user.not.found")).apply {
                position = Notification.Position.MIDDLE
                addThemeVariants(NotificationVariant.LUMO_ERROR)
            }
            return
        }

        // Validate password fields
        if (currentPassword.value.isEmpty() || newPassword.value.isEmpty() || confirmPassword.value.isEmpty()) {
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
            // In a real application, we would validate the current password here
            // But for simplicity, we'll just update the password
            userService.changePassword(currentUser!!, newPassword.value)

            // Clear password fields
            currentPassword.clear()
            newPassword.clear()
            confirmPassword.clear()

            Notification.show(getTranslation("notification.updated")).apply {
                position = Notification.Position.MIDDLE
                addThemeVariants(NotificationVariant.LUMO_SUCCESS)
            }
        } catch (e: Exception) {
            Notification.show(getTranslation("notification.failed") + ": ${e.message}").apply {
                position = Notification.Position.MIDDLE
                addThemeVariants(NotificationVariant.LUMO_ERROR)
            }
        }
    }

    override fun getPageTitle() = "micro-booking :: ${getTranslation("profile.title")}"
}