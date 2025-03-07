package online.kimino.micro.booking.ui

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.html.H2
import com.vaadin.flow.component.html.H3
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.progressbar.ProgressBar
import com.vaadin.flow.router.*
import jakarta.annotation.security.PermitAll
import online.kimino.micro.booking.entity.Booking
import online.kimino.micro.booking.entity.BookingStatus
import online.kimino.micro.booking.entity.UserRole
import online.kimino.micro.booking.security.SecurityUtils
import online.kimino.micro.booking.service.BookingService
import online.kimino.micro.booking.service.UserService
import java.time.format.DateTimeFormatter

@Route(value = "", layout = MainLayout::class)
@RouteAlias(value = "dashboard", layout = MainLayout::class)
@PermitAll
class DashboardView(
    private val bookingService: BookingService,
    private val userService: UserService
) : VerticalLayout(), BeforeEnterObserver, HasDynamicTitle {

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    init {
        addClassName("dashboard-view")
        setSizeFull()

        add(
            H2(getTranslation("dashboard.title")),
            createStatsLayout(),
            createBookingStatusVisualization(),
            createUpcomingBookingsGrid()
        )
    }

    private fun createStatsLayout(): Component {
        val stats = HorizontalLayout()
        stats.setWidthFull()
        stats.isPadding = true
        stats.isSpacing = true

        val currentUser = SecurityUtils.getCurrentUsername()?.let { userService.findByEmail(it).orElse(null) }

        // Different stats based on user role
        when (SecurityUtils.getCurrentUserRole()) {
            UserRole.ADMIN -> {
                stats.add(
                    createStat(getTranslation("admin.stats.total.users"), userService.getAllUsers().size.toString()),
                    createStat(
                        getTranslation("admin.stats.service.providers"),
                        userService.getAllProviders().size.toString()
                    ),
                    createStat(
                        getTranslation("admin.stats.total.bookings"),
                        bookingService.findAllByUserId(currentUser?.id ?: 0).size.toString()
                    )
                )
            }

            UserRole.PROVIDER -> {
                val providerId = currentUser?.id ?: 0
                stats.add(
                    createStat(
                        getTranslation("admin.stats.total.bookings"),
                        bookingService.findAllByProviderId(providerId).size.toString()
                    ),
                    createStat(
                        getTranslation("booking.status.pending"),
                        bookingService.findAllByProviderIdAndStatus(providerId, BookingStatus.PENDING).size.toString()
                    ),
                    createStat(
                        getTranslation("booking.status.completed"),
                        bookingService.findAllByProviderIdAndStatus(providerId, BookingStatus.COMPLETED).size.toString()
                    )
                )
            }

            else -> { // Regular user
                val userId = currentUser?.id ?: 0
                stats.add(
                    createStat(
                        getTranslation("admin.stats.total.bookings"),
                        bookingService.findAllByUserId(userId).size.toString()
                    ),
                    createStat(
                        getTranslation("booking.status.pending"),
                        bookingService.findAllByUserIdAndStatus(userId, BookingStatus.PENDING).size.toString()
                    ),
                    createStat(
                        getTranslation("booking.status.confirmed"),
                        bookingService.findAllByUserIdAndStatus(userId, BookingStatus.CONFIRMED).size.toString()
                    )
                )
            }
        }

        return stats
    }

    private fun createStat(title: String, value: String): Component {
        val stat = VerticalLayout()
        stat.addClassName("stat-item")
        stat.width = "33%"
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

    private fun createBookingStatusVisualization(): Component {
        val layout = VerticalLayout()
        layout.setPadding(true)
        layout.setSpacing(true)
        layout.style.set("background-color", "var(--lumo-base-color)")
        layout.style.set("border-radius", "var(--lumo-border-radius-m)")
        layout.style.set("box-shadow", "var(--lumo-box-shadow-xs)")
        layout.width = "100%"

        layout.add(H3(getTranslation("admin.bookings.by.status")))

        // Count bookings by status
        val userId = SecurityUtils.getCurrentUsername()?.let {
            userService.findByEmail(it).map { it.id }.orElse(0)
        } ?: 0

        val pendingCount = bookingService.findAllByUserIdAndStatus(userId, BookingStatus.PENDING).size
        val confirmedCount = bookingService.findAllByUserIdAndStatus(userId, BookingStatus.CONFIRMED).size
        val cancelledCount = bookingService.findAllByUserIdAndStatus(userId, BookingStatus.CANCELLED).size
        val completedCount = bookingService.findAllByUserIdAndStatus(userId, BookingStatus.COMPLETED).size

        val totalBookings = pendingCount + confirmedCount + cancelledCount + completedCount

        if (totalBookings > 0) {
            // Pending bookings
            layout.add(
                createStatusBar(
                    getTranslation("booking.status.pending"),
                    pendingCount,
                    totalBookings,
                    "var(--lumo-primary-color)"
                )
            )

            // Confirmed bookings
            layout.add(
                createStatusBar(
                    getTranslation("booking.status.confirmed"),
                    confirmedCount,
                    totalBookings,
                    "var(--lumo-success-color)"
                )
            )

            // Cancelled bookings
            layout.add(
                createStatusBar(
                    getTranslation("booking.status.cancelled"),
                    cancelledCount,
                    totalBookings,
                    "var(--lumo-error-color)"
                )
            )

            // Completed bookings
            layout.add(
                createStatusBar(
                    getTranslation("booking.status.completed"),
                    completedCount,
                    totalBookings,
                    "var(--lumo-success-text-color)"
                )
            )
        } else {
            layout.add(Span(getTranslation("admin.no.bookings")))
        }

        return layout
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

    private fun createUpcomingBookingsGrid(): Component {
        val layout = VerticalLayout()
        layout.setPadding(false)
        layout.setSpacing(true)

        layout.add(H3(getTranslation("dashboard.upcoming.bookings")))

        val grid = Grid<Booking>()
        grid.setItems(bookingService.findUpcomingBookings())

        grid.addColumn { booking -> booking.service.name }
            .setHeader(getTranslation("booking.service"))

        grid.addColumn { booking -> booking.service.provider!!.fullName() }
            .setHeader(getTranslation("booking.provider"))

        grid.addColumn { booking -> booking.startTime.format(formatter) }
            .setHeader(getTranslation("booking.start.time"))

        grid.addColumn { booking -> booking.endTime.format(formatter) }
            .setHeader(getTranslation("booking.end.time"))

        grid.addColumn { booking -> booking.status.name }
            .setHeader(getTranslation("booking.status"))

        grid.setWidthFull()

        layout.add(grid)
        return layout
    }

    override fun beforeEnter(event: BeforeEnterEvent) {
        // Check if already logged in
        if (SecurityUtils.isUserLoggedIn()) {
            redirectToUserDashboard(event)
        }
    }

    private fun redirectToUserDashboard(event: BeforeEnterEvent) {
        if (SecurityUtils.getCurrentUserRole() != UserRole.USER) {
            val targetUrl = determineTargetUrl()
            event.forwardTo(targetUrl)
        }
    }

    private fun determineTargetUrl(): String {
        // Determine the target dashboard based on user role
        return when (SecurityUtils.getCurrentUserRole()) {
            UserRole.ADMIN -> "admin" // AdminDashboardView
            UserRole.PROVIDER -> "provider" // ProviderDashboardView
            UserRole.USER -> "" // DashboardView (main view)
            else -> "" // Default to main dashboard if role not determined
        }
    }

    override fun getPageTitle(): String {
        return "micro-booking :: ${getTranslation("dashboard.title")}"
    }
}