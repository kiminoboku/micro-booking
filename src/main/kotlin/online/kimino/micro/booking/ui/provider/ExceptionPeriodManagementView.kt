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
import jakarta.annotation.security.RolesAllowed
import online.kimino.micro.booking.entity.ExceptionPeriod
import online.kimino.micro.booking.entity.UserRole
import online.kimino.micro.booking.security.SecurityUtils
import online.kimino.micro.booking.service.ExceptionPeriodService
import online.kimino.micro.booking.service.UserService
import online.kimino.micro.booking.ui.MainLayout
import java.time.format.DateTimeFormatter

@Route(value = "exception-periods", layout = MainLayout::class)
@RolesAllowed("PROVIDER")
class ExceptionPeriodManagementView(
    private val exceptionPeriodService: ExceptionPeriodService,
    private val userService: UserService
) : VerticalLayout(), HasDynamicTitle {

    private val grid = Grid<ExceptionPeriod>()
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    init {
        addClassName("exception-period-management-view")
        setSizeFull()
        setPadding(true)

        configureGrid()
        add(
            createToolbar(),
            createInfoSection(),
            grid
        )

        updateExceptionPeriodList()
    }

    private fun configureGrid() {
        grid.addClassName("exception-periods-grid")
        grid.setSizeFull()

        grid.addColumn { exceptionPeriod -> exceptionPeriod.startTime.format(formatter) }
            .setHeader(getTranslation("availability.start.time"))
            .setSortable(true)
            .setAutoWidth(true)

        grid.addColumn { exceptionPeriod -> exceptionPeriod.endTime.format(formatter) }
            .setHeader(getTranslation("availability.end.time"))
            .setSortable(true)
            .setAutoWidth(true)

        grid.addColumn { exceptionPeriod ->
            val duration = java.time.Duration.between(exceptionPeriod.startTime, exceptionPeriod.endTime)
            val days = duration.toDays()
            val hours = duration.toHoursPart()
            val minutes = duration.toMinutesPart()

            when {
                days > 0 -> "${days}d ${hours}h ${minutes}m"
                hours > 0 -> "${hours}h ${minutes}m"
                else -> "${minutes}m"
            }
        }
            .setHeader(getTranslation("booking.duration"))
            .setSortable(false)
            .setAutoWidth(true)

        grid.addColumn { exceptionPeriod -> exceptionPeriod.description }
            .setHeader(getTranslation("service.description"))
            .setSortable(false)
            .setAutoWidth(true)

        grid.addComponentColumn { exceptionPeriod -> createActionButtons(exceptionPeriod) }
            .setHeader(getTranslation("common.actions"))
            .setAutoWidth(true)

        grid.getColumns().forEach { it.setResizable(true) }
    }

    private fun createToolbar(): HorizontalLayout {
        val toolbar = HorizontalLayout()
        toolbar.setWidthFull()
        toolbar.justifyContentMode = FlexComponent.JustifyContentMode.BETWEEN
        toolbar.defaultVerticalComponentAlignment = FlexComponent.Alignment.CENTER

        val title = H2(getTranslation("provider.exception.periods"))

        val addButton = Button(getTranslation("availability.exception.add.new"), Icon(VaadinIcon.PLUS)) {
            showExceptionPeriodForm(null)
        }
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY)

        toolbar.add(title, addButton)
        return toolbar
    }

    private fun createInfoSection(): VerticalLayout {
        val infoLayout = VerticalLayout()
        infoLayout.setSpacing(false)
        infoLayout.setPadding(false)

        val infoText =
            Span(getTranslation("availability.inactive.warning"))
        infoText.element.style.set("font-style", "italic")

        infoLayout.add(infoText)
        return infoLayout
    }

    private fun createActionButtons(exceptionPeriod: ExceptionPeriod): HorizontalLayout {
        val buttonLayout = HorizontalLayout()
        buttonLayout.isPadding = false
        buttonLayout.isSpacing = true

        // Edit button
        val editButton = Button(Icon(VaadinIcon.EDIT)) {
            showExceptionPeriodForm(exceptionPeriod)
        }
        editButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY)
        editButton.element.setAttribute("title", getTranslation("common.edit"))

        // Delete button
        val deleteButton = Button(Icon(VaadinIcon.TRASH)) {
            deleteExceptionPeriod(exceptionPeriod)
        }
        deleteButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR)
        deleteButton.element.setAttribute("title", getTranslation("common.delete"))

        buttonLayout.add(editButton, deleteButton)
        return buttonLayout
    }

    private fun showExceptionPeriodForm(exceptionPeriod: ExceptionPeriod?) {
        val dialog = Dialog()
        dialog.headerTitle = if (exceptionPeriod == null)
            getTranslation("availability.exception.add.new")
        else
            getTranslation("availability.exception.edit")

        val currentUser = SecurityUtils.getCurrentUsername()?.let {
            userService.findByEmail(it).orElseThrow { IllegalStateException(getTranslation("user.not.found")) }
        } ?: return

        val exceptionPeriodForm = ExceptionPeriodForm(exceptionPeriod, currentUser)

        exceptionPeriodForm.addSaveListener { event ->
            saveExceptionPeriod(event.exceptionPeriod)
            dialog.close()
        }

        exceptionPeriodForm.addCancelListener {
            dialog.close()
        }

        dialog.add(exceptionPeriodForm)
        dialog.open()
    }

    private fun saveExceptionPeriod(exceptionPeriod: ExceptionPeriod) {
        try {
            if (exceptionPeriod.id == 0L) {
                // New exception period
                val currentUser = SecurityUtils.getCurrentUsername()?.let {
                    userService.findByEmail(it).orElseThrow { IllegalStateException(getTranslation("user.not.found")) }
                } ?: throw IllegalStateException(getTranslation("user.not.logged.in"))

                exceptionPeriod.provider = currentUser
            }

            // Validate that end time is after start time
            if (!exceptionPeriod.isValid()) {
                throw IllegalArgumentException(getTranslation("validation.availability.end.after.start"))
            }

            exceptionPeriodService.createExceptionPeriod(exceptionPeriod)

            Notification.show(getTranslation("availability.saved")).apply {
                position = Notification.Position.MIDDLE
                addThemeVariants(NotificationVariant.LUMO_SUCCESS)
            }

            updateExceptionPeriodList()
        } catch (e: Exception) {
            Notification.show(getTranslation("availability.save.failed", e.message ?: "")).apply {
                position = Notification.Position.MIDDLE
                addThemeVariants(NotificationVariant.LUMO_ERROR)
            }
        }
    }

    private fun deleteExceptionPeriod(exceptionPeriod: ExceptionPeriod) {
        val dialog = Dialog()
        dialog.headerTitle = getTranslation("availability.delete")

        val content = VerticalLayout()
        content.isPadding = true
        content.setSpacing(true)

        val startTime = exceptionPeriod.startTime.format(formatter)
        val endTime = exceptionPeriod.endTime.format(formatter)

        content.add(
            Span(getTranslation("availability.delete.confirm")),
            Span("${startTime} to ${endTime}")
        )

        val buttonLayout = HorizontalLayout()
        buttonLayout.justifyContentMode = FlexComponent.JustifyContentMode.END

        val cancelButton = Button(getTranslation("common.cancel")) {
            dialog.close()
        }

        val confirmButton = Button(getTranslation("common.delete")) {
            try {
                exceptionPeriodService.deleteExceptionPeriod(exceptionPeriod.id)

                Notification.show(getTranslation("availability.deleted")).apply {
                    position = Notification.Position.MIDDLE
                    addThemeVariants(NotificationVariant.LUMO_SUCCESS)
                }

                dialog.close()
                updateExceptionPeriodList()
            } catch (e: Exception) {
                Notification.show(getTranslation("availability.delete.failed", e.message ?: "")).apply {
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

    private fun updateExceptionPeriodList() {
        val currentUser = SecurityUtils.getCurrentUsername()?.let {
            userService.findByEmail(it).orElse(null)
        }

        if (currentUser == null || !SecurityUtils.hasRole(UserRole.PROVIDER)) {
            grid.setItems(emptyList())
            return
        }

        val exceptionPeriods = exceptionPeriodService.findAllByProviderId(currentUser.id)
        grid.setItems(exceptionPeriods)
    }

    override fun getPageTitle() = "micro-booking :: ${getTranslation("provider.exception.periods")}"
}