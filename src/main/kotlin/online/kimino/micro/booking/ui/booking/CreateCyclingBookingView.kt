package online.kimino.micro.booking.ui.booking

import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.combobox.ComboBox
import com.vaadin.flow.component.datepicker.DatePicker
import com.vaadin.flow.component.html.H2
import com.vaadin.flow.component.html.H3
import com.vaadin.flow.component.html.Paragraph
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.notification.NotificationVariant
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.TextArea
import com.vaadin.flow.component.timepicker.TimePicker
import com.vaadin.flow.router.HasDynamicTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.PermitAll
import online.kimino.micro.booking.entity.RecurrencePattern
import online.kimino.micro.booking.entity.Service
import online.kimino.micro.booking.entity.User
import online.kimino.micro.booking.security.SecurityUtils
import online.kimino.micro.booking.service.CyclicBookingService
import online.kimino.micro.booking.service.ServiceService
import online.kimino.micro.booking.service.UserService
import online.kimino.micro.booking.ui.MainLayout
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

@Route(value = "book/cyclic", layout = MainLayout::class)
@PermitAll
class CreateCyclicBookingView(
    private val serviceService: ServiceService,
    private val cyclicBookingService: CyclicBookingService,
    private val userService: UserService
) : VerticalLayout(), HasDynamicTitle {

    private val providerSelector = ComboBox<User>(getTranslation("booking.select.provider"))
    private val serviceSelector = ComboBox<Service>(getTranslation("booking.select.service"))
    private val recurrencePattern = ComboBox<RecurrencePattern>(getTranslation("cyclic.booking.recurrence"))
    private val startDate = DatePicker(getTranslation("cyclic.booking.start.date"))
    private val endDate = DatePicker(getTranslation("cyclic.booking.end.date"))
    private val dayOfWeek = ComboBox<DayOfWeek>(getTranslation("cyclic.booking.day.of.week"))
    private val dayOfMonth = ComboBox<Int>(getTranslation("cyclic.booking.day.of.month"))
    private val startTime = TimePicker(getTranslation("booking.start.time"))
    private val endTime = TimePicker(getTranslation("booking.end.time"))
    private val notesField = TextArea(getTranslation("notes.plural"))

    private val providerSelectionLayout = VerticalLayout()
    private val serviceSelectionLayout = VerticalLayout()
    private val recurrenceLayout = VerticalLayout()
    private val dateSelectionLayout = VerticalLayout()
    private val timeSelectionLayout = VerticalLayout()
    private val bookingDetailsLayout = VerticalLayout()

    private var selectedProvider: User? = null
    private var selectedService: Service? = null

    init {
        addClassName("create-cyclic-booking-view")
        setPadding(true)
        setSpacing(true)

        setupProviderSelector()
        setupServiceSelector()
        setupRecurrenceSelector()
        setupDatePickers()
        setupTimeSelectors()
        setupNotesField()

        // Initially, only provider selection is visible
        serviceSelectionLayout.isVisible = false
        recurrenceLayout.isVisible = false
        dateSelectionLayout.isVisible = false
        timeSelectionLayout.isVisible = false
        bookingDetailsLayout.isVisible = false

        add(
            H2(getTranslation("cyclic.booking.create")),
            providerSelectionLayout,
            serviceSelectionLayout,
            recurrenceLayout,
            dateSelectionLayout,
            timeSelectionLayout,
            bookingDetailsLayout
        )
    }

    private fun setupProviderSelector() {
        providerSelector.setItems(userService.getAllProviders())
        providerSelector.setItemLabelGenerator { provider ->
            if (!provider.companyName.isNullOrBlank()) {
                "${provider.fullName()} - ${provider.companyName}"
            } else {
                provider.fullName()
            }
        }
        providerSelector.width = "100%"
        providerSelector.placeholder = getTranslation("booking.provider.placeholder")

        providerSelector.addValueChangeListener { event ->
            selectedProvider = event.value
            if (selectedProvider != null) {
                updateServiceSelector(selectedProvider!!)
                serviceSelectionLayout.isVisible = true
                // Reset subsequent selections
                serviceSelector.clear()
                recurrencePattern.clear()
                startDate.clear()
                endDate.clear()
                dayOfWeek.clear()
                dayOfMonth.clear()
                startTime.clear()
                endTime.clear()
                recurrenceLayout.isVisible = false
                dateSelectionLayout.isVisible = false
                timeSelectionLayout.isVisible = false
                bookingDetailsLayout.isVisible = false
            } else {
                serviceSelectionLayout.isVisible = false
            }
        }

        providerSelectionLayout.add(
            H3(getTranslation("booking.select.provider")),
            providerSelector
        )
    }

    private fun setupServiceSelector() {
        serviceSelector.width = "100%"
        serviceSelector.placeholder = getTranslation("booking.service.placeholder")

        serviceSelector.addValueChangeListener { event ->
            selectedService = event.value
            if (selectedService != null) {
                recurrenceLayout.isVisible = true
                // Set default end time based on service duration
                startTime.addValueChangeListener {
                    if (it.value != null) {
                        endTime.value = it.value.plusMinutes(selectedService!!.duration.toLong())
                    }
                }
                // Reset subsequent selections
                recurrencePattern.clear()
                startDate.clear()
                endDate.clear()
                dayOfWeek.clear()
                dayOfMonth.clear()
                startTime.clear()
                endTime.clear()
                dateSelectionLayout.isVisible = false
                timeSelectionLayout.isVisible = false
                bookingDetailsLayout.isVisible = false
            } else {
                recurrenceLayout.isVisible = false
            }
        }

        serviceSelectionLayout.add(
            H3(getTranslation("booking.select.service")),
            serviceSelector
        )
    }

    private fun setupRecurrenceSelector() {
        recurrencePattern.setItems(RecurrencePattern.entries.toList())
        recurrencePattern.setItemLabelGenerator {
            when (it) {
                RecurrencePattern.WEEKLY -> getTranslation("cyclic.booking.weekly")
                RecurrencePattern.MONTHLY -> getTranslation("cyclic.booking.monthly")
                null -> ""
            }
        }
        recurrencePattern.width = "100%"

        dayOfWeek.setItems(DayOfWeek.entries.toList())
        dayOfWeek.setItemLabelGenerator {
            it.getDisplayName(TextStyle.FULL, Locale.getDefault())
        }
        dayOfWeek.width = "100%"
        dayOfWeek.isVisible = false

        dayOfMonth.setItems((1..31).toList())
        dayOfMonth.width = "100%"
        dayOfMonth.isVisible = false

        recurrencePattern.addValueChangeListener { event ->
            val pattern = event.value
            if (pattern != null) {
                when (pattern) {
                    RecurrencePattern.WEEKLY -> {
                        dayOfWeek.isVisible = true
                        dayOfMonth.isVisible = false
                    }

                    RecurrencePattern.MONTHLY -> {
                        dayOfWeek.isVisible = false
                        dayOfMonth.isVisible = true
                    }
                }
                dateSelectionLayout.isVisible = true
                // Reset date and time selections
                startDate.clear()
                endDate.clear()
                startTime.clear()
                endTime.clear()
                timeSelectionLayout.isVisible = false
                bookingDetailsLayout.isVisible = false
            } else {
                dayOfWeek.isVisible = false
                dayOfMonth.isVisible = false
                dateSelectionLayout.isVisible = false
            }
        }

        recurrenceLayout.add(
            H3(getTranslation("cyclic.booking.recurrence.pattern")),
            recurrencePattern,
            dayOfWeek,
            dayOfMonth
        )
    }

    private fun setupDatePickers() {
        startDate.min = LocalDate.now()
        startDate.value = LocalDate.now()
        startDate.width = "100%"

        endDate.min = LocalDate.now().plusWeeks(1)
        endDate.width = "100%"
        endDate.helperText = getTranslation("cyclic.booking.end.date.helper")

        startDate.addValueChangeListener { event ->
            if (event.value != null) {
                endDate.min = event.value.plusWeeks(1)
                if (endDate.value != null && endDate.value.isBefore(event.value)) {
                    endDate.value = event.value.plusWeeks(1)
                }
                timeSelectionLayout.isVisible = true
            } else {
                timeSelectionLayout.isVisible = false
            }
        }

        dateSelectionLayout.add(
            H3(getTranslation("cyclic.booking.date.range")),
            startDate,
            endDate
        )
    }

    private fun setupTimeSelectors() {
        startTime.step = Duration.ofMinutes(15)
        startTime.width = "100%"

        endTime.step = Duration.ofMinutes(15)
        endTime.width = "100%"

        startTime.addValueChangeListener { event ->
            if (event.value != null) {
                if (selectedService != null) {
                    endTime.value = event.value.plusMinutes(selectedService!!.duration.toLong())
                }
                updateBookingDetails()
            }
        }

        endTime.addValueChangeListener { event ->
            if (event.value != null) {
                updateBookingDetails()
            }
        }

        timeSelectionLayout.add(
            H3(getTranslation("booking.select.time")),
            startTime,
            endTime
        )
    }

    private fun setupNotesField() {
        notesField.width = "100%"
        notesField.maxLength = 500
        notesField.placeholder = getTranslation("booking.notes.placeholder")
    }

    private fun updateServiceSelector(provider: User) {
        val activeServices = serviceService.findAllActiveByProvider(provider)
        serviceSelector.setItems(activeServices)
        serviceSelector.setItemLabelGenerator { it.name }
    }

    private fun updateBookingDetails() {
        if (recurrencePattern.value != null && startDate.value != null &&
            startTime.value != null && endTime.value != null && selectedService != null
        ) {

            bookingDetailsLayout.removeAll()

            val formatter = DateTimeFormatter.ofPattern("HH:mm")

            val summaryText =
                Paragraph("${getTranslation("cyclic.booking.summary.about.to.book")}: ${selectedService!!.name}")

            val recurrenceText = Paragraph(
                when (recurrencePattern.value) {
                    RecurrencePattern.WEEKLY -> getTranslation(
                        "cyclic.booking.weekly.summary",
                        dayOfWeek.value?.getDisplayName(TextStyle.FULL, Locale.getDefault()) ?: ""
                    )

                    RecurrencePattern.MONTHLY -> getTranslation(
                        "cyclic.booking.monthly.summary",
                        dayOfMonth.value?.toString() ?: ""
                    )

                    null -> ""
                }
            )

            val dateRangeText = if (endDate.value != null) {
                Paragraph(
                    getTranslation(
                        "cyclic.booking.date.range.summary",
                        startDate.value.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                        endDate.value.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    )
                )
            } else {
                Paragraph(
                    getTranslation(
                        "cyclic.booking.date.range.summary.no.end",
                        startDate.value.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    )
                )
            }

            val timeText = Paragraph(
                getTranslation(
                    "cyclic.booking.time.summary",
                    startTime.value.format(formatter),
                    endTime.value.format(formatter)
                )
            )

            val providerText =
                Paragraph("${getTranslation("booking.provider")}: ${selectedService!!.provider!!.fullName()}")
            val priceText = Paragraph("${getTranslation("service.price")}: $${selectedService!!.price}")

            val confirmButton = Button(getTranslation("cyclic.booking.confirm")) {
                createCyclicBooking()
            }

            bookingDetailsLayout.add(
                H3(getTranslation("cyclic.booking.summary")),
                summaryText,
                recurrenceText,
                dateRangeText,
                timeText,
                providerText,
                priceText,
                notesField,
                confirmButton
            )

            bookingDetailsLayout.isVisible = true
        }
    }

    private fun createCyclicBooking() {
        try {
            // Validate required fields
            val pattern =
                recurrencePattern.value ?: throw IllegalArgumentException(getTranslation("validation.required"))
            val sDate = startDate.value ?: throw IllegalArgumentException(getTranslation("validation.required"))
            val sTime = startTime.value ?: throw IllegalArgumentException(getTranslation("validation.required"))
            val eTime = endTime.value ?: throw IllegalArgumentException(getTranslation("validation.required"))
            val service = selectedService ?: throw IllegalArgumentException(getTranslation("validation.required"))

            val dowValue = if (pattern == RecurrencePattern.WEEKLY) {
                dayOfWeek.value ?: throw IllegalArgumentException(getTranslation("validation.day.of.week.required"))
            } else null

            val domValue = if (pattern == RecurrencePattern.MONTHLY) {
                dayOfMonth.value ?: throw IllegalArgumentException(getTranslation("validation.day.of.month.required"))
            } else null

            val currentUser = SecurityUtils.getCurrentUsername()?.let {
                userService.findByEmail(it).orElseThrow { IllegalStateException(getTranslation("user.not.found")) }
            } ?: throw IllegalStateException(getTranslation("user.not.logged.in"))

            cyclicBookingService.createCyclicBooking(
                serviceId = service.id,
                userId = currentUser.id,
                startDate = sDate,
                endDate = endDate.value,
                recurrencePattern = pattern,
                dayOfWeek = dowValue,
                dayOfMonth = domValue,
                startTime = sTime,
                endTime = eTime,
                notes = notesField.value
            )

            Notification.show(getTranslation("cyclic.booking.created.success")).apply {
                position = Notification.Position.MIDDLE
                addThemeVariants(NotificationVariant.LUMO_SUCCESS)
            }

            // Redirect to bookings list
            ui.ifPresent { ui -> ui.navigate(CyclicBookingListView::class.java) }

        } catch (e: Exception) {
            Notification.show("${getTranslation("cyclic.booking.created.fail")}: ${e.message}").apply {
                position = Notification.Position.MIDDLE
                addThemeVariants(NotificationVariant.LUMO_ERROR)
            }
        }
    }

    override fun getPageTitle() = "micro-booking :: ${getTranslation("cyclic.booking.create")}"
}