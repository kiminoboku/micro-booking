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
import jakarta.annotation.security.PermitAll
import online.kimino.micro.booking.entity.CyclicBooking
import online.kimino.micro.booking.entity.CyclicBookingStatus
import online.kimino.micro.booking.entity.RecurrencePattern
import online.kimino.micro.booking.entity.UserRole
import online.kimino.micro.booking.security.SecurityUtils
import online.kimino.micro.booking.service.CyclicBookingService
import online.kimino.micro.booking.service.UserService
import online.kimino.micro.booking.ui.MainLayout
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

@Route(value = "cyclic-bookings", layout = MainLayout::class)
@PermitAll
class CyclicBookingListView(
    private val cyclicBookingService: CyclicBookingService,
    private val userService: UserService
) : VerticalLayout(), HasDynamicTitle {

    private val grid = Grid<CyclicBooking>()
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    private val tabs: Tabs
    private val allTab = Tab(getTranslation("cyclic.booking.list"))
    private val pendingTab = Tab(getTranslation("booking.status.pending"))
    private val confirmedTab = Tab(getTranslation("booking.status.confirmed"))
    private val cancelledTab = Tab(getTranslation("booking.status.cancelled"))

    init {
        addClassName("cyclic-booking-list-view")
        setSizeFull()

        tabs = Tabs(allTab, pendingTab, confirmedTab, cancelledTab)
        tabs.addSelectedChangeListener { updateBookingList() }

        configureGrid()

        val title = getTranslation("cyclic.booking.list")

        val header = HorizontalLayout()
        header.setWidthFull()
        header.justifyContentMode = FlexComponent.JustifyContentMode.BETWEEN
        header.add(H2(title))

        // Add create button if user is not a provider
        if (!SecurityUtils.hasRole(UserRole.PROVIDER)) {
            val createButton = Button(
                getTranslation("cyclic.booking.create"),
                Icon(VaadinIcon.PLUS)
            )
            createButton.addClickListener {
                ui.ifPresent { ui -> ui.navigate(CreateCyclicBookingView::class.java) }
            }
            createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY)
            header.add(createButton)
        }

        add(
            header,
            tabs,
            grid
        )

        updateBookingList()
    }

    private fun configureGrid() {
        grid.addClassName("cyclic-bookings-grid")
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

        grid.addColumn { booking ->
            when (booking.recurrencePattern) {
                RecurrencePattern.WEEKLY -> {
                    val dayName = booking.dayOfWeek?.getDisplayName(TextStyle.FULL, Locale.getDefault())
                    getTranslation("cyclic.booking.weekly.label", dayName ?: "")
                }

                RecurrencePattern.MONTHLY -> getTranslation(
                    "cyclic.booking.monthly.label",
                    booking.dayOfMonth?.toString() ?: ""
                )
            }
        }
            .setHeader(getTranslation("cyclic.booking.recurrence"))
            .setAutoWidth(true)

        grid.addColumn { booking ->
            val startDateStr = booking.startDate.format(formatter)
            val endDateStr = booking.endDate?.format(formatter) ?: getTranslation("cyclic.booking.no.end.date")
            "$startDateStr - $endDateStr"
        }
            .setHeader(getTranslation("cyclic.booking.date.range"))
            .setSortable(true)
            .setAutoWidth(true)

        grid.addColumn { booking -> "${booking.startTime.format(timeFormatter)} - ${booking.endTime.format(timeFormatter)}" }
            .setHeader(getTranslation("booking.time"))
            .setAutoWidth(true)

        grid.addComponentColumn { booking ->
            val statusSpan = Span(getTranslation("booking.status.${booking.status.name.lowercase()}"))

            // Style based on status
            when (booking.status) {
                CyclicBookingStatus.PENDING -> statusSpan.element.style.set("color", "var(--lumo-primary-color)")
                CyclicBookingStatus.CONFIRMED -> statusSpan.element.style.set("color", "var(--lumo-success-color)")
                CyclicBookingStatus.CANCELLED -> statusSpan.element.style.set("color", "var(--lumo-error-color)")
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

    private fun createActionButtons(booking: CyclicBooking): HorizontalLayout {
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
                CyclicBookingStatus.PENDING -> {
                    // Confirm button
                    val confirmButton = Button(Icon(VaadinIcon.CHECK)) {
                        confirmCyclicBooking(booking)
                    }
                    confirmButton.addThemeVariants(
                        ButtonVariant.LUMO_SMALL,
                        ButtonVariant.LUMO_SUCCESS,
                        ButtonVariant.LUMO_TERTIARY
                    )
                    confirmButton.element.setAttribute("title", getTranslation("common.confirm"))

                    // Reject button
                    val rejectButton = Button(Icon(VaadinIcon.CLOSE_SMALL)) {
                        cancelCyclicBooking(booking)
                    }
                    rejectButton.addThemeVariants(
                        ButtonVariant.LUMO_SMALL,
                        ButtonVariant.LUMO_ERROR,
                        ButtonVariant.LUMO_TERTIARY
                    )
                    rejectButton.element.setAttribute("title", getTranslation("booking.cancel"))

                    buttonLayout.add(confirmButton, rejectButton)
                }

                CyclicBookingStatus.CONFIRMED -> {
                    // Cancel button
                    val cancelButton = Button(Icon(VaadinIcon.CLOSE_CIRCLE)) {
                        cancelCyclicBooking(booking)
                    }
                    cancelButton.addThemeVariants(
                        ButtonVariant.LUMO_SMALL,
                        ButtonVariant.LUMO_ERROR,
                        ButtonVariant.LUMO_TERTIARY
                    )
                    cancelButton.element.setAttribute("title", getTranslation("booking.cancel"))

                    buttonLayout.add(cancelButton)
                }

                else -> {
                    // No additional action buttons for cancelled bookings
                }
            }
        } else {
            // Regular user
            if (booking.status == CyclicBookingStatus.PENDING || booking.status == CyclicBookingStatus.CONFIRMED) {
                val cancelButton = Button(Icon(VaadinIcon.CLOSE_CIRCLE)) {
                    cancelCyclicBooking(booking)
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
                    pendingTab -> cyclicBookingService.findAllByProviderIdAndStatus(
                        currentUser.id,
                        CyclicBookingStatus.PENDING
                    )

                    confirmedTab -> cyclicBookingService.findAllByProviderIdAndStatus(
                        currentUser.id,
                        CyclicBookingStatus.CONFIRMED
                    )

                    cancelledTab -> cyclicBookingService.findAllByProviderIdAndStatus(
                        currentUser.id,
                        CyclicBookingStatus.CANCELLED
                    )

                    else -> cyclicBookingService.findAllByProviderId(currentUser.id)
                }
            }

            else -> {
                when (tabs.selectedTab) {
                    pendingTab -> cyclicBookingService.findAllByUserIdAndStatus(
                        currentUser.id,
                        CyclicBookingStatus.PENDING
                    )

                    confirmedTab -> cyclicBookingService.findAllByUserIdAndStatus(
                        currentUser.id,
                        CyclicBookingStatus.CONFIRMED
                    )

                    cancelledTab -> cyclicBookingService.findAllByUserIdAndStatus(
                        currentUser.id,
                        CyclicBookingStatus.CANCELLED
                    )

                    else -> cyclicBookingService.findAllByUserId(currentUser.id)
                }
            }
        }

        grid.setItems(bookings)
    }

    private fun showBookingDetails(booking: CyclicBooking) {
        val dialog = Dialog()
        dialog.headerTitle = getTranslation("cyclic.booking.details")

        val content = VerticalLayout()
        content.isPadding = true
        content.setSpacing(true)
        content.width = "400px"

        // Format recurrence pattern text
        val recurrenceText = when (booking.recurrencePattern) {
            RecurrencePattern.WEEKLY -> {
                val dayName = booking.dayOfWeek?.getDisplayName(TextStyle.FULL, Locale.getDefault())
                getTranslation("cyclic.booking.weekly.label", dayName ?: "")
            }

            RecurrencePattern.MONTHLY -> getTranslation(
                "cyclic.booking.monthly.label",
                booking.dayOfMonth?.toString() ?: ""
            )
        }

        // Format date range
        val dateRange = if (booking.endDate != null) {
            "${booking.startDate.format(formatter)} - ${booking.endDate!!.format(formatter)}"
        } else {
            "${booking.startDate.format(formatter)} - ${getTranslation("cyclic.booking.no.end.date")}"
        }

        content.add(
            createDetailItem(getTranslation("booking.service") + ":", booking.service.name),
            createDetailItem(getTranslation("booking.provider") + ":", booking.service.provider!!.fullName()),
            createDetailItem(getTranslation("booking.customer") + ":", booking.user.fullName()),
            createDetailItem(getTranslation("cyclic.booking.recurrence") + ":", recurrenceText),
            createDetailItem(getTranslation("cyclic.booking.date.range") + ":", dateRange),
            createDetailItem(
                getTranslation("booking.time") + ":",
                "${booking.startTime.format(timeFormatter)} - ${booking.endTime.format(timeFormatter)}"
            ),
            createDetailItem(
                getTranslation("booking.status") + ":",
                getTranslation("booking.status.${booking.status.name.lowercase()}")
            ),
            createDetailItem(getTranslation("notes.plural") + ":", booking.notes ?: "")
        )

        // Add text area for adding/editing notes if the booking is not cancelled
        if (booking.status != CyclicBookingStatus.CANCELLED) {
            val notesField = TextArea(getTranslation("notes.plural"))
            notesField.value = booking.notes ?: ""
            notesField.width = "100%"

            val saveButton = Button(getTranslation("notes.save")) {
                try {
                    cyclicBookingService.updateCyclicBookingNotes(booking.id, notesField.value)
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
                }
            }

            content.add(notesField, saveButton)
        }

        // View individual bookings link
        if (booking.bookings.isNotEmpty()) {
            val viewBookingsButton = Button(getTranslation("cyclic.booking.view.instances")) {
                ui.ifPresent { ui -> ui.navigate(BookingListView::class.java) }
            }
            content.add(viewBookingsButton)
        }

        val closeButton = Button(getTranslation("common.close")) {
            dialog.close()
        }
        content.add(closeButton)

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

    private fun confirmCyclicBooking(booking: CyclicBooking) {
        try {
            cyclicBookingService.updateCyclicBookingStatus(booking.id, CyclicBookingStatus.CONFIRMED)
            Notification.show(getTranslation("cyclic.booking.status.updated.confirmed")).apply {
                position = Notification.Position.MIDDLE
                addThemeVariants(NotificationVariant.LUMO_SUCCESS)
            }
            updateBookingList()
        } catch (e: Exception) {
            Notification.show("${getTranslation("cyclic.booking.status.update.failed")}: ${e.message}").apply {
                position = Notification.Position.MIDDLE
                addThemeVariants(NotificationVariant.LUMO_ERROR)
            }
        }
    }

    private fun cancelCyclicBooking(booking: CyclicBooking) {
        val dialog = Dialog()
        dialog.headerTitle = getTranslation("cyclic.booking.cancel")

        val content = VerticalLayout()
        content.isPadding = true
        content.isSpacing = true

        content.add(
            Span(getTranslation("cyclic.booking.cancel.confirm")),
            createDetailItem(getTranslation("booking.service") + ":", booking.service.name)
        )

        val buttonLayout = HorizontalLayout()
        buttonLayout.justifyContentMode = FlexComponent.JustifyContentMode.END

        val cancelButton = Button(getTranslation("common.no.keep.it")) {
            dialog.close()
        }

        val confirmButton = Button(getTranslation("common.yes.cancel")) {
            try {
                cyclicBookingService.updateCyclicBookingStatus(booking.id, CyclicBookingStatus.CANCELLED)
                Notification.show(getTranslation("cyclic.booking.status.updated.cancelled")).apply {
                    position = Notification.Position.MIDDLE
                    addThemeVariants(NotificationVariant.LUMO_SUCCESS)
                }
                dialog.close()
                updateBookingList()
            } catch (e: Exception) {
                Notification.show("${getTranslation("cyclic.booking.status.update.failed")}: ${e.message}").apply {
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

    override fun getPageTitle() = "micro-booking :: ${getTranslation("cyclic.booking.list")}"
}