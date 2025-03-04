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
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.PermitAll
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
@PageTitle("Book a Service | Booking SaaS")
@PermitAll
class CreateBookingView(
    private val serviceService: ServiceService,
    private val availabilityService: AvailabilityService,
    private val bookingService: BookingService,
    private val userService: UserService
) : VerticalLayout() {

    private val providerSelector = ComboBox<User>("Select a Provider")
    private val serviceSelector = ComboBox<Service>("Select a Service")
    private val datePicker = DatePicker("Select a Date")
    private val timeSelector = ComboBox<LocalTime>("Select a Time")
    private val notesField = TextArea("Notes")

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
            H2("Book a Service"),
            providerSelectionLayout,
            serviceSelectionLayout,
            serviceInfoLayout,
            timeSelectionLayout,
            bookingDetailsLayout
        )
    }

    private fun setupProviderSelector() {
        providerSelector.setItems(userService.getAllProviders())
        providerSelector.setItemLabelGenerator { it.fullName() }
        providerSelector.width = "100%"
        providerSelector.placeholder = "First select a service provider"

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
            H3("Select a Provider"),
            providerSelector
        )
    }

    private fun setupServiceSelector() {
        serviceSelector.width = "100%"
        serviceSelector.placeholder = "Select a service offered by this provider"

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
            H3("Select a Service"),
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
            H3("Select Date and Time"),
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
        notesField.placeholder = "Additional information for the service provider"
    }

    private fun setupAvailableDates(serviceId: Long) {
        val availableDates = availabilityService.getAvailableDates(serviceId)

        if (availableDates.isEmpty()) {
            Notification.show("No available dates found for this service")
            datePicker.isEnabled = false
            return
        }

        // Set min and max dates
        datePicker.min = availableDates.minOf { it }
        datePicker.max = availableDates.maxOf { it }

        datePicker.addValueChangeListener { event ->
            if (event.value != null && !availableDates.contains(event.value)) {
                Notification.show("The selected date is not available")
                datePicker.value = null
            }
        }

        datePicker.isEnabled = true
    }

    private fun fetchAvailableTimeSlots(serviceId: Long, date: LocalDate) {
        availableTimes = availabilityService.getAvailableTimeSlots(serviceId, date)

        if (availableTimes.isEmpty()) {
            Notification.show("No available time slots found for the selected date")
            timeSelector.isEnabled = false
            return
        }

        val times = availableTimes.map { it["start"]!!.toLocalTime() }
        timeSelector.setItems(times)
        timeSelector.setItemLabelGenerator { it.format(DateTimeFormatter.ofPattern("HH:mm")) }
        timeSelector.isEnabled = true
    }

    private fun showServiceInfo(service: Service) {
        serviceInfoLayout.removeAll()

        val priceText = Paragraph("Price: $${service.price}")
        val durationText = Paragraph("Duration: ${service.duration} minutes")
        val descriptionText = Paragraph(service.description ?: "No description available")

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

        val summaryText = Paragraph("You are about to book: ${service.name}")
        val dateTimeText =
            Paragraph("Date/Time: ${selectedDateTime.format(formatter)} to ${endDateTime.format(formatter)}")
        val providerText = Paragraph("Provider: ${service.provider!!.fullName()}")
        val priceText = Paragraph("Price: $${service.price}")

        val confirmButton = Button("Confirm Booking") {
            createBooking(service.id, selectedDateTime)
        }

        bookingDetailsLayout.add(
            H3("Booking Summary"),
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
                userService.findByEmail(it).orElseThrow { NoSuchElementException("User not found") }
            } ?: throw IllegalStateException("User not logged in")

            val booking = bookingService.createBooking(serviceId, currentUser.id, startTime)

            if (notesField.value.isNotBlank()) {
                bookingService.updateBookingNotes(booking.id, notesField.value)
            }

            Notification.show("Booking created successfully!").apply {
                position = Notification.Position.MIDDLE
                addThemeVariants(NotificationVariant.LUMO_SUCCESS)
            }

            // Redirect to bookings list
            ui.ifPresent { ui -> ui.navigate(BookingListView::class.java) }

        } catch (e: Exception) {
            Notification.show("Failed to create booking: ${e.message}").apply {
                position = Notification.Position.MIDDLE
                addThemeVariants(NotificationVariant.LUMO_ERROR)
            }
        }
    }
}