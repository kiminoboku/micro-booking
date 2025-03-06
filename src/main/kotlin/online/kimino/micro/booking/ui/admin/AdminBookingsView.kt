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
import online.kimino.micro.booking.service.TranslationProvider
import online.kimino.micro.booking.service.UserService
import online.kimino.micro.booking.ui.MainLayout
import java.text.NumberFormat
import java.time.format.DateTimeFormatter

@Route(value = "admin/bookings", layout = MainLayout::class)
@RolesAllowed("ADMIN")
class AdminBookingsView(
    private val bookingService: BookingService,
    private val userService: UserService,
    private val translationProvider: TranslationProvider
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

        header.add(H2(translationProvider.getTranslation("admin.bookings")))

        val navLayout = HorizontalLayout()
        navLayout.setWidthFull()
        navLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.START)
        navLayout.add(
            RouterLink(translationProvider.getTranslation("dashboard.title"), AdminDashboardView::class.java),
            RouterLink(translationProvider.getTranslation("user.title.plural"), AdminUsersView::class.java),
            RouterLink(translationProvider.getTranslation("service.title.plural"), AdminServicesView::class.java)
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
        statusFilter.setPlaceholder(translationProvider.getTranslation("status.filter.placeholder"))
        statusFilter.setItems(BookingStatus.entries.toList())
        statusFilter.setItemLabelGenerator { translationProvider.getTranslation("booking.status.${it.name.lowercase()}") }

        statusFilter.addValueChangeListener { event ->
            if (event.value == null) {
                updateBookingList()
            } else {
                filterBookingsByStatus(event.value)
            }
        }

        val clearButton = Button(translationProvider.getTranslation("filters.clear")) {
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
            .setHeader(translationProvider.getTranslation("booking.service"))
            .setSortable(true)
            .setAutoWidth(true)

        grid.addColumn { booking -> booking.service.provider!!.fullName() }
            .setHeader(translationProvider.getTranslation("booking.provider"))
            .setSortable(true)
            .setAutoWidth(true)

        grid.addColumn { booking -> booking.user.fullName() }
            .setHeader(translationProvider.getTranslation("booking.customer"))
            .setSortable(true)
            .setAutoWidth(true)

        grid.addColumn { booking -> booking.startTime.format(formatter) }
            .setHeader(translationProvider.getTranslation("booking.start.time"))
            .setSortable(true)
            .setAutoWidth(true)

        grid.addColumn { booking -> booking.endTime.format(formatter) }
            .setHeader(translationProvider.getTranslation("booking.end.time"))
            .setAutoWidth(true)

        grid.addComponentColumn { booking ->
            val statusSpan =
                Span(translationProvider.getTranslation("booking.status.${booking.status.name.lowercase()}"))

            // Style based on status
            when (booking.status) {
                BookingStatus.PENDING -> statusSpan.element.style.set("color", "var(--lumo-primary-color)")
                BookingStatus.CONFIRMED -> statusSpan.element.style.set("color", "var(--lumo-success-color)")
                BookingStatus.CANCELLED -> statusSpan.element.style.set("color", "var(--lumo-error-color)")
                BookingStatus.COMPLETED -> statusSpan.element.style.set("color", "var(--lumo-success-text-color)")
            }

            statusSpan
        }
            .setHeader(translationProvider.getTranslation("booking.status"))
            .setAutoWidth(true)

        grid.addComponentColumn { booking -> createBookingActionButtons(booking) }
            .setHeader(translationProvider.getTranslation("common.actions"))
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
        detailsButton.element.setAttribute("title", translationProvider.getTranslation("booking.action.details"))

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
                confirmButton.element.setAttribute("title", translationProvider.getTranslation("common.confirm"))

                // Cancel button
                val cancelButton = Button(Icon(VaadinIcon.BAN)) {
                    updateBookingStatus(booking, BookingStatus.CANCELLED)
                }
                cancelButton.addThemeVariants(
                    ButtonVariant.LUMO_SMALL,
                    ButtonVariant.LUMO_TERTIARY,
                    ButtonVariant.LUMO_ERROR
                )
                cancelButton.element.setAttribute("title", translationProvider.getTranslation("common.cancel"))

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
                completeButton.element.setAttribute("title", translationProvider.getTranslation("booking.complete"))

                // Cancel button
                val cancelButton = Button(Icon(VaadinIcon.BAN)) {
                    updateBookingStatus(booking, BookingStatus.CANCELLED)
                }
                cancelButton.addThemeVariants(
                    ButtonVariant.LUMO_SMALL,
                    ButtonVariant.LUMO_TERTIARY,
                    ButtonVariant.LUMO_ERROR
                )
                cancelButton.element.setAttribute("title", translationProvider.getTranslation("common.cancel"))

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
        dialog.headerTitle = translationProvider.getTranslation("booking.details")

        val content = VerticalLayout()
        content.isPadding = true
        content.isSpacing = true
        content.width = "400px"

        content.add(
            createDetailItem("${translationProvider.getTranslation("booking.service")}:", booking.service.name),
            createDetailItem(
                "${translationProvider.getTranslation("booking.price")}:",
                NumberFormat.getCurrencyInstance().format(booking.service.price)
            ),
            createDetailItem(
                "${translationProvider.getTranslation("booking.provider")}:",
                booking.service.provider!!.fullName()
            ),
            createDetailItem(
                "${translationProvider.getTranslation("booking.provider.email")}:",
                booking.service.provider!!.email
            ),
            createDetailItem("${translationProvider.getTranslation("booking.customer")}:", booking.user.fullName()),
            createDetailItem("${translationProvider.getTranslation("booking.customer.email")}:", booking.user.email),
            createDetailItem(
                "${translationProvider.getTranslation("booking.start.time")}:",
                booking.startTime.format(formatter)
            ),
            createDetailItem(
                "${translationProvider.getTranslation("booking.end.time")}:",
                booking.endTime.format(formatter)
            ),
            createDetailItem(
                "${translationProvider.getTranslation("booking.duration")}:",
                translationProvider.getTranslation("common.minutes", arrayOf(booking.service.duration))
            ),
            createDetailItem(
                "${translationProvider.getTranslation("booking.status")}:",
                translationProvider.getTranslation("booking.status.${booking.status.name.lowercase()}")
            ),
            createDetailItem(
                "${translationProvider.getTranslation("common.created.at")}:",
                booking.createdAt.format(formatter)
            ),
            createDetailItem(
                "${translationProvider.getTranslation("common.last.updated.at")}:",
                booking.updatedAt.format(formatter)
            )
        )

        // Notes section
        val notesSection = VerticalLayout()
        notesSection.setPadding(false)
        notesSection.setSpacing(true)

        val notesLabel = Span("${translationProvider.getTranslation("notes.plural")}:")
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
            val saveButton = Button(translationProvider.getTranslation("notes.save")) {
                try {
                    bookingService.updateBookingNotes(booking.id, notesValue.value)
                    Notification.show(translationProvider.getTranslation("notes.updated")).apply {
                        position = Notification.Position.MIDDLE
                        addThemeVariants(NotificationVariant.LUMO_SUCCESS)
                    }
                    dialog.close()
                    updateBookingList()
                } catch (e: Exception) {
                    Notification.show(
                        translationProvider.getTranslation(
                            "notes.update.fail",
                            arrayOf(e.message ?: "undefined")
                        )
                    ).apply {
                        position = Notification.Position.MIDDLE
                        addThemeVariants(NotificationVariant.LUMO_ERROR)
                    }
                }
            }
            saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY)
            content.add(saveButton)
        }

        // Close button
        val closeButton = Button(translationProvider.getTranslation("common.close")) {
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
                translationProvider.getTranslation(
                    "booking.status.updated.to",
                    arrayOf(translationProvider.getTranslation("booking.status.${newStatus.name.lowercase()}"))
                )
            ).apply {
                position = Notification.Position.MIDDLE
                addThemeVariants(NotificationVariant.LUMO_SUCCESS)
            }

            updateBookingList()
        } catch (e: Exception) {
            Notification.show(
                translationProvider.getTranslation(
                    "booking.status.update.failed",
                    arrayOf(e.message ?: "undefined")
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

    override fun getPageTitle() = "micro-booking :: ${translationProvider.getTranslation("admin.bookings")}"
}