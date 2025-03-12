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
import online.kimino.micro.booking.entity.CyclicBookingStatus
import online.kimino.micro.booking.entity.RecurrencePattern
import online.kimino.micro.booking.entity.Service
import online.kimino.micro.booking.entity.User
import online.kimino.micro.booking.security.SecurityUtils
import online.kimino.micro.booking.service.AvailabilityService
import online.kimino.micro.booking.service.BookingService
import online.kimino.micro.booking.service.ServiceService
import online.kimino.micro.booking.service.UserService
import online.kimino.micro.booking.ui.MainLayout
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Route(value = "book", layout = MainLayout::class)
@PermitAll
class CreateBookingView(
    private val serviceService: ServiceService,
    private val availabilityService: AvailabilityService,
    private val bookingService: BookingService,
    private val userService: UserService
) : VerticalLayout(), HasDynamicTitle {

    private val logger = KotlinLogging.logger {}
    private val providerSelector = ComboBox<User>(getTranslation("booking.select.provider"))
    private val serviceSelector = ComboBox<Service>(getTranslation("booking.select.service"))
    private val datePicker = DatePicker(getTranslation("booking.select.date"))
    private val timeSelector = ComboBox<LocalTime>(getTranslation("booking.select.time"))
    private val notesField = TextArea(getTranslation("notes.plural"))

    private val providerSelectionLayout = VerticalLayout()
    private val serviceSelectionLayout = VerticalLayout()
    private val serviceInfoLayout = VerticalLayout()
    private val timeSelectionLayout = VerticalLayout()
    private val bookingDetailsLayout = VerticalLayout()

    private var selectedProvider: User? = null
    private var selectedService: Service? = null
    private var availableTimes: List<Map<String, LocalDateTime>> = emptyList()

    init {
        addClassName("create-booking-view")
        setPadding(true)
        setSpacing(true)

        setupProviderSelector()
        setupServiceSelector()
        setupDatePicker()
        setupTimeSelector()
        setupNotesField()

        // Initially, only provider selection is visible
        serviceSelectionLayout.isVisible = false
        serviceInfoLayout.isVisible = false
        timeSelectionLayout.isVisible = false
        bookingDetailsLayout.isVisible = false

        add(
            H2(getTranslation("booking.create")),
            providerSelectionLayout,
            serviceSelectionLayout,
            serviceInfoLayout,
            timeSelectionLayout,
            bookingDetailsLayout
        )
    }

    private fun setupProviderSelector() {
        providerSelector.setItems(userService.getAllProviders())

        // Update item label generator to include company name if available
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
                datePicker.clear()
                timeSelector.clear()
                serviceInfoLayout.isVisible = false
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
                showServiceInfo(selectedService!!)
                setupAvailableDates(selectedService!!.id)
                serviceInfoLayout.isVisible = true
                timeSelectionLayout.isVisible = true
                // Reset date and time selections
                datePicker.clear()
                timeSelector.clear()
                bookingDetailsLayout.isVisible = false
            } else {
                serviceInfoLayout.isVisible = false
                timeSelectionLayout.isVisible = false
                bookingDetailsLayout.isVisible = false
            }
        }

        serviceSelectionLayout.add(
            H3(getTranslation("booking.select.service")),
            serviceSelector
        )
    }

    private fun updateServiceSelector(provider: User) {
        // Get active services for the selected provider
        val activeServices = serviceService.findAllActiveByProvider(provider)
        serviceSelector.setItems(activeServices)
        serviceSelector.setItemLabelGenerator { it.name }
    }

    private fun setupDatePicker() {
        datePicker.width = "100%"
        datePicker.isEnabled = false

        datePicker.addValueChangeListener { event ->
            val selectedDate = event.value
            if (selectedDate != null && selectedService != null) {
                fetchAvailableTimeSlots(selectedService!!.id, selectedDate)
                timeSelector.isEnabled = true
            } else {
                timeSelector.isEnabled = false
                bookingDetailsLayout.isVisible = false
            }
        }

        timeSelectionLayout.add(
            H3(getTranslation("booking.select.datetime")),
            datePicker,
            timeSelector
        )
    }

    private fun setupTimeSelector() {
        timeSelector.width = "100%"
        timeSelector.isEnabled = false

        timeSelector.addValueChangeListener { event ->
            val selectedTime = event.value
            if (selectedTime != null && selectedService != null && datePicker.value != null) {
                showBookingDetails(selectedService!!, datePicker.value, selectedTime)
                bookingDetailsLayout.isVisible = true
            } else {
                bookingDetailsLayout.isVisible = false
            }
        }
    }

    private fun setupNotesField() {
        notesField.width = "100%"
        notesField.maxLength = 500
        notesField.placeholder = getTranslation("booking.notes.placeholder")
    }

    private fun setupAvailableDates(serviceId: Long) {
        val availableDates = availabilityService.getAvailableDates(serviceId)

        if (availableDates.isEmpty()) {
            Notification.show(getTranslation("booking.no.available.dates"))
            datePicker.isEnabled = false
            return
        }

        // Set min and max dates
        datePicker.min = availableDates.minOf { it }
        datePicker.max = availableDates.maxOf { it }

        datePicker.addValueChangeListener { event ->
            if (event.value != null && !availableDates.contains(event.value)) {
                Notification.show(getTranslation("booking.date.not.available"))
                datePicker.value = null
            }
        }

        datePicker.isEnabled = true
    }

    private fun fetchAvailableTimeSlots(serviceId: Long, date: LocalDate) {
        availableTimes = availabilityService.getAvailableTimeSlots(serviceId, date)

        if (availableTimes.isEmpty()) {
            // Check if there's a cyclic booking on this day that might be conflicting
            val isCyclicBookingDay = checkForCyclicBookingConflicts(serviceId, date)

            if (isCyclicBookingDay) {
                Notification.show(getTranslation("booking.no.available.slots.cyclic.conflict"))
            } else {
                Notification.show(getTranslation("booking.no.available.slots"))
            }

            timeSelector.isEnabled = false
            return
        }

        val times = availableTimes.map { it["start"]!!.toLocalTime() }
        timeSelector.setItems(times)
        timeSelector.setItemLabelGenerator { it.format(DateTimeFormatter.ofPattern("HH:mm")) }
        timeSelector.isEnabled = true
    }

    /**
     * Check if there are any cyclic bookings that might be causing conflicts on this date
     */
    private fun checkForCyclicBookingConflicts(serviceId: Long, date: LocalDate): Boolean {
        try {
            // We can use a simplified check just to give better error messages
            val dayOfWeek = date.dayOfWeek
            val dayOfMonth = date.dayOfMonth

            // Check if service has availabilities for this day
            val hasAvailability = availabilityService.findAllByServiceId(serviceId)
                .any { it.dayOfWeek == dayOfWeek }

            // If no availabilities defined for this day, it's not a cyclic booking conflict
            if (!hasAvailability) {
                return false
            }

            // Simple check to see if there might be cyclic bookings on this date
            val provider = selectedProvider ?: return false

            return provider.services
                .filter { it.id == serviceId }
                .flatMap { it.bookings }
                .filter { it.cyclicBooking != null && it.cyclicBooking!!.status == CyclicBookingStatus.CONFIRMED }
                .any { booking ->
                    val cb = booking.cyclicBooking!!

                    // Check if this cyclic booking applies to this date
                    when (cb.recurrencePattern) {
                        RecurrencePattern.WEEKLY -> cb.dayOfWeek == dayOfWeek
                        RecurrencePattern.MONTHLY -> cb.dayOfMonth == dayOfMonth
                    }
                }
        } catch (e: Exception) {
            logger.warn(e, { "Error checking for cyclic booking conflicts" })
            return false // Default to regular message on error
        }
    }

    private fun showServiceInfo(service: Service) {
        serviceInfoLayout.removeAll()

        val priceText = Paragraph("${getTranslation("service.price")}: $${service.price}")
        val durationText =
            Paragraph("${getTranslation("service.duration")}: ${getTranslation("common.minutes", service.duration)}")
        val descriptionText = Paragraph(service.description ?: getTranslation("service.no.description"))

        serviceInfoLayout.add(
            H3(service.name),
            priceText,
            durationText,
            descriptionText
        )
    }

    private fun showBookingDetails(service: Service, date: LocalDate, time: LocalTime) {
        bookingDetailsLayout.removeAll()

        val selectedDateTime = LocalDateTime.of(date, time)
        val endDateTime = selectedDateTime.plusMinutes(service.duration.toLong())

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

        val summaryText = Paragraph("${getTranslation("booking.summary.about.to.book")}: ${service.name}")
        val dateTimeText =
            Paragraph(
                getTranslation(
                    "booking.datetime",
                    selectedDateTime.format(formatter),
                    endDateTime.format(formatter)
                )
            )
        val providerText = Paragraph("${getTranslation("booking.provider")}: ${service.provider!!.fullName()}")
        val priceText = Paragraph("${getTranslation("service.price")}: $${service.price}")

        val confirmButton = Button(getTranslation("booking.confirm")) {
            createBooking(service.id, selectedDateTime)
        }

        bookingDetailsLayout.add(
            H3(getTranslation("booking.summary")),
            summaryText,
            dateTimeText,
            providerText,
            priceText,
            notesField,
            confirmButton
        )
    }

    private fun createBooking(serviceId: Long, startTime: LocalDateTime) {
        try {
            val currentUser = SecurityUtils.getCurrentUsername()?.let {
                userService.findByEmail(it).orElseThrow { NoSuchElementException(getTranslation("user.not.found")) }
            } ?: throw IllegalStateException(getTranslation("user.not.logged.in"))

            val booking = bookingService.createBooking(serviceId, currentUser.id, startTime)

            if (notesField.value.isNotBlank()) {
                bookingService.updateBookingNotes(booking.id, notesField.value)
            }

            Notification.show(getTranslation("booking.created.success")).apply {
                position = Notification.Position.MIDDLE
                addThemeVariants(NotificationVariant.LUMO_SUCCESS)
            }

            // Redirect to bookings list
            ui.ifPresent { ui -> ui.navigate(BookingListView::class.java) }

        } catch (e: Exception) {
            Notification.show("${getTranslation("booking.created.fail")}: ${e.message}").apply {
                position = Notification.Position.MIDDLE
                addThemeVariants(NotificationVariant.LUMO_ERROR)
            }
            logger.warn(e, { "Error when creating booking" })
        }
    }

    override fun getPageTitle() = "micro-booking :: ${getTranslation("booking.create")}"
}