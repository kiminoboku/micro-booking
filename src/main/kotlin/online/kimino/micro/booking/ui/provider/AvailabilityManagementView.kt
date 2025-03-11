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
import com.vaadin.flow.router.HasDynamicTitle
import com.vaadin.flow.router.HasUrlParameter
import com.vaadin.flow.router.Route
import io.github.oshai.kotlinlogging.KotlinLogging
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
@RolesAllowed("PROVIDER")
class AvailabilityManagementView(
    private val availabilityService: AvailabilityService,
    private val serviceService: ServiceService
) : VerticalLayout(), HasUrlParameter<Long>, HasDynamicTitle {

    private val logger = KotlinLogging.logger { }
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
                IllegalArgumentException(getTranslation("service.not.found", serviceId))
            }

            removeAll()

            add(
                createToolbar(),
                createServiceInfo(),
                grid
            )

            updateAvailabilityList()
        } catch (e: Exception) {
            Notification.show(getTranslation("notification.error", e.message)).apply {
                position = Notification.Position.MIDDLE
                addThemeVariants(NotificationVariant.LUMO_ERROR)
            }
            logger.warn(e, { e.message })

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
            .setHeader(getTranslation("availability.day.of.week"))
            .setSortable(true)
            .setAutoWidth(true)

        grid.addColumn { availability -> availability.startTime.format(timeFormatter) }
            .setHeader(getTranslation("availability.start.time"))
            .setSortable(true)
            .setAutoWidth(true)

        grid.addColumn { availability -> availability.endTime.format(timeFormatter) }
            .setHeader(getTranslation("availability.end.time"))
            .setSortable(true)
            .setAutoWidth(true)

        grid.addComponentColumn { availability -> createActionButtons(availability) }
            .setHeader(getTranslation("common.actions"))
            .setAutoWidth(true)

        grid.getColumns().forEach { it.setResizable(true) }
    }

    private fun createToolbar(): HorizontalLayout {
        val toolbar = HorizontalLayout()
        toolbar.setWidthFull()
        toolbar.justifyContentMode = FlexComponent.JustifyContentMode.BETWEEN
        toolbar.defaultVerticalComponentAlignment = FlexComponent.Alignment.CENTER

        val title = H2(getTranslation("availability.manage"))

        val backButton = Button(getTranslation("availability.back.to.services"), Icon(VaadinIcon.ARROW_LEFT)) {
            ui.ifPresent { ui ->
                ui.navigate(ServiceManagementView::class.java)
            }
        }

        val addButton = Button(getTranslation("availability.add"), Icon(VaadinIcon.PLUS)) {
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
        val statusText = if (service.active) getTranslation("service.active") else getTranslation("service.inactive")
        val statusSpan = Span("${getTranslation("availability.status")}: $statusText")
        statusSpan.element.style.set(
            "color",
            if (service.active) "var(--lumo-success-color)" else "var(--lumo-error-color)"
        )

        val durationSpan =
            Span("${getTranslation("availability.duration")}: ${getTranslation("common.minutes", service.duration)}")

        infoLayout.add(nameHeading, statusSpan, durationSpan)

        if (!service.active) {
            val warningLayout = HorizontalLayout()
            warningLayout.defaultVerticalComponentAlignment = FlexComponent.Alignment.CENTER

            val warningIcon = Icon(VaadinIcon.EXCLAMATION_CIRCLE)
            warningIcon.color = "var(--lumo-error-color)"

            val warningText = Span(getTranslation("availability.inactive.warning"))
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
        editButton.element.setAttribute("title", getTranslation("common.edit"))

        // Delete button
        val deleteButton = Button(Icon(VaadinIcon.TRASH)) {
            deleteAvailability(availability)
        }
        deleteButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR)
        deleteButton.element.setAttribute("title", getTranslation("common.delete"))

        buttonLayout.add(editButton, deleteButton)
        return buttonLayout
    }

    private fun showAvailabilityForm(availability: Availability?) {
        val dialog = Dialog()
        dialog.headerTitle = if (availability == null)
            getTranslation("availability.add.new")
        else
            getTranslation("availability.edit")

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
                availability.service =
                    currentService ?: throw IllegalStateException(getTranslation("availability.error.service.null"))
            }

            // Validate that end time is after start time
            if (!availability.isValid()) {
                throw IllegalArgumentException(getTranslation("validation.availability.end.after.start"))
            }

            availabilityService.createAvailability(availability)

            Notification.show(getTranslation("availability.saved")).apply {
                position = Notification.Position.MIDDLE
                addThemeVariants(NotificationVariant.LUMO_SUCCESS)
            }

            updateAvailabilityList()
        } catch (e: Exception) {
            Notification.show(getTranslation("availability.save.failed", e.message ?: "")).apply {
                position = Notification.Position.MIDDLE
                addThemeVariants(NotificationVariant.LUMO_ERROR)
            }
            logger.warn(e, { "Error when saving availability" })
        }
    }

    private fun deleteAvailability(availability: Availability) {
        val dialog = Dialog()
        dialog.headerTitle = getTranslation("availability.delete")

        val content = VerticalLayout()
        content.isPadding = true
        content.isSpacing = true

        val day = availability.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
        val time = "${availability.startTime.format(timeFormatter)} - ${availability.endTime.format(timeFormatter)}"

        content.add(
            Span(getTranslation("availability.delete.confirm")),
            Span("$day, $time")
        )

        val buttonLayout = HorizontalLayout()
        buttonLayout.justifyContentMode = FlexComponent.JustifyContentMode.END

        val cancelButton = Button(getTranslation("common.cancel")) {
            dialog.close()
        }

        val confirmButton = Button(getTranslation("common.delete")) {
            try {
                availabilityService.deleteAvailability(availability.id)

                Notification.show(getTranslation("availability.deleted")).apply {
                    position = Notification.Position.MIDDLE
                    addThemeVariants(NotificationVariant.LUMO_SUCCESS)
                }

                dialog.close()
                updateAvailabilityList()
            } catch (e: Exception) {
                Notification.show(getTranslation("availability.delete.failed", e.message ?: "")).apply {
                    position = Notification.Position.MIDDLE
                    addThemeVariants(NotificationVariant.LUMO_ERROR)
                }
                logger.warn(e, { "Error when deleting availability" })
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

    override fun getPageTitle() = "micro-booking :: ${getTranslation("provider.availabilities")}"
}