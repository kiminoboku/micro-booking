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
import com.vaadin.flow.router.HasDynamicTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.router.RouterLink
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.security.RolesAllowed
import online.kimino.micro.booking.entity.Service
import online.kimino.micro.booking.service.ServiceService
import online.kimino.micro.booking.ui.MainLayout

@Route(value = "admin/services", layout = MainLayout::class)
@RolesAllowed("ADMIN")
class AdminServicesView(
    private val serviceService: ServiceService
) : VerticalLayout(), HasDynamicTitle {

    private val logger = KotlinLogging.logger {}
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

        header.add(H2(getTranslation("admin.services")))

        val navLayout = HorizontalLayout()
        navLayout.setWidthFull()
        navLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.START)
        navLayout.add(
            RouterLink(getTranslation("dashboard.title"), AdminDashboardView::class.java),
            RouterLink(getTranslation("user.title.plural"), AdminUsersView::class.java),
            RouterLink(getTranslation("admin.bookings"), AdminBookingsView::class.java)
        )

        header.add(navLayout)
        return header
    }

    private fun configureGrid(): Grid<Service> {
        grid.addClassName("services-grid")
        grid.setSizeFull()

        grid.addColumn { service -> service.name }
            .setHeader(getTranslation("service.name"))
            .setSortable(true)
            .setAutoWidth(true)

        grid.addColumn { service -> service.provider!!.fullName() }
            .setHeader(getTranslation("booking.provider"))
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

        grid.addComponentColumn { service -> createServiceActionButtons(service) }
            .setHeader(getTranslation("common.actions"))
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

        // View details button
        val detailsButton = Button(Icon(VaadinIcon.INFO_CIRCLE)) {
            showServiceDetails(service)
        }.apply {
            element.setAttribute("title", getTranslation("service.details"))
            addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY)
        }

        buttonLayout.add(detailsButton, toggleButton)
        return buttonLayout
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
            logger.warn(e, { "Error when toggling service status" })
        }
    }

    private fun showServiceDetails(service: Service) {
        val dialog = com.vaadin.flow.component.dialog.Dialog()
        dialog.headerTitle = getTranslation("service.details")

        val content = VerticalLayout()
        content.isPadding = true
        content.setSpacing(true)
        content.width = "400px"

        // Create detail items
        val nameDetail = createDetailItem("${getTranslation("service.name")}:", service.name)
        val providerDetail = createDetailItem("${getTranslation("booking.provider")}:", service.provider!!.fullName())
        val durationDetail = createDetailItem(
            "${getTranslation("service.duration")}:",
            "${service.duration} ${getTranslation("common.minutes", service.duration)}"
        )
        val priceDetail = createDetailItem("${getTranslation("service.price")}:", "$${service.price}")
        val statusDetail = createDetailItem(
            "${getTranslation("booking.status")}:",
            if (service.active) getTranslation("service.active") else getTranslation("service.inactive")
        )
        val descriptionDetail = createDetailItem(
            "${getTranslation("service.description")}:",
            service.description ?: getTranslation("service.description")
        )
        val createdAtDetail = createDetailItem(
            "${getTranslation("common.created.at")}:",
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

        val closeButton = Button(getTranslation("common.close")) {
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

    override fun getPageTitle() = "micro-booking :: ${getTranslation("admin.services")}"
}