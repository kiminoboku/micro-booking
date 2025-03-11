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
import com.vaadin.flow.router.HasDynamicTitle
import com.vaadin.flow.router.Route
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.security.RolesAllowed
import online.kimino.micro.booking.entity.Service
import online.kimino.micro.booking.entity.UserRole
import online.kimino.micro.booking.security.SecurityUtils
import online.kimino.micro.booking.service.ServiceService
import online.kimino.micro.booking.service.UserService
import online.kimino.micro.booking.ui.MainLayout
import java.time.format.DateTimeFormatter

@Route(value = "services", layout = MainLayout::class)
@RolesAllowed("PROVIDER")
class ServiceManagementView(
    private val serviceService: ServiceService,
    private val userService: UserService
) : VerticalLayout(), HasDynamicTitle {

    private val logger = KotlinLogging.logger { }
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
            .setHeader(getTranslation("service.name"))
            .setSortable(true)
            .setAutoWidth(true)

        grid.addColumn { service -> service.duration }
            .setHeader(getTranslation("service.duration"))
            .setSortable(true)
            .setAutoWidth(true)

        grid.addColumn { service -> "$${service.price}" }
            .setHeader(getTranslation("service.price"))
            .setSortable(true)
            .setAutoWidth(true)

        grid.addComponentColumn { service ->
            if (service.active) {
                Span(getTranslation("service.active")).apply {
                    element.style.set("color", "var(--lumo-success-color)")
                }
            } else {
                Span(getTranslation("service.inactive")).apply {
                    element.style.set("color", "var(--lumo-error-color)")
                }
            }
        }
            .setHeader(getTranslation("booking.status"))
            .setAutoWidth(true)

        grid.addColumn { service -> service.createdAt.format(formatter) }
            .setHeader(getTranslation("common.created.at"))
            .setSortable(true)
            .setAutoWidth(true)

        grid.addComponentColumn { service -> createActionButtons(service) }
            .setHeader(getTranslation("common.actions"))
            .setAutoWidth(true)

        grid.getColumns().forEach { it.setResizable(true) }
    }

    private fun createToolbar(): HorizontalLayout {
        val toolbar = HorizontalLayout()
        toolbar.setWidthFull()
        toolbar.justifyContentMode = FlexComponent.JustifyContentMode.BETWEEN
        toolbar.defaultVerticalComponentAlignment = FlexComponent.Alignment.CENTER

        val title = H2(getTranslation("provider.services"))

        // Create button layout for multiple buttons
        val buttonLayout = HorizontalLayout()
        buttonLayout.isSpacing = true

        val exceptionButton = Button(getTranslation("provider.exception.periods"), Icon(VaadinIcon.EXCLAMATION)) {
            ui.ifPresent { ui -> ui.navigate(ExceptionPeriodManagementView::class.java) }
        }

        val addButton = Button(getTranslation("service.add"), Icon(VaadinIcon.PLUS)) {
            showServiceForm(null)
        }
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY)

        buttonLayout.add(exceptionButton, addButton)
        toolbar.add(title, buttonLayout)
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
        availabilityButton.element.setAttribute("title", getTranslation("provider.availabilities"))

        // Edit button
        val editButton = Button(Icon(VaadinIcon.EDIT)) {
            showServiceForm(service)
        }
        editButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY)
        editButton.element.setAttribute("title", getTranslation("service.edit"))

        // Toggle status button
        val toggleButton = if (service.active) {
            Button(Icon(VaadinIcon.BAN)) {
                toggleServiceStatus(service)
            }.apply {
                element.setAttribute("title", getTranslation("service.inactive"))
                addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR)
            }
        } else {
            Button(Icon(VaadinIcon.CHECK)) {
                toggleServiceStatus(service)
            }.apply {
                element.setAttribute("title", getTranslation("service.active"))
                addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SUCCESS)
            }
        }

        buttonLayout.add(availabilityButton, editButton, toggleButton)
        return buttonLayout
    }

    private fun showServiceForm(service: Service?) {
        val dialog = Dialog()
        dialog.headerTitle = if (service == null)
            getTranslation("service.add")
        else
            getTranslation("service.edit")

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
                userService.findByEmail(it).orElseThrow { IllegalStateException(getTranslation("user.not.found")) }
            } ?: throw IllegalStateException(getTranslation("user.not.logged.in"))

            if (service.id == 0L) {
                // New service
                service.provider = currentUser
            }

            serviceService.createService(service)

            Notification.show(getTranslation("notification.saved")).apply {
                position = Notification.Position.MIDDLE
                addThemeVariants(NotificationVariant.LUMO_SUCCESS)
            }

            updateServiceList()
        } catch (e: Exception) {
            Notification.show("${getTranslation("notification.failed")}: ${e.message}").apply {
                position = Notification.Position.MIDDLE
                addThemeVariants(NotificationVariant.LUMO_ERROR)
            }
            logger.warn(e, { "Error when trying to create service" })
        }
    }

    private fun toggleServiceStatus(service: Service) {
        try {
            serviceService.toggleServiceStatus(service.id)

            Notification.show(getTranslation("notification.updated")).apply {
                position = Notification.Position.MIDDLE
                addThemeVariants(NotificationVariant.LUMO_SUCCESS)
            }

            updateServiceList()
        } catch (e: Exception) {
            Notification.show("${getTranslation("notification.failed")}: ${e.message}").apply {
                position = Notification.Position.MIDDLE
                addThemeVariants(NotificationVariant.LUMO_ERROR)
            }
            logger.warn(e, { "Error when trying to toggle service" })
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

    override fun getPageTitle() = "micro-booking :: ${getTranslation("provider.services")}"
}