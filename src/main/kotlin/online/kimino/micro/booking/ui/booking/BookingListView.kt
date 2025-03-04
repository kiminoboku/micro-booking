package online.kimino.micro.booking.ui.booking

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
import com.vaadin.flow.component.tabs.Tab
import com.vaadin.flow.component.tabs.Tabs
import com.vaadin.flow.component.textfield.TextArea
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.PermitAll
import online.kimino.micro.booking.entity.Booking
import online.kimino.micro.booking.entity.BookingStatus
import online.kimino.micro.booking.entity.UserRole
import online.kimino.micro.booking.security.SecurityUtils
import online.kimino.micro.booking.service.BookingService
import online.kimino.micro.booking.service.UserService
import online.kimino.micro.booking.ui.MainLayout
import java.time.format.DateTimeFormatter

@Route(value = "bookings", layout = MainLayout::class)
@PageTitle("My Bookings | Booking SaaS")
@PermitAll
class BookingListView(
    private val bookingService: BookingService,
    private val userService: UserService
) : VerticalLayout() {

    private val grid = Grid<Booking>()
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    private val tabs: Tabs
    private val allTab = Tab("All Bookings")
    private val pendingTab = Tab("Pending")
    private val confirmedTab = Tab("Confirmed")
    private val completedTab = Tab("Completed")
    private val cancelledTab = Tab("Cancelled")

    init {
        addClassName("booking-list-view")
        setSizeFull()

        tabs = Tabs(allTab, pendingTab, confirmedTab, completedTab, cancelledTab)
        tabs.addSelectedChangeListener { updateBookingList() }

        configureGrid()

        val title = if (SecurityUtils.hasRole(UserRole.PROVIDER)) {
            "Customer Bookings"
        } else {
            "My Bookings"
        }

        add(
            H2(title),
            tabs,
            grid
        )

        updateBookingList()
    }

    private fun configureGrid() {
        grid.addClassName("bookings-grid")
        grid.setSizeFull()

        grid.addColumn { booking -> booking.service.name }
            .setHeader("Service")
            .setSortable(true)
            .setAutoWidth(true)

        // Different columns based on role
        if (SecurityUtils.hasRole(UserRole.PROVIDER)) {
            grid.addColumn { booking -> booking.user.fullName() }
                .setHeader("Customer")
                .setSortable(true)
                .setAutoWidth(true)

            grid.addColumn { booking -> booking.user.email }
                .setHeader("Email")
                .setAutoWidth(true)
        } else {
            grid.addColumn { booking -> booking.service.provider!!.fullName() }
                .setHeader("Provider")
                .setSortable(true)
                .setAutoWidth(true)
        }

        grid.addColumn { booking -> booking.startTime.format(formatter) }
            .setHeader("Start Time")
            .setSortable(true)
            .setAutoWidth(true)

        grid.addColumn { booking -> booking.endTime.format(formatter) }
            .setHeader("End Time")
            .setAutoWidth(true)

        grid.addComponentColumn { booking ->
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

        grid.addComponentColumn { booking -> createActionButtons(booking) }
            .setHeader("Actions")
            .setAutoWidth(true)

        grid.getColumns().forEach { it.setResizable(true) }
    }

    private fun createActionButtons(booking: Booking): HorizontalLayout {
        val buttonLayout = HorizontalLayout()
        buttonLayout.isPadding = false
        buttonLayout.isSpacing = true

        val viewButton = Button(Icon(VaadinIcon.EYE)) {
            showBookingDetails(booking)
        }
        viewButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY)
        viewButton.element.setAttribute("title", "View Details")

        buttonLayout.add(viewButton)

        // Add different action buttons based on booking status and user role
        if (SecurityUtils.hasRole(UserRole.PROVIDER)) {
            when (booking.status) {
                BookingStatus.PENDING -> {
                    // Confirm button
                    val confirmButton = Button(Icon(VaadinIcon.CHECK)) {
                        confirmBooking(booking)
                    }
                    confirmButton.addThemeVariants(
                        ButtonVariant.LUMO_SMALL,
                        ButtonVariant.LUMO_SUCCESS,
                        ButtonVariant.LUMO_TERTIARY
                    )
                    confirmButton.element.setAttribute("title", "Confirm")

                    // Reject button
                    val rejectButton = Button(Icon(VaadinIcon.CLOSE_SMALL)) {
                        cancelBooking(booking)
                    }
                    rejectButton.addThemeVariants(
                        ButtonVariant.LUMO_SMALL,
                        ButtonVariant.LUMO_ERROR,
                        ButtonVariant.LUMO_TERTIARY
                    )
                    rejectButton.element.setAttribute("title", "Reject")

                    buttonLayout.add(confirmButton, rejectButton)
                }

                BookingStatus.CONFIRMED -> {
                    // Complete button
                    val completeButton = Button(Icon(VaadinIcon.CHECK_CIRCLE)) {
                        completeBooking(booking)
                    }
                    completeButton.addThemeVariants(
                        ButtonVariant.LUMO_SMALL,
                        ButtonVariant.LUMO_SUCCESS,
                        ButtonVariant.LUMO_TERTIARY
                    )
                    completeButton.element.setAttribute("title", "Mark as Completed")

                    // Cancel button
                    val cancelButton = Button(Icon(VaadinIcon.CLOSE_CIRCLE)) {
                        cancelBooking(booking)
                    }
                    cancelButton.addThemeVariants(
                        ButtonVariant.LUMO_SMALL,
                        ButtonVariant.LUMO_ERROR,
                        ButtonVariant.LUMO_TERTIARY
                    )
                    cancelButton.element.setAttribute("title", "Cancel")

                    buttonLayout.add(completeButton, cancelButton)
                }

                else -> {
                    // No additional action buttons for completed or cancelled bookings
                }
            }
        } else {
            // Regular user
            if (booking.status == BookingStatus.PENDING || booking.status == BookingStatus.CONFIRMED) {
                val cancelButton = Button(Icon(VaadinIcon.CLOSE_CIRCLE)) {
                    cancelBooking(booking)
                }
                cancelButton.addThemeVariants(
                    ButtonVariant.LUMO_SMALL,
                    ButtonVariant.LUMO_ERROR,
                    ButtonVariant.LUMO_TERTIARY
                )
                cancelButton.element.setAttribute("title", "Cancel")

                buttonLayout.add(cancelButton)
            }
        }

        return buttonLayout
    }

    private fun updateBookingList() {
        val currentUser = SecurityUtils.getCurrentUsername()?.let {
            userService.findByEmail(it).orElse(null)
        }

        if (currentUser == null) {
            grid.setItems(emptyList())
            return
        }

        val bookings = when {
            SecurityUtils.hasRole(UserRole.PROVIDER) -> {
                when (tabs.selectedTab) {
                    pendingTab -> bookingService.findAllByProviderIdAndStatus(currentUser.id, BookingStatus.PENDING)
                    confirmedTab -> bookingService.findAllByProviderIdAndStatus(currentUser.id, BookingStatus.CONFIRMED)
                    completedTab -> bookingService.findAllByProviderIdAndStatus(currentUser.id, BookingStatus.COMPLETED)
                    cancelledTab -> bookingService.findAllByProviderIdAndStatus(currentUser.id, BookingStatus.CANCELLED)
                    else -> bookingService.findAllByProviderId(currentUser.id)
                }
            }

            else -> {
                when (tabs.selectedTab) {
                    pendingTab -> bookingService.findAllByUserIdAndStatus(currentUser.id, BookingStatus.PENDING)
                    confirmedTab -> bookingService.findAllByUserIdAndStatus(currentUser.id, BookingStatus.CONFIRMED)
                    completedTab -> bookingService.findAllByUserIdAndStatus(currentUser.id, BookingStatus.COMPLETED)
                    cancelledTab -> bookingService.findAllByUserIdAndStatus(currentUser.id, BookingStatus.CANCELLED)
                    else -> bookingService.findAllByUserId(currentUser.id)
                }
            }
        }

        grid.setItems(bookings)
    }

    private fun showBookingDetails(booking: Booking) {
        val dialog = Dialog()
        dialog.headerTitle = "Booking Details"

        val content = VerticalLayout()
        content.isPadding = true
        content.setSpacing(true)
        content.width = "400px"

        content.add(
            createDetailItem("Service:", booking.service.name),
            createDetailItem("Provider:", booking.service.provider!!.fullName()),
            createDetailItem("Customer:", booking.user.fullName()),
            createDetailItem("Start Time:", booking.startTime.format(formatter)),
            createDetailItem("End Time:", booking.endTime.format(formatter)),
            createDetailItem("Status:", booking.status.name),
            createDetailItem("Notes:", booking.notes ?: "No notes")
        )

        // Add text area for adding/editing notes if the booking is not cancelled or completed
        if (booking.status == BookingStatus.PENDING || booking.status == BookingStatus.CONFIRMED) {
            val notesField = TextArea("Update Notes")
            notesField.value = booking.notes ?: ""
            notesField.width = "100%"

            val saveButton = Button("Save Notes") {
                try {
                    bookingService.updateBookingNotes(booking.id, notesField.value)
                    Notification.show("Notes updated").apply {
                        position = Notification.Position.MIDDLE
                        addThemeVariants(NotificationVariant.LUMO_SUCCESS)
                    }
                    dialog.close()
                    updateBookingList()
                } catch (e: Exception) {
                    Notification.show("Failed to update notes: ${e.message}").apply {
                        position = Notification.Position.MIDDLE
                        addThemeVariants(NotificationVariant.LUMO_ERROR)
                    }
                }
            }

            content.add(notesField, saveButton)
        }

        dialog.add(content)
        dialog.open()
    }

    private fun createDetailItem(label: String, value: String): HorizontalLayout {
        val layout = HorizontalLayout()
        layout.defaultVerticalComponentAlignment = FlexComponent.Alignment.BASELINE

        val labelSpan = Span(label)
        labelSpan.style.set("font-weight", "bold")

        val valueSpan = Span(value)

        layout.add(labelSpan, valueSpan)
        return layout
    }

    private fun confirmBooking(booking: Booking) {
        try {
            bookingService.updateBookingStatus(booking.id, BookingStatus.CONFIRMED)
            Notification.show("Booking confirmed").apply {
                position = Notification.Position.MIDDLE
                addThemeVariants(NotificationVariant.LUMO_SUCCESS)
            }
            updateBookingList()
        } catch (e: Exception) {
            Notification.show("Failed to confirm booking: ${e.message}").apply {
                position = Notification.Position.MIDDLE
                addThemeVariants(NotificationVariant.LUMO_ERROR)
            }
        }
    }

    private fun completeBooking(booking: Booking) {
        try {
            bookingService.updateBookingStatus(booking.id, BookingStatus.COMPLETED)
            Notification.show("Booking marked as completed").apply {
                position = Notification.Position.MIDDLE
                addThemeVariants(NotificationVariant.LUMO_SUCCESS)
            }
            updateBookingList()
        } catch (e: Exception) {
            Notification.show("Failed to complete booking: ${e.message}").apply {
                position = Notification.Position.MIDDLE
                addThemeVariants(NotificationVariant.LUMO_ERROR)
            }
        }
    }

    private fun cancelBooking(booking: Booking) {
        val dialog = Dialog()
        dialog.headerTitle = "Cancel Booking"

        val content = VerticalLayout()
        content.isPadding = true
        content.setSpacing(true)

        content.add(
            Span("Are you sure you want to cancel this booking?"),
            createDetailItem("Service:", booking.service.name),
            createDetailItem("Time:", booking.startTime.format(formatter))
        )

        val buttonLayout = HorizontalLayout()
        buttonLayout.justifyContentMode = FlexComponent.JustifyContentMode.END

        val cancelButton = Button("No, Keep it") {
            dialog.close()
        }

        val confirmButton = Button("Yes, Cancel Booking") {
            try {
                bookingService.updateBookingStatus(booking.id, BookingStatus.CANCELLED)
                Notification.show("Booking cancelled").apply {
                    position = Notification.Position.MIDDLE
                    addThemeVariants(NotificationVariant.LUMO_SUCCESS)
                }
                dialog.close()
                updateBookingList()
            } catch (e: Exception) {
                Notification.show("Failed to cancel booking: ${e.message}").apply {
                    position = Notification.Position.MIDDLE
                    addThemeVariants(NotificationVariant.LUMO_ERROR)
                }
            }
        }
        confirmButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR)

        buttonLayout.add(cancelButton, confirmButton)
        content.add(buttonLayout)

        dialog.add(content)
        dialog.open()
    }
}