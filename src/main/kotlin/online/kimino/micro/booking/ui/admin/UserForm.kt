package online.kimino.micro.booking.ui.admin

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.ComponentEvent
import com.vaadin.flow.component.ComponentEventListener
import com.vaadin.flow.component.Key
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.combobox.ComboBox
import com.vaadin.flow.component.formlayout.FormLayout
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.textfield.EmailField
import com.vaadin.flow.component.textfield.PasswordField
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.binder.BeanValidationBinder
import com.vaadin.flow.data.validator.EmailValidator
import com.vaadin.flow.shared.Registration
import online.kimino.micro.booking.entity.User
import online.kimino.micro.booking.entity.UserRole

class UserForm(user: User?) : FormLayout() {

    private val firstName = TextField("First Name")
    private val lastName = TextField("Last Name")
    private val email = EmailField("Email")
    private val phoneNumber = TextField("Phone Number")
    private val role = ComboBox<UserRole>("Role")
    private val enabled = Checkbox("Enabled")
    private val password = PasswordField("Password")

    private val save = Button("Save")
    private val cancel = Button("Cancel")

    private val binder = BeanValidationBinder(User::class.java)
    private var currentUser = user ?: User(
        email = "",
        password = "",
        firstName = "",
        lastName = ""
    )

    init {
        addClassName("user-form")

        configureFields()

        binder.bindInstanceFields(this)
        binder.readBean(currentUser)

        // Set password field visibility based on whether this is a new user
        password.isVisible = currentUser.id == 0L

        add(
            firstName,
            lastName,
            email,
            phoneNumber,
            role,
            enabled,
            password,
            createButtonsLayout()
        )
    }

    private fun configureFields() {
        firstName.isRequired = true
        lastName.isRequired = true
        email.isRequired = true

        role.setItems(UserRole.entries.toList())
        role.isRequired = true

        // Make password required for new users
        if (currentUser.id == 0L) {
            password.isRequired = true
        }

        configureBinder()
    }

    private fun configureBinder() {
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

        binder.forField(phoneNumber)
            .bind("phoneNumber")

        binder.forField(role)
            .asRequired("Role is required")
            .bind("role")

        binder.forField(enabled)
            .bind("enabled")

        // Only bind password for new users
        if (currentUser.id == 0L) {
            binder.forField(password)
                .asRequired("Password is required")
                .withValidator({ it.length >= 8 }, "Password must be at least 8 characters long")
                .bind("password")
        }
    }

    private fun createButtonsLayout(): Component {
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY)
        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY)

        save.addClickShortcut(Key.ENTER)
        cancel.addClickShortcut(Key.ESCAPE)

        save.addClickListener { validateAndSave() }
        cancel.addClickListener { fireEvent(CancelEvent(this)) }

        return HorizontalLayout(save, cancel)
    }

    private fun validateAndSave() {
        if (binder.validate().isOk) {
            binder.writeBean(currentUser)
            fireEvent(SaveEvent(this, currentUser))
        }
    }

    // Events
    abstract class UserFormEvent(source: UserForm, val user: User) : ComponentEvent<UserForm>(source, false)

    class SaveEvent(source: UserForm, user: User) : UserFormEvent(source, user)

    class CancelEvent(source: UserForm) : ComponentEvent<UserForm>(source, false)

    // Event registration
    fun addSaveListener(listener: ComponentEventListener<SaveEvent>): Registration {
        return addListener(SaveEvent::class.java, listener)
    }

    fun addCancelListener(listener: ComponentEventListener<CancelEvent>): Registration {
        return addListener(CancelEvent::class.java, listener)
    }
}