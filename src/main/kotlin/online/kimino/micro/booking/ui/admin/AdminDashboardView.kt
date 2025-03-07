package online.kimino.micro.booking.ui.admin

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.html.H2
import com.vaadin.flow.component.html.H3
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.progressbar.ProgressBar
import com.vaadin.flow.router.HasDynamicTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.router.RouterLink
import jakarta.annotation.security.RolesAllowed
import online.kimino.micro.booking.entity.BookingStatus
import online.kimino.micro.booking.entity.UserRole
import online.kimino.micro.booking.service.BookingService
import online.kimino.micro.booking.service.ServiceService
import online.kimino.micro.booking.service.UserService
import online.kimino.micro.booking.ui.MainLayout

@Route(value = "admin", layout = MainLayout::class)
@RolesAllowed("ADMIN")
class AdminDashboardView(
    private val userService: UserService,
    private val serviceService: ServiceService,
    private val bookingService: BookingService
) : VerticalLayout(), HasDynamicTitle {

    init {
        addClassName("admin-dashboard-view")
        setSizeFull()

        add(
            createHeaderWithNavigation(),
            createStatsLayout(),
            createBookingStatusVisual(),
            createUserRoleDistribution()
        )
    }

    private fun createHeaderWithNavigation(): Component {
        val header = VerticalLayout()
        header.setPadding(false)
        header.setSpacing(true)

        header.add(H2(getTranslation("admin.dashboard")))

        val navLayout = HorizontalLayout()
        navLayout.setWidthFull()
        navLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.START)
        navLayout.add(
            RouterLink(getTranslation("admin.users"), AdminUsersView::class.java),
            RouterLink(getTranslation("admin.services"), AdminServicesView::class.java),
            RouterLink(getTranslation("admin.bookings"), AdminBookingsView::class.java)
        )

        header.add(navLayout)
        return header
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
            createStat(getTranslation("admin.stats.total.users"), totalUsers.toString()),
            createStat(getTranslation("admin.stats.service.providers"), totalProviders.toString()),
            createStat(getTranslation("admin.stats.total.services"), totalServices.toString()),
            createStat(getTranslation("admin.stats.total.bookings"), totalBookings.toString())
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

        layout.add(H3(getTranslation("admin.bookings.by.status")))

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
            layout.add(createStatusBar(getTranslation("booking.status.pending"), pendingCount, totalBookings, "var(--lumo-primary-color)"))

            // Confirmed bookings
            layout.add(createStatusBar(getTranslation("booking.status.confirmed"), confirmedCount, totalBookings, "var(--lumo-success-color)"))

            // Cancelled bookings
            layout.add(createStatusBar(getTranslation("booking.status.cancelled"), cancelledCount, totalBookings, "var(--lumo-error-color)"))

            // Completed bookings
            layout.add(createStatusBar(getTranslation("booking.status.completed"), completedCount, totalBookings, "var(--lumo-success-text-color)"))
        } else {
            layout.add(Span(getTranslation("admin.no.bookings")))
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

        layout.add(H3(getTranslation("admin.user.roles.distribution")))

        val allUsers = userService.getAllUsers()
        val adminCount = allUsers.count { it.role == UserRole.ADMIN }
        val providerCount = allUsers.count { it.role == UserRole.PROVIDER }
        val regularUserCount = allUsers.count { it.role == UserRole.USER }

        val totalUsers = allUsers.size

        if (totalUsers > 0) {
            // Admin users
            layout.add(createStatusBar(getTranslation("admin.role.admins"), adminCount, totalUsers, "#7986CB")) // Indigo color

            // Provider users
            layout.add(createStatusBar(getTranslation("admin.role.providers"), providerCount, totalUsers, "#4CAF50")) // Green color

            // Regular users
            layout.add(createStatusBar(getTranslation("admin.role.users"), regularUserCount, totalUsers, "#2196F3")) // Blue color

            // Add a table with detailed numbers
            val statsLayout = createRoleStatsTable(adminCount, providerCount, regularUserCount, totalUsers)
            layout.add(statsLayout)
        } else {
            layout.add(Span(getTranslation("admin.no.users")))
        }

        return layout
    }

    private fun createRoleStatsTable(adminCount: Int, providerCount: Int, userCount: Int, total: Int): Component {
        val grid = com.vaadin.flow.component.grid.Grid<RoleStatRow>()
        grid.setItems(
            RoleStatRow(getTranslation("admin.role.admins"), adminCount, calculatePercentage(adminCount, total)),
            RoleStatRow(getTranslation("admin.role.providers"), providerCount, calculatePercentage(providerCount, total)),
            RoleStatRow(getTranslation("admin.role.users"), userCount, calculatePercentage(userCount, total)),
            RoleStatRow(getTranslation("admin.total"), total, 100.0)
        )

        grid.addColumn { it.role }
            .setHeader(getTranslation("admin.column.role"))
            .setAutoWidth(true)

        grid.addColumn { it.count }
            .setHeader(getTranslation("admin.column.count"))
            .setAutoWidth(true)

        grid.addColumn { "%.1f%%".format(it.percentage) }
            .setHeader(getTranslation("admin.column.percentage"))
            .setAutoWidth(true)

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

    // Data class for role statistics
    data class RoleStatRow(val role: String, val count: Int, val percentage: Double)

    override fun getPageTitle() = "micro-booking :: ${getTranslation("admin.dashboard")}"
}