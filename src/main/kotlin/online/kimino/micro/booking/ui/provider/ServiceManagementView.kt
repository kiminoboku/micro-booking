package online.kimino.micro.booking.ui.provider

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
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed
import online.kimino.micro.booking.entity.Service
import online.kimino.micro.booking.entity.UserRole
import online.kimino.micro.booking.security.SecurityUtils
import online.kimino.micro.booking.service.ServiceService
import online.kimino.micro.booking.service.UserService
import online.kimino.micro.booking.ui.MainLayout
import java.time.format.DateTimeFormatter

@Route(value = "services", layout = MainLayout::class)
@PageTitle("Service Management | Booking SaaS")
@RolesAllowed("PROVIDER")
class ServiceManagementView(
    private val serviceService: ServiceService,
    private val userService: UserService
) : VerticalLayout() {

    private val grid = Grid<Service>()
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    init {
        addClassName("service-management-view")
        setSizeFull()

        val toolbar = createToolbar()
        configureGrid()

        add(
            toolbar,
            grid
        )

        updateServiceList()
    }

    private fun configureGrid() {
        grid.addClassName("services-grid")
        grid.setSizeFull()

        grid.addColumn { service -> service.name }
            .setHeader("Service Name")
            .setSortable(true)
            .setAutoWidth(true)

        grid.addColumn { service -> service.duration }
            .setHeader("Duration (min)")
            .setSortable(true)
            .setAutoWidth(true)

        grid.addColumn { service -> "$${service.price}" }
            .setHeader("Price")
            .setSortable(true)
            .setAutoWidth(true)

        grid.addComponentColumn { service ->
            if (service.active) {
                Span("Active").apply {
                    element.style.set("color", "var(--lumo-success-color)")
                }
            } else {
                Span("Inactive").apply {
                    element.style.set("color", "var(--lumo-error-color)")
                }
            }
        }
            .setHeader("Status")
            .setAutoWidth(true)

        grid.addColumn { service -> service.createdAt.format(formatter) }
            .setHeader("Created")
            .setSortable(true)
            .setAutoWidth(true)

        grid.addComponentColumn { service -> createActionButtons(service) }
            .setHeader("Actions")
            .setAutoWidth(true)

        grid.getColumns().forEach { it.setResizable(true) }
    }

    private fun createToolbar(): HorizontalLayout {
        val toolbar = HorizontalLayout()
        toolbar.setWidthFull()
        toolbar.justifyContentMode = FlexComponent.JustifyContentMode.BETWEEN
        toolbar.defaultVerticalComponentAlignment = FlexComponent.Alignment.CENTER

        val title = H2("My Services")

        val addButton = Button("Add Service", Icon(VaadinIcon.PLUS)) {
            showServiceForm(null)
        }
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY)

        toolbar.add(title, addButton)
        return toolbar
    }

    private fun createActionButtons(service: Service): HorizontalLayout {
        val buttonLayout = HorizontalLayout()
        buttonLayout.isPadding = false
        buttonLayout.isSpacing = true

        // View availabilities button
        val availabilityButton = Button(Icon(VaadinIcon.CALENDAR)) {
            navigateToAvailabilities(service)
        }
        availabilityButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY)
        availabilityButton.element.setAttribute("title", "Manage Availabilities")

        // Edit button
        val editButton = Button(Icon(VaadinIcon.EDIT)) {
            showServiceForm(service)
        }
        editButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY)
        editButton.element.setAttribute("title", "Edit Service")

        // Toggle status button
        val toggleButton = if (service.active) {
            Button(Icon(VaadinIcon.BAN)) {
                toggleServiceStatus(service)
            }.apply {
                element.setAttribute("title", "Deactivate Service")
                addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR)
            }
        } else {
            Button(Icon(VaadinIcon.CHECK)) {
                toggleServiceStatus(service)
            }.apply {
                element.setAttribute("title", "Activate Service")
                addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SUCCESS)
            }
        }

        buttonLayout.add(availabilityButton, editButton, toggleButton)
        return buttonLayout
    }

    private fun showServiceForm(service: Service?) {
        val dialog = Dialog()
        dialog.headerTitle = if (service == null) "Add New Service" else "Edit Service"

        val serviceForm = ServiceForm(service)

        serviceForm.addSaveListener { event ->
            saveService(event.service)
            dialog.close()
        }

        serviceForm.addCancelListener {
            dialog.close()
        }

        dialog.add(serviceForm)
        dialog.open()
    }

    private fun saveService(service: Service) {
        try {
            val currentUser = SecurityUtils.getCurrentUsername()?.let {
                userService.findByEmail(it).orElseThrow { IllegalStateException("User not found") }
            } ?: throw IllegalStateException("User not logged in")

            if (service.id == 0L) {
                // New service
                service.provider = currentUser
            }

            serviceService.createService(service)

            Notification.show("Service saved successfully").apply {
                position = Notification.Position.MIDDLE
                addThemeVariants(NotificationVariant.LUMO_SUCCESS)
            }

            updateServiceList()
        } catch (e: Exception) {
            Notification.show("Failed to save service: ${e.message}").apply {
                position = Notification.Position.MIDDLE
                addThemeVariants(NotificationVariant.LUMO_ERROR)
            }
        }
    }

    private fun toggleServiceStatus(service: Service) {
        try {
            serviceService.toggleServiceStatus(service.id)

            val statusText = if (service.active) "deactivated" else "activated"
            Notification.show("Service $statusText successfully").apply {
                position = Notification.Position.MIDDLE
                addThemeVariants(NotificationVariant.LUMO_SUCCESS)
            }

            updateServiceList()
        } catch (e: Exception) {
            Notification.show("Failed to update service status: ${e.message}").apply {
                position = Notification.Position.MIDDLE
                addThemeVariants(NotificationVariant.LUMO_ERROR)
            }
        }
    }

    private fun navigateToAvailabilities(service: Service) {
        ui.ifPresent { ui ->
            ui.navigate(AvailabilityManagementView::class.java, service.id)
        }
    }

    private fun updateServiceList() {
        val currentUser = SecurityUtils.getCurrentUsername()?.let {
            userService.findByEmail(it).orElse(null)
        }

        if (currentUser == null || !SecurityUtils.hasRole(UserRole.PROVIDER)) {
            grid.setItems(emptyList())
            return
        }

        val services = serviceService.findAllByProvider(currentUser.id)
        grid.setItems(services)
    }
}