package online.kimino.micro.booking.ui.admin

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.html.H2
import com.vaadin.flow.component.html.H3
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.icon.Icon
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.notification.NotificationVariant
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.progressbar.ProgressBar
import com.vaadin.flow.component.tabs.Tab
import com.vaadin.flow.component.tabs.Tabs
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed
import online.kimino.micro.booking.entity.Booking
import online.kimino.micro.booking.entity.BookingStatus
import online.kimino.micro.booking.entity.User
import online.kimino.micro.booking.entity.UserRole
import online.kimino.micro.booking.service.BookingService
import online.kimino.micro.booking.service.ServiceService
import online.kimino.micro.booking.service.UserService
import online.kimino.micro.booking.ui.MainLayout
import java.time.format.DateTimeFormatter

@Route(value = "admin", layout = MainLayout::class)
@PageTitle("Admin Dashboard | Booking SaaS")
@RolesAllowed("ADMIN")
class AdminDashboardView(
    private val userService: UserService,
    private val serviceService: ServiceService,
    private val bookingService: BookingService
) : VerticalLayout() {

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    private val tabs = Tabs()
    private val dashboardTab = Tab("Dashboard")
    private val usersTab = Tab("Users")
    private val servicesTab = Tab("Services")
    private val bookingsTab = Tab("Bookings")

    private val contentContainer = VerticalLayout()

    init {
        addClassName("admin-dashboard-view")
        setSizeFull()

        configureTabs()

        add(
            H2("Admin Dashboard"),
            tabs,
            contentContainer
        )

        showDashboard()
    }

    private fun configureTabs() {
        tabs.add(dashboardTab, usersTab, servicesTab, bookingsTab)
        tabs.addSelectedChangeListener { event ->
            when (event.selectedTab) {
                dashboardTab -> showDashboard()
                usersTab -> showUsers()
                servicesTab -> showServices()
                bookingsTab -> showBookings()
            }
        }
    }

    private fun showDashboard() {
        contentContainer.removeAll()

        contentContainer.add(
            createStatsLayout(),
            createBookingStatusVisual(),
            createUserRoleDistribution()
        )
    }

    private fun showUsers() {
        contentContainer.removeAll()

        val usersGrid = Grid<User>()
        usersGrid.addClassName("users-grid")
        usersGrid.setSizeFull()

        usersGrid.addColumn { user -> user.firstName }
            .setHeader("First Name")
            .setSortable(true)
            .setAutoWidth(true)

        usersGrid.addColumn { user -> user.lastName }
            .setHeader("Last Name")
            .setSortable(true)
            .setAutoWidth(true)

        usersGrid.addColumn { user -> user.email }
            .setHeader("Email")
            .setSortable(true)
            .setAutoWidth(true)

        usersGrid.addColumn { user -> user.phoneNumber ?: "-" }
            .setHeader("Phone")
            .setAutoWidth(true)

        usersGrid.addColumn { user -> user.role.name }
            .setHeader("Role")
            .setSortable(true)
            .setAutoWidth(true)

        usersGrid.addColumn { user ->
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

        usersGrid.addComponentColumn { user -> createUserActionButtons(user) }
            .setHeader("Actions")
            .setAutoWidth(true)

        usersGrid.getColumns().forEach { it.setResizable(true) }

        usersGrid.setItems(userService.getAllUsers())

        contentContainer.add(
            createAddUserButton(),
            usersGrid
        )
    }

    private fun showServices() {
        contentContainer.removeAll()

        val servicesGrid = Grid<online.kimino.micro.booking.entity.Service>()
        servicesGrid.addClassName("services-grid")
        servicesGrid.setSizeFull()

        servicesGrid.addColumn { service -> service.name }
            .setHeader("Service Name")
            .setSortable(true)
            .setAutoWidth(true)

        servicesGrid.addColumn { service -> service.provider!!.fullName() }
            .setHeader("Provider")
            .setSortable(true)
            .setAutoWidth(true)

        servicesGrid.addColumn { service -> service.duration }
            .setHeader("Duration (min)")
            .setSortable(true)
            .setAutoWidth(true)

        servicesGrid.addColumn { service -> "$${service.price}" }
            .setHeader("Price")
            .setSortable(true)
            .setAutoWidth(true)

        servicesGrid.addColumn { service ->
            if (service.active) {
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

        servicesGrid.addComponentColumn { service -> createServiceActionButtons(service) }
            .setHeader("Actions")
            .setAutoWidth(true)

        servicesGrid.getColumns().forEach { it.setResizable(true) }

        servicesGrid.setItems(serviceService.findAll())

        contentContainer.add(servicesGrid)
    }

    private fun showBookings() {
        contentContainer.removeAll()

        val bookingsGrid = Grid<Booking>()
        bookingsGrid.addClassName("bookings-grid")
        bookingsGrid.setSizeFull()

        bookingsGrid.addColumn { booking -> booking.service.name }
            .setHeader("Service")
            .setSortable(true)
            .setAutoWidth(true)

        bookingsGrid.addColumn { booking -> booking.service.provider!!.fullName() }
            .setHeader("Provider")
            .setSortable(true)
            .setAutoWidth(true)

        bookingsGrid.addColumn { booking -> booking.user.fullName() }
            .setHeader("Customer")
            .setSortable(true)
            .setAutoWidth(true)

        bookingsGrid.addColumn { booking -> booking.startTime.format(formatter) }
            .setHeader("Start Time")
            .setSortable(true)
            .setAutoWidth(true)

        bookingsGrid.addColumn { booking -> booking.endTime.format(formatter) }
            .setHeader("End Time")
            .setAutoWidth(true)

        bookingsGrid.addColumn { booking ->
            val statusSpan = Span(booking.status.name)

            // Style based on status
            when (booking.status) {
                BookingStatus.PENDING -> statusSpan.element.style.set("color", "var(--lumo-primary-color)")
                BookingStatus.CONFIRMED -> statusSpan.element.style.set("color", "var(--lumo-success-color)")
                BookingStatus.CANCELLED -> statusSpan.element.style.set("color", "var(--lumo-error-color)")
                BookingStatus.COMPLETED -> statusSpan.element.style.set("color", "var(--lumo-success-text-color)")
            }

            statusSpan
        }
            .setHeader("Status")
            .setAutoWidth(true)

        bookingsGrid.getColumns().forEach { it.setResizable(true) }

        // Get all bookings
        val allBookings = mutableListOf<Booking>()
        userService.getAllUsers().forEach { user ->
            if (user.role == UserRole.PROVIDER) {
                allBookings.addAll(bookingService.findAllByProviderId(user.id))
            }
        }

        bookingsGrid.setItems(allBookings)

        contentContainer.add(bookingsGrid)
    }

    private fun createStatsLayout(): Component {
        val stats = HorizontalLayout()
        stats.setWidthFull()
        stats.isPadding = true
        stats.isSpacing = true

        val totalUsers = userService.getAllUsers().size
        val totalProviders = userService.getAllProviders().size
        val totalServices = serviceService.findAll().size

        // Count total bookings
        val totalBookings = userService.getAllUsers()
            .filter { it.role == UserRole.PROVIDER }
            .flatMap { bookingService.findAllByProviderId(it.id) }
            .size

        stats.add(
            createStat("Total Users", totalUsers.toString()),
            createStat("Service Providers", totalProviders.toString()),
            createStat("Total Services", totalServices.toString()),
            createStat("Total Bookings", totalBookings.toString())
        )

        return stats
    }

    private fun createStat(title: String, value: String): Component {
        val stat = VerticalLayout()
        stat.addClassName("stat-item")
        stat.width = "25%"
        stat.isPadding = true
        stat.isSpacing = true
        stat.style.set("background-color", "var(--lumo-base-color)")
        stat.style.set("border-radius", "var(--lumo-border-radius-m)")
        stat.style.set("box-shadow", "var(--lumo-box-shadow-xs)")

        val titleSpan = H3(title)
        titleSpan.style.set("margin", "0")

        val valueSpan = Span(value)
        valueSpan.style.set("font-size", "var(--lumo-font-size-xxl)")
        valueSpan.style.set("font-weight", "bold")

        stat.add(titleSpan, valueSpan)
        return stat
    }

    private fun createBookingStatusVisual(): Component {
        val layout = VerticalLayout()
        layout.setPadding(true)
        layout.setSpacing(true)
        layout.style.set("background-color", "var(--lumo-base-color)")
        layout.style.set("border-radius", "var(--lumo-border-radius-m)")
        layout.style.set("box-shadow", "var(--lumo-box-shadow-xs)")
        layout.width = "100%"
        layout.height = "auto"
        layout.isMargin = true

        layout.add(H3("Bookings by Status"))

        // Count bookings by status
        val allBookings = userService.getAllUsers()
            .filter { it.role == UserRole.PROVIDER }
            .flatMap { bookingService.findAllByProviderId(it.id) }

        val pendingCount = allBookings.count { it.status == BookingStatus.PENDING }
        val confirmedCount = allBookings.count { it.status == BookingStatus.CONFIRMED }
        val cancelledCount = allBookings.count { it.status == BookingStatus.CANCELLED }
        val completedCount = allBookings.count { it.status == BookingStatus.COMPLETED }

        val totalBookings = allBookings.size

        if (totalBookings > 0) {
            // Pending bookings
            layout.add(createStatusBar("Pending", pendingCount, totalBookings, "var(--lumo-primary-color)"))

            // Confirmed bookings
            layout.add(createStatusBar("Confirmed", confirmedCount, totalBookings, "var(--lumo-success-color)"))

            // Cancelled bookings
            layout.add(createStatusBar("Cancelled", cancelledCount, totalBookings, "var(--lumo-error-color)"))

            // Completed bookings
            layout.add(createStatusBar("Completed", completedCount, totalBookings, "var(--lumo-success-text-color)"))
        } else {
            layout.add(Span("No bookings yet."))
        }

        return layout
    }

    private fun createUserRoleDistribution(): Component {
        val layout = VerticalLayout()
        layout.setPadding(true)
        layout.setSpacing(true)
        layout.style.set("background-color", "var(--lumo-base-color)")
        layout.style.set("border-radius", "var(--lumo-border-radius-m)")
        layout.style.set("box-shadow", "var(--lumo-box-shadow-xs)")
        layout.width = "100%"
        layout.height = "auto"
        layout.isMargin = true

        layout.add(H3("User Roles Distribution"))

        val allUsers = userService.getAllUsers()
        val adminCount = allUsers.count { it.role == UserRole.ADMIN }
        val providerCount = allUsers.count { it.role == UserRole.PROVIDER }
        val regularUserCount = allUsers.count { it.role == UserRole.USER }

        val totalUsers = allUsers.size

        if (totalUsers > 0) {
            // Admin users
            layout.add(createStatusBar("Admins", adminCount, totalUsers, "#7986CB")) // Indigo color

            // Provider users
            layout.add(createStatusBar("Providers", providerCount, totalUsers, "#4CAF50")) // Green color

            // Regular users
            layout.add(createStatusBar("Regular Users", regularUserCount, totalUsers, "#2196F3")) // Blue color

            // Add a table with detailed numbers
            val statsLayout = createRoleStatsTable(adminCount, providerCount, regularUserCount, totalUsers)
            layout.add(statsLayout)
        } else {
            layout.add(Span("No users yet."))
        }

        return layout
    }

    private fun createRoleStatsTable(adminCount: Int, providerCount: Int, userCount: Int, total: Int): Component {
        val grid = Grid<RoleStatRow>()
        grid.setItems(
            RoleStatRow("Admins", adminCount, calculatePercentage(adminCount, total)),
            RoleStatRow("Providers", providerCount, calculatePercentage(providerCount, total)),
            RoleStatRow("Regular Users", userCount, calculatePercentage(userCount, total)),
            RoleStatRow("Total", total, 100.0)
        )

        grid.addColumn { it.role }
            .setHeader("Role")
            .setAutoWidth(true)

        grid.addColumn { it.count }
            .setHeader("Count")
            .setAutoWidth(true)

        grid.addColumn { "%.1f%%".format(it.percentage) }
            .setHeader("Percentage")
            .setAutoWidth(true)

//        grid.setHeightByRows(true)
        return grid
    }

    private fun calculatePercentage(count: Int, total: Int): Double {
        return if (total > 0) (count.toDouble() / total * 100) else 0.0
    }

    private fun createStatusBar(label: String, count: Int, total: Int, color: String): Component {
        val layout = VerticalLayout()
        layout.isSpacing = false
        layout.isPadding = false
        layout.width = "100%"

        val percentage = if (total > 0) (count.toDouble() / total) else 0.0

        val headerLayout = HorizontalLayout()
        headerLayout.setWidthFull()
        headerLayout.justifyContentMode = FlexComponent.JustifyContentMode.BETWEEN

        val labelSpan = Span(label)
        labelSpan.style.set("color", color)
        labelSpan.style.set("font-weight", "bold")

        val countSpan = Span("$count (${"%.1f".format(percentage * 100)}%)")

        headerLayout.add(labelSpan, countSpan)

        // Progress bar showing the percentage
        val progressBar = ProgressBar(0.0, 1.0, percentage)
        progressBar.width = "100%"
        progressBar.height = "10px"
        progressBar.style.set("--lumo-primary-color", color)

        layout.add(headerLayout, progressBar)
        return layout
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

    private fun createServiceActionButtons(service: online.kimino.micro.booking.entity.Service): HorizontalLayout {
        val buttonLayout = HorizontalLayout()
        buttonLayout.isPadding = false
        buttonLayout.isSpacing = true

        // Toggle status button
        val toggleButton = if (service.active) {
            Button(Icon(VaadinIcon.BAN)) {
                toggleServiceStatus(service)
            }.apply {
                element.setAttribute("title", "Deactivate Service")
                addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR)
            }
        } else {
            Button(Icon(VaadinIcon.CHECK)) {
                toggleServiceStatus(service)
            }.apply {
                element.setAttribute("title", "Activate Service")
                addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SUCCESS)
            }
        }

        buttonLayout.add(toggleButton)
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

            // Refresh users tab
            if (tabs.selectedTab == usersTab) {
                showUsers()
            }
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

            // Refresh users tab
            if (tabs.selectedTab == usersTab) {
                showUsers()
            }
        } catch (e: Exception) {
            Notification.show("Failed to update user status: ${e.message}").apply {
                position = Notification.Position.MIDDLE
                addThemeVariants(NotificationVariant.LUMO_ERROR)
            }
        }
    }

    private fun toggleServiceStatus(service: online.kimino.micro.booking.entity.Service) {
        try {
            serviceService.toggleServiceStatus(service.id)

            val statusText = if (service.active) "deactivated" else "activated"
            Notification.show("Service $statusText successfully").apply {
                position = Notification.Position.MIDDLE
                addThemeVariants(NotificationVariant.LUMO_SUCCESS)
            }

            // Refresh services tab
            if (tabs.selectedTab == servicesTab) {
                showServices()
            }
        } catch (e: Exception) {
            Notification.show("Failed to update service status: ${e.message}").apply {
                position = Notification.Position.MIDDLE
                addThemeVariants(NotificationVariant.LUMO_ERROR)
            }
        }
    }

    // Data class for role statistics
    data class RoleStatRow(val role: String, val count: Int, val percentage: Double)
}