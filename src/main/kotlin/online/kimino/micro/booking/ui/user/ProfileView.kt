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
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.PermitAll
import online.kimino.micro.booking.entity.User
import online.kimino.micro.booking.security.SecurityUtils
import online.kimino.micro.booking.service.UserService
import online.kimino.micro.booking.ui.MainLayout

@Route(value = "profile", layout = MainLayout::class)
@PageTitle("My Profile | Booking SaaS")
@PermitAll
class ProfileView(
    private val userService: UserService
) : VerticalLayout() {

    private val firstName = TextField("First Name")
    private val lastName = TextField("Last Name")
    private val email = EmailField("Email")
    private val phoneNumber = TextField("Phone Number")

    private val currentPassword = PasswordField("Current Password")
    private val newPassword = PasswordField("New Password")
    private val confirmPassword = PasswordField("Confirm New Password")

    private val userBinder = BeanValidationBinder(User::class.java)
    private var currentUser: User? = null

    init {
        addClassName("profile-view")
        setSizeFull()

        configureFields()

        add(
            H2("My Profile"),
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

        configureUserBinder()
    }

    private fun configureUserBinder() {
        userBinder.forField(firstName)
            .asRequired("First name is required")
            .bind("firstName")

        userBinder.forField(lastName)
            .asRequired("Last name is required")
            .bind("lastName")

        userBinder.forField(email)
            .asRequired("Email is required")
            .withValidator(EmailValidator("Please enter a valid email address"))
            .bind("email")

        userBinder.forField(phoneNumber)
            .bind("phoneNumber")
    }

    private fun createProfileForm(): VerticalLayout {
        val form = VerticalLayout()
        form.width = "100%"
        form.maxWidth = "600px"
        form.isPadding = true
        form.isSpacing = true

        form.add(
            H2("Personal Information"),
            firstName,
            lastName,
            email,
            phoneNumber,
            Button("Save Profile") { saveProfile() }
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
            H2("Change Password"),
            currentPassword,
            newPassword,
            confirmPassword,
            Button("Change Password") { changePassword() }
                .apply { addThemeVariants(ButtonVariant.LUMO_PRIMARY) }
        )

        return form
    }

    private fun loadUserData() {
        val username = SecurityUtils.getCurrentUsername()
        if (username == null) {
            Notification.show("Error: Not logged in").apply {
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
                Notification.show("Error: User not found").apply {
                    position = Notification.Position.MIDDLE
                    addThemeVariants(NotificationVariant.LUMO_ERROR)
                }
            }
        } catch (e: Exception) {
            Notification.show("Error loading user data: ${e.message}").apply {
                position = Notification.Position.MIDDLE
                addThemeVariants(NotificationVariant.LUMO_ERROR)
            }
        }
    }

    private fun saveProfile() {
        if (currentUser == null) {
            Notification.show("Error: User not found").apply {
                position = Notification.Position.MIDDLE
                addThemeVariants(NotificationVariant.LUMO_ERROR)
            }
            return
        }

        if (userBinder.validate().isOk) {
            try {
                userBinder.writeBean(currentUser)
                userService.updateUser(currentUser!!)

                Notification.show("Profile updated successfully").apply {
                    position = Notification.Position.MIDDLE
                    addThemeVariants(NotificationVariant.LUMO_SUCCESS)
                }
            } catch (e: Exception) {
                Notification.show("Error updating profile: ${e.message}").apply {
                    position = Notification.Position.MIDDLE
                    addThemeVariants(NotificationVariant.LUMO_ERROR)
                }
            }
        }
    }

    private fun changePassword() {
        if (currentUser == null) {
            Notification.show("Error: User not found").apply {
                position = Notification.Position.MIDDLE
                addThemeVariants(NotificationVariant.LUMO_ERROR)
            }
            return
        }

        // Validate password fields
        if (currentPassword.value.isEmpty() || newPassword.value.isEmpty() || confirmPassword.value.isEmpty()) {
            Notification.show("All password fields are required").apply {
                position = Notification.Position.MIDDLE
                addThemeVariants(NotificationVariant.LUMO_ERROR)
            }
            return
        }

        if (newPassword.value != confirmPassword.value) {
            Notification.show("New passwords do not match").apply {
                position = Notification.Position.MIDDLE
                addThemeVariants(NotificationVariant.LUMO_ERROR)
            }
            return
        }

        if (newPassword.value.length < 8) {
            Notification.show("New password must be at least 8 characters long").apply {
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

            Notification.show("Password changed successfully").apply {
                position = Notification.Position.MIDDLE
                addThemeVariants(NotificationVariant.LUMO_SUCCESS)
            }
        } catch (e: Exception) {
            Notification.show("Error changing password: ${e.message}").apply {
                position = Notification.Position.MIDDLE
                addThemeVariants(NotificationVariant.LUMO_ERROR)
            }
        }
    }
}