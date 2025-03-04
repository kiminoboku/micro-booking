package online.kimino.micro.booking.ui.provider

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
import com.vaadin.flow.router.BeforeEvent
import com.vaadin.flow.router.HasUrlParameter
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed
import online.kimino.micro.booking.entity.Availability
import online.kimino.micro.booking.entity.Service
import online.kimino.micro.booking.service.AvailabilityService
import online.kimino.micro.booking.service.ServiceService
import online.kimino.micro.booking.ui.MainLayout
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

@Route(value = "availabilities", layout = MainLayout::class)
@PageTitle("Manage Availabilities | Booking SaaS")
@RolesAllowed("PROVIDER")
class AvailabilityManagementView(
    private val availabilityService: AvailabilityService,
    private val serviceService: ServiceService
) : VerticalLayout(), HasUrlParameter<Long> {

    private val grid = Grid<Availability>()
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    private var currentService: Service? = null

    init {
        addClassName("availability-management-view")
        setSizeFull()
        setPadding(true)

        configureGrid()
    }

    override fun setParameter(event: BeforeEvent, serviceId: Long) {
        try {
            currentService = serviceService.findById(serviceId).orElseThrow {
                IllegalArgumentException("Service not found with id $serviceId")
            }

            removeAll()

            add(
                createToolbar(),
                createServiceInfo(),
                grid
            )

            updateAvailabilityList()
        } catch (e: Exception) {
            Notification.show("Error: ${e.message}").apply {
                position = Notification.Position.MIDDLE
                addThemeVariants(NotificationVariant.LUMO_ERROR)
            }

            ui.ifPresent { ui ->
                ui.navigate(ServiceManagementView::class.java)
            }
        }
    }

    private fun configureGrid() {
        grid.addClassName("availabilities-grid")
        grid.setSizeFull()

        grid.addColumn { availability ->
            availability.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
        }
            .setHeader("Day")
            .setSortable(true)
            .setAutoWidth(true)

        grid.addColumn { availability -> availability.startTime.format(timeFormatter) }
            .setHeader("Start Time")
            .setSortable(true)
            .setAutoWidth(true)

        grid.addColumn { availability -> availability.endTime.format(timeFormatter) }
            .setHeader("End Time")
            .setSortable(true)
            .setAutoWidth(true)

        grid.addComponentColumn { availability -> createActionButtons(availability) }
            .setHeader("Actions")
            .setAutoWidth(true)

        grid.getColumns().forEach { it.setResizable(true) }
    }

    private fun createToolbar(): HorizontalLayout {
        val toolbar = HorizontalLayout()
        toolbar.setWidthFull()
        toolbar.justifyContentMode = FlexComponent.JustifyContentMode.BETWEEN
        toolbar.defaultVerticalComponentAlignment = FlexComponent.Alignment.CENTER

        val title = H2("Manage Availabilities")

        val backButton = Button("Back to Services", Icon(VaadinIcon.ARROW_LEFT)) {
            ui.ifPresent { ui ->
                ui.navigate(ServiceManagementView::class.java)
            }
        }

        val addButton = Button("Add Availability", Icon(VaadinIcon.PLUS)) {
            showAvailabilityForm(null)
        }
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY)

        val buttonLayout = HorizontalLayout(backButton, addButton)
        buttonLayout.isSpacing = true

        toolbar.add(title, buttonLayout)
        return toolbar
    }

    private fun createServiceInfo(): VerticalLayout {
        val infoLayout = VerticalLayout()
        infoLayout.setSpacing(false)
        infoLayout.setPadding(false)

        val service = currentService ?: return infoLayout

        val nameHeading = H3(service.name)
        val statusText = if (service.active) "Active" else "Inactive"
        val statusSpan = Span("Status: $statusText")
        statusSpan.element.style.set(
            "color",
            if (service.active) "var(--lumo-success-color)" else "var(--lumo-error-color)"
        )

        val durationSpan = Span("Duration: ${service.duration} minutes")

        infoLayout.add(nameHeading, statusSpan, durationSpan)

        if (!service.active) {
            val warningLayout = HorizontalLayout()
            warningLayout.defaultVerticalComponentAlignment = FlexComponent.Alignment.CENTER

            val warningIcon = Icon(VaadinIcon.EXCLAMATION_CIRCLE)
            warningIcon.color = "var(--lumo-error-color)"

            val warningText = Span("Note: This service is currently inactive and won't be visible to customers")
            warningText.element.style.set("color", "var(--lumo-error-color)")

            warningLayout.add(warningIcon, warningText)
            infoLayout.add(warningLayout)
        }

        return infoLayout
    }

    private fun createActionButtons(availability: Availability): HorizontalLayout {
        val buttonLayout = HorizontalLayout()
        buttonLayout.isPadding = false
        buttonLayout.isSpacing = true

        // Edit button
        val editButton = Button(Icon(VaadinIcon.EDIT)) {
            showAvailabilityForm(availability)
        }
        editButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY)
        editButton.element.setAttribute("title", "Edit")

        // Delete button
        val deleteButton = Button(Icon(VaadinIcon.TRASH)) {
            deleteAvailability(availability)
        }
        deleteButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR)
        deleteButton.element.setAttribute("title", "Delete")

        buttonLayout.add(editButton, deleteButton)
        return buttonLayout
    }

    private fun showAvailabilityForm(availability: Availability?) {
        val dialog = Dialog()
        dialog.headerTitle = if (availability == null) "Add New Availability" else "Edit Availability"

        val service = currentService ?: return

        val availabilityForm = AvailabilityForm(availability, service)

        availabilityForm.addSaveListener { event ->
            saveAvailability(event.availability)
            dialog.close()
        }

        availabilityForm.addCancelListener {
            dialog.close()
        }

        dialog.add(availabilityForm)
        dialog.open()
    }

    private fun saveAvailability(availability: Availability) {
        try {
            if (availability.id == 0L) {
                // New availability
                availability.service = currentService ?: throw IllegalStateException("Current service is null")
            }

            // Validate that end time is after start time
            if (!availability.isValid()) {
                throw IllegalArgumentException("End time must be after start time")
            }

            availabilityService.createAvailability(availability)

            Notification.show("Availability saved successfully").apply {
                position = Notification.Position.MIDDLE
                addThemeVariants(NotificationVariant.LUMO_SUCCESS)
            }

            updateAvailabilityList()
        } catch (e: Exception) {
            Notification.show("Failed to save availability: ${e.message}").apply {
                position = Notification.Position.MIDDLE
                addThemeVariants(NotificationVariant.LUMO_ERROR)
            }
        }
    }

    private fun deleteAvailability(availability: Availability) {
        val dialog = Dialog()
        dialog.headerTitle = "Delete Availability"

        val content = VerticalLayout()
        content.isPadding = true
        content.setSpacing(true)

        val day = availability.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
        val time = "${availability.startTime.format(timeFormatter)} - ${availability.endTime.format(timeFormatter)}"

        content.add(
            Span("Are you sure you want to delete this availability?"),
            Span("$day, $time")
        )

        val buttonLayout = HorizontalLayout()
        buttonLayout.justifyContentMode = FlexComponent.JustifyContentMode.END

        val cancelButton = Button("Cancel") {
            dialog.close()
        }

        val confirmButton = Button("Delete") {
            try {
                availabilityService.deleteAvailability(availability.id)

                Notification.show("Availability deleted").apply {
                    position = Notification.Position.MIDDLE
                    addThemeVariants(NotificationVariant.LUMO_SUCCESS)
                }

                dialog.close()
                updateAvailabilityList()
            } catch (e: Exception) {
                Notification.show("Failed to delete availability: ${e.message}").apply {
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

    private fun updateAvailabilityList() {
        val service = currentService

        if (service == null) {
            grid.setItems(emptyList())
            return
        }

        val availabilities = availabilityService.findAllByServiceId(service.id)
        grid.setItems(availabilities)
    }
}