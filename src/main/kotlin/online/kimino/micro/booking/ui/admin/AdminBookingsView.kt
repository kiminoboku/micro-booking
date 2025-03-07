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
import com.vaadin.flow.component.textfield.TextArea
import com.vaadin.flow.router.HasDynamicTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.router.RouterLink
import jakarta.annotation.security.RolesAllowed
import online.kimino.micro.booking.entity.Booking
import online.kimino.micro.booking.entity.BookingStatus
import online.kimino.micro.booking.entity.UserRole
import online.kimino.micro.booking.service.BookingService
import online.kimino.micro.booking.service.UserService
import online.kimino.micro.booking.ui.MainLayout
import java.text.NumberFormat
import java.time.format.DateTimeFormatter

@Route(value = "admin/bookings", layout = MainLayout::class)
@RolesAllowed("ADMIN")
class AdminBookingsView(
    private val bookingService: BookingService,
    private val userService: UserService
) : VerticalLayout(), HasDynamicTitle {

    private val grid = Grid<Booking>()
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    init {
        addClassName("admin-bookings-view")
        setSizeFull()

        add(
            createHeaderWithNavigation(),
            createFilters(),
            configureGrid()
        )

        updateBookingList()
    }

    private fun createHeaderWithNavigation(): Component {
        val header = VerticalLayout()
        header.setPadding(false)
        header.setSpacing(true)

        header.add(H2(getTranslation("admin.bookings")))

        val navLayout = HorizontalLayout()
        navLayout.setWidthFull()
        navLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.START)
        navLayout.add(
            RouterLink(getTranslation("dashboard.title"), AdminDashboardView::class.java),
            RouterLink(getTranslation("user.title.plural"), AdminUsersView::class.java),
            RouterLink(getTranslation("service.title.plural"), AdminServicesView::class.java)
        )

        header.add(navLayout)
        return header
    }

    private fun createFilters(): Component {
        val filterLayout = HorizontalLayout()
        filterLayout.width = "100%"
        filterLayout.setPadding(true)
        filterLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.START)

        val statusFilter = com.vaadin.flow.component.combobox.ComboBox<BookingStatus>()
        statusFilter.setPlaceholder(getTranslation("status.filter.placeholder"))
        statusFilter.setItems(BookingStatus.entries.toList())
        statusFilter.setItemLabelGenerator { getTranslation("booking.status.${it.name.lowercase()}") }

        statusFilter.addValueChangeListener { event ->
            if (event.value == null) {
                updateBookingList()
            } else {
                filterBookingsByStatus(event.value)
            }
        }

        val clearButton = Button(getTranslation("filters.clear")) {
            statusFilter.clear()
            updateBookingList()
        }
        clearButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL)

        filterLayout.add(statusFilter, clearButton)
        return filterLayout
    }

    private fun configureGrid(): Grid<Booking> {
        grid.addClassName("bookings-grid")
        grid.setSizeFull()

        grid.addColumn { booking -> booking.service.name }
            .setHeader(getTranslation("booking.service"))
            .setSortable(true)
            .setAutoWidth(true)

        grid.addColumn { booking -> booking.service.provider!!.fullName() }
            .setHeader(getTranslation("booking.provider"))
            .setSortable(true)
            .setAutoWidth(true)

        grid.addColumn { booking -> booking.user.fullName() }
            .setHeader(getTranslation("booking.customer"))
            .setSortable(true)
            .setAutoWidth(true)

        grid.addColumn { booking -> booking.startTime.format(formatter) }
            .setHeader(getTranslation("booking.start.time"))
            .setSortable(true)
            .setAutoWidth(true)

        grid.addColumn { booking -> booking.endTime.format(formatter) }
            .setHeader(getTranslation("booking.end.time"))
            .setAutoWidth(true)

        grid.addComponentColumn { booking ->
            val statusSpan =
                Span(getTranslation("booking.status.${booking.status.name.lowercase()}"))

            // Style based on status
            when (booking.status) {
                BookingStatus.PENDING -> statusSpan.element.style.set("color", "var(--lumo-primary-color)")
                BookingStatus.CONFIRMED -> statusSpan.element.style.set("color", "var(--lumo-success-color)")
                BookingStatus.CANCELLED -> statusSpan.element.style.set("color", "var(--lumo-error-color)")
                BookingStatus.COMPLETED -> statusSpan.element.style.set("color", "var(--lumo-success-text-color)")
            }

            statusSpan
        }
            .setHeader(getTranslation("booking.status"))
            .setAutoWidth(true)

        grid.addComponentColumn { booking -> createBookingActionButtons(booking) }
            .setHeader(getTranslation("common.actions"))
            .setAutoWidth(true)

        grid.getColumns().forEach { it.setResizable(true) }

        return grid
    }

    private fun createBookingActionButtons(booking: Booking): HorizontalLayout {
        val buttonLayout = HorizontalLayout()
        buttonLayout.isPadding = false
        buttonLayout.isSpacing = true

        // View details button
        val detailsButton = Button(Icon(VaadinIcon.INFO_CIRCLE)) {
            showBookingDetails(booking)
        }
        detailsButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY)
        detailsButton.element.setAttribute("title", getTranslation("booking.action.details"))

        // Add status change buttons based on current status
        when (booking.status) {
            BookingStatus.PENDING -> {
                // Confirm button
                val confirmButton = Button(Icon(VaadinIcon.CHECK)) {
                    updateBookingStatus(booking, BookingStatus.CONFIRMED)
                }
                confirmButton.addThemeVariants(
                    ButtonVariant.LUMO_SMALL,
                    ButtonVariant.LUMO_TERTIARY,
                    ButtonVariant.LUMO_SUCCESS
                )
                confirmButton.element.setAttribute("title", getTranslation("common.confirm"))

                // Cancel button
                val cancelButton = Button(Icon(VaadinIcon.BAN)) {
                    updateBookingStatus(booking, BookingStatus.CANCELLED)
                }
                cancelButton.addThemeVariants(
                    ButtonVariant.LUMO_SMALL,
                    ButtonVariant.LUMO_TERTIARY,
                    ButtonVariant.LUMO_ERROR
                )
                cancelButton.element.setAttribute("title", getTranslation("common.cancel"))

                buttonLayout.add(detailsButton, confirmButton, cancelButton)
            }

            BookingStatus.CONFIRMED -> {
                // Complete button
                val completeButton = Button(Icon(VaadinIcon.CHECK_CIRCLE)) {
                    updateBookingStatus(booking, BookingStatus.COMPLETED)
                }
                completeButton.addThemeVariants(
                    ButtonVariant.LUMO_SMALL,
                    ButtonVariant.LUMO_TERTIARY,
                    ButtonVariant.LUMO_SUCCESS
                )
                completeButton.element.setAttribute("title", getTranslation("booking.complete"))

                // Cancel button
                val cancelButton = Button(Icon(VaadinIcon.BAN)) {
                    updateBookingStatus(booking, BookingStatus.CANCELLED)
                }
                cancelButton.addThemeVariants(
                    ButtonVariant.LUMO_SMALL,
                    ButtonVariant.LUMO_TERTIARY,
                    ButtonVariant.LUMO_ERROR
                )
                cancelButton.element.setAttribute("title", getTranslation("common.cancel"))

                buttonLayout.add(detailsButton, completeButton, cancelButton)
            }

            else -> {
                // Just show details for completed or cancelled bookings
                buttonLayout.add(detailsButton)
            }
        }

        return buttonLayout
    }

    private fun showBookingDetails(booking: Booking) {
        val dialog = Dialog()
        dialog.headerTitle = getTranslation("booking.details")

        val content = VerticalLayout()
        content.isPadding = true
        content.isSpacing = true
        content.width = "400px"

        content.add(
            createDetailItem("${getTranslation("booking.service")}:", booking.service.name),
            createDetailItem(
                "${getTranslation("booking.price")}:",
                NumberFormat.getCurrencyInstance().format(booking.service.price)
            ),
            createDetailItem(
                "${getTranslation("booking.provider")}:",
                booking.service.provider!!.fullName()
            ),
            createDetailItem(
                "${getTranslation("booking.provider.email")}:",
                booking.service.provider!!.email
            ),
            createDetailItem("${getTranslation("booking.customer")}:", booking.user.fullName()),
            createDetailItem("${getTranslation("booking.customer.email")}:", booking.user.email),
            createDetailItem(
                "${getTranslation("booking.start.time")}:", booking.startTime.format(formatter)
            ),
            createDetailItem(
                "${getTranslation("booking.end.time")}:", booking.endTime.format(formatter)
            ),
            createDetailItem(
                "${getTranslation("booking.duration")}:",
                getTranslation("common.minutes", arrayOf(booking.service.duration))
            ),
            createDetailItem(
                "${getTranslation("booking.status")}:",
                getTranslation("booking.status.${booking.status.name.lowercase()}")
            ),
            createDetailItem(
                "${getTranslation("common.created.at")}:",
                booking.createdAt.format(formatter)
            ),
            createDetailItem(
                "${getTranslation("common.last.updated.at")}:",
                booking.updatedAt.format(formatter)
            )
        )

        // Notes section
        val notesSection = VerticalLayout()
        notesSection.setPadding(false)
        notesSection.setSpacing(true)

        val notesLabel = Span("${getTranslation("notes.plural")}:")
        notesLabel.style.set("font-weight", "bold")

        val notesValue = TextArea()
        notesValue.value = booking.notes ?: ""
        notesValue.isReadOnly = booking.status == BookingStatus.COMPLETED || booking.status == BookingStatus.CANCELLED
        notesValue.width = "100%"
        notesValue.minHeight = "100px"

        notesSection.add(notesLabel, notesValue)
        content.add(notesSection)

        // Button for saving notes
        if (booking.status != BookingStatus.COMPLETED && booking.status != BookingStatus.CANCELLED) {
            val saveButton = Button(getTranslation("notes.save")) {
                try {
                    bookingService.updateBookingNotes(booking.id, notesValue.value)
                    Notification.show(getTranslation("notes.updated")).apply {
                        position = Notification.Position.MIDDLE
                        addThemeVariants(NotificationVariant.LUMO_SUCCESS)
                    }
                    dialog.close()
                    updateBookingList()
                } catch (e: Exception) {
                    Notification.show(getTranslation("notes.update.fail", e.message ?: "undefined")).apply {
                        position = Notification.Position.MIDDLE
                        addThemeVariants(NotificationVariant.LUMO_ERROR)
                    }
                }
            }
            saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY)
            content.add(saveButton)
        }

        // Close button
        val closeButton = Button(getTranslation("common.close")) {
            dialog.close()
        }
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY)
        content.add(closeButton)

        dialog.add(content)
        dialog.open()
    }

    private fun createDetailItem(label: String, value: String): HorizontalLayout {
        val layout = HorizontalLayout()
        layout.setWidthFull()
        layout.defaultVerticalComponentAlignment = FlexComponent.Alignment.BASELINE

        val labelSpan = Span(label)
        labelSpan.style.set("font-weight", "bold")
        labelSpan.width = "120px"

        val valueSpan = Span(value)

        layout.add(labelSpan, valueSpan)
        return layout
    }

    private fun updateBookingStatus(booking: Booking, newStatus: BookingStatus) {
        try {
            bookingService.updateBookingStatus(booking.id, newStatus)

            Notification.show(
                getTranslation(
                    "booking.status.updated.to", getTranslation("booking.status.${newStatus.name.lowercase()}")
                )
            ).apply {
                position = Notification.Position.MIDDLE
                addThemeVariants(NotificationVariant.LUMO_SUCCESS)
            }

            updateBookingList()
        } catch (e: Exception) {
            Notification.show(
                getTranslation(
                    "booking.status.update.failed", e.message ?: "undefined"
                )
            ).apply {
                position = Notification.Position.MIDDLE
                addThemeVariants(NotificationVariant.LUMO_ERROR)
            }
        }
    }

    private fun updateBookingList() {
        // Get all bookings from all providers
        val allBookings = mutableListOf<Booking>()
        userService.getAllUsers().forEach { user ->
            if (user.role == UserRole.PROVIDER) {
                allBookings.addAll(bookingService.findAllByProviderId(user.id))
            }
        }

        grid.setItems(allBookings)
    }

    private fun filterBookingsByStatus(status: BookingStatus) {
        // Get all bookings and filter by status
        val filteredBookings = mutableListOf<Booking>()
        userService.getAllUsers().forEach { user ->
            if (user.role == UserRole.PROVIDER) {
                filteredBookings.addAll(bookingService.findAllByProviderIdAndStatus(user.id, status))
            }
        }

        grid.setItems(filteredBookings)
    }

    override fun getPageTitle() = "micro-booking :: ${getTranslation("admin.bookings")}"
}