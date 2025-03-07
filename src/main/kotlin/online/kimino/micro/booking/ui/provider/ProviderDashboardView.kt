package online.kimino.micro.booking.ui.provider

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.html.H2
import com.vaadin.flow.component.html.H3
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.progressbar.ProgressBar
import com.vaadin.flow.router.HasDynamicTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed
import online.kimino.micro.booking.entity.Booking
import online.kimino.micro.booking.entity.BookingStatus
import online.kimino.micro.booking.security.SecurityUtils
import online.kimino.micro.booking.service.BookingService
import online.kimino.micro.booking.service.ServiceService
import online.kimino.micro.booking.service.UserService
import online.kimino.micro.booking.ui.MainLayout
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Route(value = "provider", layout = MainLayout::class)
@RolesAllowed("PROVIDER")
class ProviderDashboardView(
    private val userService: UserService,
    private val serviceService: ServiceService,
    private val bookingService: BookingService
) : VerticalLayout(), HasDynamicTitle {

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    init {
        addClassName("provider-dashboard-view")
        setSizeFull()

        add(
            H2(getTranslation("provider.dashboard")),
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

        val currentUser = SecurityUtils.getCurrentUsername()?.let {
            userService.findByEmail(it).orElse(null)
        }

        if (currentUser == null) {
            return VerticalLayout()
        }

        val totalServices = serviceService.findAllByProvider(currentUser.id).size
        val activeServices = serviceService.findAllActiveByProvider(currentUser).size

        val totalBookings = bookingService.findAllByProviderId(currentUser.id).size
        val pendingBookings = bookingService.findAllByProviderIdAndStatus(currentUser.id, BookingStatus.PENDING).size
        val confirmedBookings =
            bookingService.findAllByProviderIdAndStatus(currentUser.id, BookingStatus.CONFIRMED).size

        stats.add(
            createStat(getTranslation("admin.stats.total.services"), totalServices.toString()),
            createStat(getTranslation("service.active"), activeServices.toString()),
            createStat(getTranslation("admin.stats.total.bookings"), totalBookings.toString()),
            createStat(getTranslation("booking.status.pending"), pendingBookings.toString()),
            createStat(getTranslation("booking.status.confirmed"), confirmedBookings.toString())
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

    private fun createBookingStatusVisualization(): Component {
        val layout = VerticalLayout()
        layout.setPadding(true)
        layout.setSpacing(true)
        layout.style.set("background-color", "var(--lumo-base-color)")
        layout.style.set("border-radius", "var(--lumo-border-radius-m)")
        layout.style.set("box-shadow", "var(--lumo-box-shadow-xs)")
        layout.width = "100%"
        layout.isMargin = true

        layout.add(H3(getTranslation("admin.bookings.by.status")))

        // Count bookings by status
        val providerId = SecurityUtils.getCurrentUsername()?.let {
            userService.findByEmail(it).map { it.id }.orElse(0)
        } ?: 0

        val pendingCount = bookingService.findAllByProviderIdAndStatus(providerId, BookingStatus.PENDING).size
        val confirmedCount = bookingService.findAllByProviderIdAndStatus(providerId, BookingStatus.CONFIRMED).size
        val cancelledCount = bookingService.findAllByProviderIdAndStatus(providerId, BookingStatus.CANCELLED).size
        val completedCount = bookingService.findAllByProviderIdAndStatus(providerId, BookingStatus.COMPLETED).size

        val totalBookings = pendingCount + confirmedCount + cancelledCount + completedCount

        if (totalBookings > 0) {
            // Pending bookings
            layout.add(createStatusBar(getTranslation("booking.status.pending"), pendingCount, totalBookings, "var(--lumo-primary-color)"))

            // Confirmed bookings
            layout.add(createStatusBar(getTranslation("booking.status.confirmed"), confirmedCount, totalBookings, "var(--lumo-success-color)"))

            // Cancelled bookings
            layout.add(createStatusBar(getTranslation("booking.status.cancelled"), cancelledCount, totalBookings, "var(--lumo-error-color)"))

            // Completed bookings
            layout.add(createStatusBar(getTranslation("booking.status.completed"), completedCount, totalBookings, "var(--lumo-success-text-color)"))

            // Add booking statistics table
            val statisticsTable = createBookingStatisticsTable(
                pendingCount,
                confirmedCount,
                cancelledCount,
                completedCount,
                totalBookings
            )
            layout.add(statisticsTable)
        } else {
            layout.add(Span(getTranslation("admin.no.bookings")))
        }

        return layout
    }

    private fun createBookingStatisticsTable(
        pending: Int,
        confirmed: Int,
        cancelled: Int,
        completed: Int,
        total: Int
    ): Component {
        val grid = Grid<BookingStat>()
        grid.setItems(
            BookingStat(getTranslation("booking.status.pending"), pending, calculatePercentage(pending, total)),
            BookingStat(getTranslation("booking.status.confirmed"), confirmed, calculatePercentage(confirmed, total)),
            BookingStat(getTranslation("booking.status.cancelled"), cancelled, calculatePercentage(cancelled, total)),
            BookingStat(getTranslation("booking.status.completed"), completed, calculatePercentage(completed, total)),
            BookingStat(getTranslation("admin.total"), total, 100.0)
        )

        grid.addColumn { it.status }
            .setHeader(getTranslation("booking.status"))
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

    private fun createUpcomingBookingsGrid(): Component {
        val layout = VerticalLayout()
        layout.setPadding(false)
        layout.setSpacing(true)

        layout.add(H3(getTranslation("dashboard.upcoming.bookings")))

        val grid = Grid<Booking>()

        val providerId = SecurityUtils.getCurrentUsername()?.let {
            userService.findByEmail(it).map { it.id }.orElse(0)
        } ?: 0

        val upcomingBookings = bookingService.findAllByProviderIdAndStatus(providerId, BookingStatus.CONFIRMED)
            .filter { booking -> booking.startTime.isAfter(LocalDateTime.now()) }
            .sortedBy { it.startTime }
            .take(10)

        grid.setItems(upcomingBookings)

        grid.addColumn { booking -> booking.service.name }
            .setHeader(getTranslation("booking.service"))
            .setAutoWidth(true)

        grid.addColumn { booking -> booking.user.fullName() }
            .setHeader(getTranslation("booking.customer"))
            .setAutoWidth(true)

        grid.addColumn { booking -> booking.user.email }
            .setHeader(getTranslation("auth.email"))
            .setAutoWidth(true)

        grid.addColumn { booking -> booking.startTime.format(formatter) }
            .setHeader(getTranslation("booking.start.time"))
            .setAutoWidth(true)

        grid.addColumn { booking -> booking.endTime.format(formatter) }
            .setHeader(getTranslation("booking.end.time"))
            .setAutoWidth(true)

        grid.setWidthFull()

        layout.add(grid)
        return layout
    }

    // Data class for booking statistics
    data class BookingStat(val status: String, val count: Int, val percentage: Double)

    override fun getPageTitle() = "micro-booking :: ${getTranslation("provider.dashboard")}"
}