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
import com.vaadin.flow.router.HasDynamicTitle
import com.vaadin.flow.router.Route
import io.github.oshai.kotlinlogging.KotlinLogging
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
@PermitAll
class BookingListView(
    private val bookingService: BookingService,
    private val userService: UserService
) : VerticalLayout(), HasDynamicTitle {

    private val logger = KotlinLogging.logger {}
    private val grid = Grid<Booking>()
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    private val tabs: Tabs
    private val allTab = Tab(getTranslation("booking.list"))
    private val pendingTab = Tab(getTranslation("booking.status.pending"))
    private val confirmedTab = Tab(getTranslation("booking.status.confirmed"))
    private val completedTab = Tab(getTranslation("booking.status.completed"))
    private val cancelledTab = Tab(getTranslation("booking.status.cancelled"))

    init {
        addClassName("booking-list-view")
        setSizeFull()

        tabs = Tabs(allTab, pendingTab, confirmedTab, completedTab, cancelledTab)
        tabs.addSelectedChangeListener { updateBookingList() }

        configureGrid()

        val title = if (SecurityUtils.hasRole(UserRole.PROVIDER)) {
            getTranslation("booking.customer")
        } else {
            getTranslation("booking.list")
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
            .setHeader(getTranslation("booking.service"))
            .setSortable(true)
            .setAutoWidth(true)

        // Different columns based on role
        if (SecurityUtils.hasRole(UserRole.PROVIDER)) {
            grid.addColumn { booking -> booking.user.fullName() }
                .setHeader(getTranslation("booking.customer"))
                .setSortable(true)
                .setAutoWidth(true)

            grid.addColumn { booking -> booking.user.email }
                .setHeader(getTranslation("booking.customer.email"))
                .setAutoWidth(true)
        } else {
            grid.addColumn { booking -> booking.service.provider!!.fullName() }
                .setHeader(getTranslation("booking.provider"))
                .setSortable(true)
                .setAutoWidth(true)
        }

        grid.addColumn { booking -> booking.startTime.format(formatter) }
            .setHeader(getTranslation("booking.start.time"))
            .setSortable(true)
            .setAutoWidth(true)

        grid.addColumn { booking -> booking.endTime.format(formatter) }
            .setHeader(getTranslation("booking.end.time"))
            .setAutoWidth(true)

        grid.addComponentColumn { booking ->
            val statusSpan = Span(getTranslation("booking.status.${booking.status.name.lowercase()}"))

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

        grid.addComponentColumn { booking -> createActionButtons(booking) }
            .setHeader(getTranslation("common.actions"))
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
        viewButton.element.setAttribute("title", getTranslation("booking.action.details"))

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
                    confirmButton.element.setAttribute("title", getTranslation("common.confirm"))

                    // Reject button
                    val rejectButton = Button(Icon(VaadinIcon.CLOSE_SMALL)) {
                        cancelBooking(booking)
                    }
                    rejectButton.addThemeVariants(
                        ButtonVariant.LUMO_SMALL,
                        ButtonVariant.LUMO_ERROR,
                        ButtonVariant.LUMO_TERTIARY
                    )
                    rejectButton.element.setAttribute("title", getTranslation("booking.cancel"))

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
                    completeButton.element.setAttribute("title", getTranslation("booking.complete"))

                    // Cancel button
                    val cancelButton = Button(Icon(VaadinIcon.CLOSE_CIRCLE)) {
                        cancelBooking(booking)
                    }
                    cancelButton.addThemeVariants(
                        ButtonVariant.LUMO_SMALL,
                        ButtonVariant.LUMO_ERROR,
                        ButtonVariant.LUMO_TERTIARY
                    )
                    cancelButton.element.setAttribute("title", getTranslation("booking.cancel"))

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
                cancelButton.element.setAttribute("title", getTranslation("booking.cancel"))

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
        dialog.headerTitle = getTranslation("booking.details")

        val content = VerticalLayout()
        content.isPadding = true
        content.setSpacing(true)
        content.width = "400px"

        content.add(
            createDetailItem(getTranslation("booking.service") + ":", booking.service.name),
            createDetailItem(getTranslation("booking.provider") + ":", booking.service.provider!!.fullName()),
            createDetailItem(getTranslation("booking.customer") + ":", booking.user.fullName()),
            createDetailItem(getTranslation("booking.start.time") + ":", booking.startTime.format(formatter)),
            createDetailItem(getTranslation("booking.end.time") + ":", booking.endTime.format(formatter)),
            createDetailItem(
                getTranslation("booking.status") + ":",
                getTranslation("booking.status.${booking.status.name.lowercase()}")
            ),
            createDetailItem(getTranslation("notes.plural") + ":", booking.notes ?: getTranslation("notes.plural"))
        )

        // Add text area for adding/editing notes if the booking is not cancelled or completed
        if (booking.status == BookingStatus.PENDING || booking.status == BookingStatus.CONFIRMED) {
            val notesField = TextArea(getTranslation("notes.plural"))
            notesField.value = booking.notes ?: ""
            notesField.width = "100%"

            val saveButton = Button(getTranslation("notes.save")) {
                try {
                    bookingService.updateBookingNotes(booking.id, notesField.value)
                    Notification.show(getTranslation("notes.updated")).apply {
                        position = Notification.Position.MIDDLE
                        addThemeVariants(NotificationVariant.LUMO_SUCCESS)
                    }
                    dialog.close()
                    updateBookingList()
                } catch (e: Exception) {
                    Notification.show(getTranslation("notes.update.fail", e.message)).apply {
                        position = Notification.Position.MIDDLE
                        addThemeVariants(NotificationVariant.LUMO_ERROR)
                    }
                    logger.warn(e, { "Error when updating booking notes" })
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
            Notification.show(
                getTranslation(
                    "booking.status.updated.to", getTranslation("booking.status.confirmed")
                )
            ).apply {
                position = Notification.Position.MIDDLE
                addThemeVariants(NotificationVariant.LUMO_SUCCESS)
            }
            updateBookingList()
        } catch (e: Exception) {
            Notification.show(getTranslation("booking.status.update.failed", e.message)).apply {
                position = Notification.Position.MIDDLE
                addThemeVariants(NotificationVariant.LUMO_ERROR)
            }
            logger.warn(e, { "Error when completing boooking" })
        }
    }

    private fun completeBooking(booking: Booking) {
        try {
            bookingService.updateBookingStatus(booking.id, BookingStatus.COMPLETED)
            Notification.show(
                getTranslation(
                    "booking.status.updated.to", getTranslation("booking.status.completed")
                )
            ).apply {
                position = Notification.Position.MIDDLE
                addThemeVariants(NotificationVariant.LUMO_SUCCESS)
            }
            updateBookingList()
        } catch (e: Exception) {
            Notification.show(getTranslation("booking.status.update.failed", e.message)).apply {
                position = Notification.Position.MIDDLE
                addThemeVariants(NotificationVariant.LUMO_ERROR)
            }
            logger.warn(e, { "Error when completing booking" })
        }
    }

    private fun cancelBooking(booking: Booking) {
        val dialog = Dialog()
        dialog.headerTitle = getTranslation("booking.cancel")

        val content = VerticalLayout()
        content.isPadding = true
        content.isSpacing = true

        content.add(
            Span(getTranslation("booking.cancel") + "?"),
            createDetailItem(getTranslation("booking.service") + ":", booking.service.name),
            createDetailItem(getTranslation("booking.time") + ":", booking.startTime.format(formatter))
        )

        val buttonLayout = HorizontalLayout()
        buttonLayout.justifyContentMode = FlexComponent.JustifyContentMode.END

        val cancelButton = Button(getTranslation("common.no.keep.it")) {
            dialog.close()
        }

        val confirmButton = Button(getTranslation("common.yes.cancel")) {
            try {
                bookingService.updateBookingStatus(booking.id, BookingStatus.CANCELLED)
                Notification.show(
                    getTranslation(
                        "booking.status.updated.to", getTranslation("booking.status.cancelled")
                    )
                ).apply {
                    position = Notification.Position.MIDDLE
                    addThemeVariants(NotificationVariant.LUMO_SUCCESS)
                }
                dialog.close()
                updateBookingList()
            } catch (e: Exception) {
                Notification.show(getTranslation("booking.status.update.failed", e.message)).apply {
                    position = Notification.Position.MIDDLE
                    addThemeVariants(NotificationVariant.LUMO_ERROR)
                }
                logger.warn(e, { "Error when cancelling booking" })
            }
        }
        confirmButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR)

        buttonLayout.add(cancelButton, confirmButton)
        content.add(buttonLayout)

        dialog.add(content)
        dialog.open()
    }

    override fun getPageTitle() = "micro-booking :: ${getTranslation("booking.list")}"
}