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
import com.vaadin.flow.router.PageTitle
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
@PageTitle("Provider Dashboard | Booking SaaS")
@RolesAllowed("PROVIDER")
class ProviderDashboardView(
    private val userService: UserService,
    private val serviceService: ServiceService,
    private val bookingService: BookingService
) : VerticalLayout() {

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    init {
        addClassName("provider-dashboard-view")
        setSizeFull()

        add(
            H2("Provider Dashboard"),
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
            createStat("Total Services", totalServices.toString()),
            createStat("Active Services", activeServices.toString()),
            createStat("Total Bookings", totalBookings.toString()),
            createStat("Pending Bookings", pendingBookings.toString()),
            createStat("Confirmed Bookings", confirmedBookings.toString())
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

        layout.add(H3("Bookings by Status"))

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
            layout.add(createStatusBar("Pending", pendingCount, totalBookings, "var(--lumo-primary-color)"))

            // Confirmed bookings
            layout.add(createStatusBar("Confirmed", confirmedCount, totalBookings, "var(--lumo-success-color)"))

            // Cancelled bookings
            layout.add(createStatusBar("Cancelled", cancelledCount, totalBookings, "var(--lumo-error-color)"))

            // Completed bookings
            layout.add(createStatusBar("Completed", completedCount, totalBookings, "var(--lumo-success-text-color)"))

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
            layout.add(Span("No bookings yet."))
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
            BookingStat("Pending", pending, calculatePercentage(pending, total)),
            BookingStat("Confirmed", confirmed, calculatePercentage(confirmed, total)),
            BookingStat("Cancelled", cancelled, calculatePercentage(cancelled, total)),
            BookingStat("Completed", completed, calculatePercentage(completed, total)),
            BookingStat("Total", total, 100.0)
        )

        grid.addColumn { it.status }
            .setHeader("Status")
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

    private fun createUpcomingBookingsGrid(): Component {
        val layout = VerticalLayout()
        layout.setPadding(false)
        layout.setSpacing(true)

        layout.add(H3("Upcoming Bookings"))

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
            .setHeader("Service")
            .setAutoWidth(true)

        grid.addColumn { booking -> booking.user.fullName() }
            .setHeader("Customer")
            .setAutoWidth(true)

        grid.addColumn { booking -> booking.user.email }
            .setHeader("Email")
            .setAutoWidth(true)

        grid.addColumn { booking -> booking.startTime.format(formatter) }
            .setHeader("Start Time")
            .setAutoWidth(true)

        grid.addColumn { booking -> booking.endTime.format(formatter) }
            .setHeader("End Time")
            .setAutoWidth(true)

        grid.setWidthFull()

        layout.add(grid)
        return layout
    }

    // Data class for booking statistics
    data class BookingStat(val status: String, val count: Int, val percentage: Double)
}