package online.kimino.micro.booking.ui.admin

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
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
import com.vaadin.flow.router.RouterLink
import jakarta.annotation.security.RolesAllowed
import online.kimino.micro.booking.entity.Service
import online.kimino.micro.booking.service.ServiceService
import online.kimino.micro.booking.ui.MainLayout

@Route(value = "admin/services", layout = MainLayout::class)
@PageTitle("Service Management | Booking SaaS")
@RolesAllowed("ADMIN")
class AdminServicesView(
    private val serviceService: ServiceService
) : VerticalLayout() {

    private val grid = Grid<Service>()

    init {
        addClassName("admin-services-view")
        setSizeFull()

        add(
            createHeaderWithNavigation(),
            configureGrid()
        )

        updateServiceList()
    }

    private fun createHeaderWithNavigation(): Component {
        val header = VerticalLayout()
        header.setPadding(false)
        header.setSpacing(true)

        header.add(H2("Service Management"))

        val navLayout = HorizontalLayout()
        navLayout.setWidthFull()
        navLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.START)
        navLayout.add(
            RouterLink("Dashboard", AdminDashboardView::class.java),
            RouterLink("Users", AdminUsersView::class.java),
            RouterLink("Bookings", AdminBookingsView::class.java)
        )

        header.add(navLayout)
        return header
    }

    private fun configureGrid(): Grid<Service> {
        grid.addClassName("services-grid")
        grid.setSizeFull()

        grid.addColumn { service -> service.name }
            .setHeader("Service Name")
            .setSortable(true)
            .setAutoWidth(true)

        grid.addColumn { service -> service.provider!!.fullName() }
            .setHeader("Provider")
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

        grid.addComponentColumn { service -> createServiceActionButtons(service) }
            .setHeader("Actions")
            .setAutoWidth(true)

        grid.getColumns().forEach { it.setResizable(true) }

        return grid
    }

    private fun createServiceActionButtons(service: Service): HorizontalLayout {
        val buttonLayout = HorizontalLayout()
        buttonLayout.isPadding = false
        buttonLayout.isSpacing = true

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

        // View details button
        val detailsButton = Button(Icon(VaadinIcon.INFO_CIRCLE)) {
            showServiceDetails(service)
        }.apply {
            element.setAttribute("title", "View Details")
            addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY)
        }

        buttonLayout.add(detailsButton, toggleButton)
        return buttonLayout
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

    private fun showServiceDetails(service: Service) {
        val dialog = com.vaadin.flow.component.dialog.Dialog()
        dialog.headerTitle = "Service Details"

        val content = VerticalLayout()
        content.isPadding = true
        content.setSpacing(true)
        content.width = "400px"

        // Create detail items
        val nameDetail = createDetailItem("Name:", service.name)
        val providerDetail = createDetailItem("Provider:", service.provider!!.fullName())
        val durationDetail = createDetailItem("Duration:", "${service.duration} minutes")
        val priceDetail = createDetailItem("Price:", "$${service.price}")
        val statusDetail = createDetailItem("Status:", if (service.active) "Active" else "Inactive")
        val descriptionDetail = createDetailItem("Description:", service.description ?: "No description")
        val createdAtDetail = createDetailItem(
            "Created:",
            service.createdAt.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        )

        content.add(
            nameDetail,
            providerDetail,
            durationDetail,
            priceDetail,
            statusDetail,
            descriptionDetail,
            createdAtDetail
        )

        val closeButton = Button("Close") {
            dialog.close()
        }

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
        valueSpan.style.set("overflow-wrap", "break-word")

        layout.add(labelSpan, valueSpan)
        return layout
    }

    private fun updateServiceList() {
        grid.setItems(serviceService.findAll())
    }
}