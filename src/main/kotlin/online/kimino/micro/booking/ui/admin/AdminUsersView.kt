package online.kimino.micro.booking.ui.admin

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.html.H2
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.icon.Icon
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.notification.NotificationVariant
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.router.RouterLink
import jakarta.annotation.security.RolesAllowed
import online.kimino.micro.booking.entity.User
import online.kimino.micro.booking.service.UserService
import online.kimino.micro.booking.ui.MainLayout

@Route(value = "admin/users", layout = MainLayout::class)
@PageTitle("User Management | Booking SaaS")
@RolesAllowed("ADMIN")
class AdminUsersView(
    private val userService: UserService
) : VerticalLayout() {

    private val grid = Grid<User>()

    init {
        addClassName("admin-users-view")
        setSizeFull()

        add(
            createHeaderWithNavigation(),
            createAddUserButton(),
            configureGrid()
        )

        updateUserList()
    }

    private fun createHeaderWithNavigation(): Component {
        val header = VerticalLayout()
        header.setPadding(false)
        header.setSpacing(true)

        header.add(H2("User Management"))

        val navLayout = HorizontalLayout()
        navLayout.setWidthFull()
        navLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.START)
        navLayout.add(
            RouterLink("Dashboard", AdminDashboardView::class.java),
            RouterLink("Services", AdminServicesView::class.java),
            RouterLink("Bookings", AdminBookingsView::class.java)
        )

        header.add(navLayout)
        return header
    }

    private fun configureGrid(): Grid<User> {
        grid.addClassName("users-grid")
        grid.setSizeFull()

        grid.addColumn { user -> user.firstName }
            .setHeader("First Name")
            .setSortable(true)
            .setAutoWidth(true)

        grid.addColumn { user -> user.lastName }
            .setHeader("Last Name")
            .setSortable(true)
            .setAutoWidth(true)

        grid.addColumn { user -> user.email }
            .setHeader("Email")
            .setSortable(true)
            .setAutoWidth(true)

        grid.addColumn { user -> user.phoneNumber ?: "-" }
            .setHeader("Phone")
            .setAutoWidth(true)

        grid.addColumn { user -> user.role.name }
            .setHeader("Role")
            .setSortable(true)
            .setAutoWidth(true)

        grid.addComponentColumn { user ->
            if (user.enabled) {
                Span("Active").apply {
                    element.style.set("color", "var(--lumo-success-color)")
                }
            } else {
                Span("Inactive").apply {
                    element.style.set("color", "var(--lumo-error-color)")
                }
            }
        }
            .setHeader("Status")
            .setAutoWidth(true)

        grid.addComponentColumn { user -> createUserActionButtons(user) }
            .setHeader("Actions")
            .setAutoWidth(true)

        grid.getColumns().forEach { it.setResizable(true) }

        return grid
    }

    private fun createAddUserButton(): Component {
        val buttonLayout = HorizontalLayout()
        buttonLayout.setWidthFull()
        buttonLayout.justifyContentMode = FlexComponent.JustifyContentMode.END

        val addButton = Button("Add User", Icon(VaadinIcon.PLUS)) {
            showUserForm(null)
        }
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY)

        buttonLayout.add(addButton)
        return buttonLayout
    }

    private fun createUserActionButtons(user: User): HorizontalLayout {
        val buttonLayout = HorizontalLayout()
        buttonLayout.isPadding = false
        buttonLayout.isSpacing = true

        // Edit button
        val editButton = Button(Icon(VaadinIcon.EDIT)) {
            showUserForm(user)
        }
        editButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY)
        editButton.element.setAttribute("title", "Edit User")

        // Toggle status button
        val toggleButton = if (user.enabled) {
            Button(Icon(VaadinIcon.BAN)) {
                toggleUserStatus(user)
            }.apply {
                element.setAttribute("title", "Disable User")
                addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR)
            }
        } else {
            Button(Icon(VaadinIcon.CHECK)) {
                toggleUserStatus(user)
            }.apply {
                element.setAttribute("title", "Enable User")
                addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SUCCESS)
            }
        }

        buttonLayout.add(editButton, toggleButton)
        return buttonLayout
    }

    private fun showUserForm(user: User?) {
        val dialog = Dialog()
        dialog.headerTitle = if (user == null) "Add New User" else "Edit User"

        val userForm = UserForm(user)

        userForm.addSaveListener { event ->
            saveUser(event.user)
            dialog.close()
        }

        userForm.addCancelListener {
            dialog.close()
        }

        dialog.add(userForm)
        dialog.open()
    }

    private fun saveUser(user: User) {
        try {
            if (user.id == 0L) {
                // New user
                userService.createUser(user, user.role)
            } else {
                // Existing user
                userService.updateUser(user)
            }

            Notification.show("User saved successfully").apply {
                position = Notification.Position.MIDDLE
                addThemeVariants(NotificationVariant.LUMO_SUCCESS)
            }

            updateUserList()
        } catch (e: Exception) {
            Notification.show("Failed to save user: ${e.message}").apply {
                position = Notification.Position.MIDDLE
                addThemeVariants(NotificationVariant.LUMO_ERROR)
            }
        }
    }

    private fun toggleUserStatus(user: User) {
        try {
            user.enabled = !user.enabled
            userService.updateUser(user)

            val statusText = if (user.enabled) "enabled" else "disabled"
            Notification.show("User $statusText successfully").apply {
                position = Notification.Position.MIDDLE
                addThemeVariants(NotificationVariant.LUMO_SUCCESS)
            }

            updateUserList()
        } catch (e: Exception) {
            Notification.show("Failed to update user status: ${e.message}").apply {
                position = Notification.Position.MIDDLE
                addThemeVariants(NotificationVariant.LUMO_ERROR)
            }
        }
    }

    private fun updateUserList() {
        grid.setItems(userService.getAllUsers())
    }
}