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
import com.vaadin.flow.router.HasDynamicTitle
import com.vaadin.flow.router.Route
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.security.PermitAll
import online.kimino.micro.booking.entity.RecurrencePattern
import online.kimino.micro.booking.entity.Service
import online.kimino.micro.booking.entity.User
import online.kimino.micro.booking.security.SecurityUtils
import online.kimino.micro.booking.service.AvailabilityService
import online.kimino.micro.booking.service.CyclicBookingService
import online.kimino.micro.booking.service.ServiceService
import online.kimino.micro.booking.service.UserService
import online.kimino.micro.booking.ui.MainLayout
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

@Route(value = "book/cyclic", layout = MainLayout::class)
@PermitAll
class CreateCyclicBookingView(
    private val serviceService: ServiceService,
    private val availabilityService: AvailabilityService,
    private val cyclicBookingService: CyclicBookingService,
    private val userService: UserService
) : VerticalLayout(), HasDynamicTitle {

    private val logger = KotlinLogging.logger {}

    private val providerSelector = ComboBox<User>(getTranslation("booking.select.provider"))
    private val serviceSelector = ComboBox<Service>(getTranslation("booking.select.service"))
    private val recurrencePattern = ComboBox<RecurrencePattern>(getTranslation("cyclic.booking.recurrence"))
    private val startDate = DatePicker(getTranslation("cyclic.booking.start.date"))
    private val endDate = DatePicker(getTranslation("cyclic.booking.end.date"))
    private val dayOfWeek = ComboBox<DayOfWeek>(getTranslation("cyclic.booking.day.of.week"))
    private val dayOfMonth = ComboBox<Int>(getTranslation("cyclic.booking.day.of.month"))
    private val timeSelector = ComboBox<LocalTime>(getTranslation("booking.select.time"))
    private val notesField = TextArea(getTranslation("notes.plural"))

    private val providerSelectionLayout = VerticalLayout()
    private val serviceSelectionLayout = VerticalLayout()
    private val recurrenceLayout = VerticalLayout()
    private val dateSelectionLayout = VerticalLayout()
    private val timeSelectionLayout = VerticalLayout()
    private val bookingDetailsLayout = VerticalLayout()

    private var selectedProvider: User? = null
    private var selectedService: Service? = null
    private var availableTimes: List<Map<String, LocalDateTime>> = emptyList()

    init {
        addClassName("create-cyclic-booking-view")
        setPadding(true)
        setSpacing(true)

        setupProviderSelector()
        setupServiceSelector()
        setupRecurrenceSelector()
        setupDatePickers()
        setupTimeSelector()
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
                timeSelector.clear()
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
                // Reset subsequent selections
                recurrencePattern.clear()
                startDate.clear()
                endDate.clear()
                dayOfWeek.clear()
                dayOfMonth.clear()
                timeSelector.clear()
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

        // Day of week change listener to fetch available times
        dayOfWeek.addValueChangeListener { event ->
            if (event.value != null && selectedService != null) {
                fetchAvailableTimeSlots()
                timeSelectionLayout.isVisible = true
            }
        }

        dayOfMonth.setItems((1..31).toList())
        dayOfMonth.width = "100%"
        dayOfMonth.isVisible = false

        // Day of month change listener to fetch available times
        dayOfMonth.addValueChangeListener { event ->
            if (event.value != null && selectedService != null) {
                fetchAvailableTimeSlots()
                timeSelectionLayout.isVisible = true
            }
        }

        recurrencePattern.addValueChangeListener { event ->
            val pattern = event.value
            if (pattern != null) {
                when (pattern) {
                    RecurrencePattern.WEEKLY -> {
                        dayOfWeek.isVisible = true
                        dayOfMonth.isVisible = false
                        dayOfMonth.clear()
                    }

                    RecurrencePattern.MONTHLY -> {
                        dayOfWeek.isVisible = false
                        dayOfWeek.clear()
                        dayOfMonth.isVisible = true
                    }
                }
                dateSelectionLayout.isVisible = true
                // Reset subsequent selections
                startDate.clear()
                endDate.clear()
                timeSelector.clear()
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
            }
        }

        dateSelectionLayout.add(
            H3(getTranslation("cyclic.booking.date.range")),
            startDate,
            endDate
        )
    }

    private fun setupTimeSelector() {
        timeSelector.width = "100%"
        timeSelector.isEnabled = false
        timeSelector.setItemLabelGenerator { it.format(DateTimeFormatter.ofPattern("HH:mm")) }

        timeSelector.addValueChangeListener { event ->
            if (event.value != null && selectedService != null) {
                updateBookingDetails()
            }
        }

        timeSelectionLayout.add(
            H3(getTranslation("booking.select.time")),
            timeSelector
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

    private fun fetchAvailableTimeSlots() {
        if (selectedService == null) {
            return
        }

        timeSelector.clear()
        bookingDetailsLayout.isVisible = false

        // Get the day for which we need to check availability
        val day = when (recurrencePattern.value) {
            RecurrencePattern.WEEKLY -> dayOfWeek.value
            RecurrencePattern.MONTHLY -> {
                // Find the next date with this day of month
                findNextDateWithDayOfMonth(dayOfMonth.value)?.dayOfWeek
            }

            null -> null
        } ?: return

        // Get available time slots based on service availability for this day
        val availabilities = availabilityService.findAllByServiceId(selectedService!!.id)
            .filter { it.dayOfWeek == day }

        if (availabilities.isEmpty()) {
            Notification.show(getTranslation("booking.no.available.slots"))
            timeSelector.isEnabled = false
            return
        }

        // Use a sample date with the correct day of week to fetch time slots
        val sampleDate = findSampleDateWithDayOfWeek(day)
        availableTimes = availabilityService.getAvailableTimeSlots(selectedService!!.id, sampleDate)

        if (availableTimes.isEmpty()) {
            Notification.show(getTranslation("booking.no.available.slots"))
            timeSelector.isEnabled = false
            return
        }

        val times = availableTimes.map { it["start"]!!.toLocalTime() }
        timeSelector.setItems(times)
        timeSelector.isEnabled = true
    }

    private fun findSampleDateWithDayOfWeek(dayOfWeek: DayOfWeek): LocalDate {
        var date = LocalDate.now()
        while (date.dayOfWeek != dayOfWeek) {
            date = date.plusDays(1)
        }
        return date
    }

    private fun findNextDateWithDayOfMonth(dayOfMonth: Int?): LocalDate? {
        if (dayOfMonth == null) {
            return null
        }

        var date = LocalDate.now()
        // If current day is higher than target day, move to next month
        if (date.dayOfMonth > dayOfMonth) {
            date = date.plusMonths(1)
        }

        // Try to set the day of month (handling month length differences)
        try {
            date = date.withDayOfMonth(dayOfMonth)
        } catch (e: Exception) {
            // Handle invalid dates (e.g., Feb 30)
            return null
        }

        return date
    }

    private fun updateBookingDetails() {
        if (recurrencePattern.value != null && startDate.value != null &&
            timeSelector.value != null && selectedService != null
        ) {

            bookingDetailsLayout.removeAll()

            val formatter = DateTimeFormatter.ofPattern("HH:mm")

            // Calculate end time based on service duration
            val startTime = timeSelector.value
            val endTime = startTime.plusMinutes(selectedService!!.duration.toLong())

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
                    startTime.format(formatter),
                    endTime.format(formatter)
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
            val startTimeValue =
                timeSelector.value ?: throw IllegalArgumentException(getTranslation("validation.required"))
            val service = selectedService ?: throw IllegalArgumentException(getTranslation("validation.required"))

            // Calculate end time based on service duration
            val endTimeValue = startTimeValue.plusMinutes(service.duration.toLong())

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
                startTime = startTimeValue,
                endTime = endTimeValue,
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
            logger.error(e, { "Error creating cyclic booking" })
        }
    }

    override fun getPageTitle() = "micro-booking :: ${getTranslation("cyclic.booking.create")}"
}