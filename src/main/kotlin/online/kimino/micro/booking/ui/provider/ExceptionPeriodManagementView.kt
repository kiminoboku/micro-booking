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
import online.kimino.micro.booking.entity.ExceptionPeriod
import online.kimino.micro.booking.entity.UserRole
import online.kimino.micro.booking.security.SecurityUtils
import online.kimino.micro.booking.service.ExceptionPeriodService
import online.kimino.micro.booking.service.UserService
import online.kimino.micro.booking.ui.MainLayout
import java.time.format.DateTimeFormatter

@Route(value = "exception-periods", layout = MainLayout::class)
@PageTitle("Manage Exception Periods | Booking SaaS")
@RolesAllowed("PROVIDER")
class ExceptionPeriodManagementView(
    private val exceptionPeriodService: ExceptionPeriodService,
    private val userService: UserService
) : VerticalLayout() {

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
            .setHeader("Start Time")
            .setSortable(true)
            .setAutoWidth(true)

        grid.addColumn { exceptionPeriod -> exceptionPeriod.endTime.format(formatter) }
            .setHeader("End Time")
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
            .setHeader("Duration")
            .setSortable(false)
            .setAutoWidth(true)

        grid.addColumn { exceptionPeriod -> exceptionPeriod.description }
            .setHeader("Description")
            .setSortable(false)
            .setAutoWidth(true)

        grid.addComponentColumn { exceptionPeriod -> createActionButtons(exceptionPeriod) }
            .setHeader("Actions")
            .setAutoWidth(true)

        grid.getColumns().forEach { it.setResizable(true) }
    }

    private fun createToolbar(): HorizontalLayout {
        val toolbar = HorizontalLayout()
        toolbar.setWidthFull()
        toolbar.justifyContentMode = FlexComponent.JustifyContentMode.BETWEEN
        toolbar.defaultVerticalComponentAlignment = FlexComponent.Alignment.CENTER

        val title = H2("Exception Periods")

        val addButton = Button("Add Exception Period", Icon(VaadinIcon.PLUS)) {
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
            Span("Define periods when you are unavailable despite your regular schedule (vacations, personal days, etc.)")
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
        editButton.element.setAttribute("title", "Edit")

        // Delete button
        val deleteButton = Button(Icon(VaadinIcon.TRASH)) {
            deleteExceptionPeriod(exceptionPeriod)
        }
        deleteButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR)
        deleteButton.element.setAttribute("title", "Delete")

        buttonLayout.add(editButton, deleteButton)
        return buttonLayout
    }

    private fun showExceptionPeriodForm(exceptionPeriod: ExceptionPeriod?) {
        val dialog = Dialog()
        dialog.headerTitle = if (exceptionPeriod == null) "Add New Exception Period" else "Edit Exception Period"

        val currentUser = SecurityUtils.getCurrentUsername()?.let {
            userService.findByEmail(it).orElseThrow { IllegalStateException("User not found") }
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
                    userService.findByEmail(it).orElseThrow { IllegalStateException("User not found") }
                } ?: throw IllegalStateException("User not logged in")

                exceptionPeriod.provider = currentUser
            }

            // Validate that end time is after start time
            if (!exceptionPeriod.isValid()) {
                throw IllegalArgumentException("End time must be after start time")
            }

            exceptionPeriodService.createExceptionPeriod(exceptionPeriod)

            Notification.show("Exception period saved successfully").apply {
                position = Notification.Position.MIDDLE
                addThemeVariants(NotificationVariant.LUMO_SUCCESS)
            }

            updateExceptionPeriodList()
        } catch (e: Exception) {
            Notification.show("Failed to save exception period: ${e.message}").apply {
                position = Notification.Position.MIDDLE
                addThemeVariants(NotificationVariant.LUMO_ERROR)
            }
        }
    }

    private fun deleteExceptionPeriod(exceptionPeriod: ExceptionPeriod) {
        val dialog = Dialog()
        dialog.headerTitle = "Delete Exception Period"

        val content = VerticalLayout()
        content.isPadding = true
        content.setSpacing(true)

        val startTime = exceptionPeriod.startTime.format(formatter)
        val endTime = exceptionPeriod.endTime.format(formatter)

        content.add(
            Span("Are you sure you want to delete this exception period?"),
            Span("${startTime} to ${endTime}")
        )

        val buttonLayout = HorizontalLayout()
        buttonLayout.justifyContentMode = FlexComponent.JustifyContentMode.END

        val cancelButton = Button("Cancel") {
            dialog.close()
        }

        val confirmButton = Button("Delete") {
            try {
                exceptionPeriodService.deleteExceptionPeriod(exceptionPeriod.id)

                Notification.show("Exception period deleted").apply {
                    position = Notification.Position.MIDDLE
                    addThemeVariants(NotificationVariant.LUMO_SUCCESS)
                }

                dialog.close()
                updateExceptionPeriodList()
            } catch (e: Exception) {
                Notification.show("Failed to delete exception period: ${e.message}").apply {
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
}