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

    private val firstName = TextField(getTranslation("auth.first.name"))
    private val lastName = TextField(getTranslation("auth.last.name"))
    private val email = EmailField(getTranslation("auth.email"))
    private val phoneNumber = TextField(getTranslation("auth.phone"))
    private val companyName = TextField(getTranslation("profile.personal.info"))
    private val role = ComboBox<UserRole>(getTranslation("admin.column.role"))
    private val enabled = Checkbox(getTranslation("service.active"))
    private val password = PasswordField(getTranslation("auth.password"))

    private val save = Button(getTranslation("common.save"))
    private val cancel = Button(getTranslation("common.cancel"))

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

        // Set company name field visibility based on role
        companyName.isVisible = currentUser.role == UserRole.PROVIDER

        // Add listener to show/hide company name field when role changes
        role.addValueChangeListener { event ->
            companyName.isVisible = event.value == UserRole.PROVIDER
        }

        add(
            firstName,
            lastName,
            email,
            phoneNumber,
            role,
            companyName,
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

        companyName.placeholder = getTranslation("auth.create.account")

        // Make password required for new users
        if (currentUser.id == 0L) {
            password.isRequired = true
        }

        configureBinder()
    }

    private fun configureBinder() {
        binder.forField(firstName)
            .asRequired(getTranslation("validation.required"))
            .bind("firstName")

        binder.forField(lastName)
            .asRequired(getTranslation("validation.required"))
            .bind("lastName")

        binder.forField(email)
            .asRequired(getTranslation("validation.required"))
            .withValidator(EmailValidator(getTranslation("validation.email")))
            .bind("email")

        binder.forField(phoneNumber)
            .bind("phoneNumber")

        binder.forField(companyName)
            .bind("companyName")

        binder.forField(role)
            .asRequired(getTranslation("validation.required"))
            .bind("role")

        binder.forField(enabled)
            .bind("enabled")

        // Only bind password for new users
        if (currentUser.id == 0L) {
            binder.forField(password)
                .asRequired(getTranslation("validation.required"))
                .withValidator({ it.length >= 8 }, getTranslation("validation.password.length"))
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