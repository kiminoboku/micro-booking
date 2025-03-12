package online.kimino.micro.booking.ui.booking

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.ComponentEvent
import com.vaadin.flow.component.ComponentEventListener
import com.vaadin.flow.component.Key
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.combobox.ComboBox
import com.vaadin.flow.component.datepicker.DatePicker
import com.vaadin.flow.component.formlayout.FormLayout
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.textfield.TextArea
import com.vaadin.flow.component.timepicker.TimePicker
import com.vaadin.flow.data.binder.BeanValidationBinder
import com.vaadin.flow.data.binder.ValidationResult
import com.vaadin.flow.shared.Registration
import online.kimino.micro.booking.entity.CyclicBooking
import online.kimino.micro.booking.entity.RecurrencePattern
import online.kimino.micro.booking.entity.Service
import online.kimino.micro.booking.entity.User
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.*

class CyclicBookingForm(
    cyclicBooking: CyclicBooking?,
    private val availableServices: List<Service>
) : FormLayout() {
    private val service = ComboBox<Service>(getTranslation("booking.service"))
    private val startDate = DatePicker(getTranslation("cyclic.booking.start.date"))
    private val endDate = DatePicker(getTranslation("cyclic.booking.end.date"))
    private val recurrencePattern = ComboBox<RecurrencePattern>(getTranslation("cyclic.booking.recurrence"))
    private val dayOfWeek = ComboBox<DayOfWeek>(getTranslation("cyclic.booking.day.of.week"))
    private val dayOfMonth = ComboBox<Int>(getTranslation("cyclic.booking.day.of.month"))
    private val startTime = TimePicker(getTranslation("booking.start.time"))
    private val endTime = TimePicker(getTranslation("booking.end.time"))
    private val notes = TextArea(getTranslation("booking.notes"))

    private val save = Button(getTranslation("common.save"))
    private val cancel = Button(getTranslation("common.cancel"))

    private val binder = BeanValidationBinder(CyclicBooking::class.java)

    private var currentCyclicBooking = cyclicBooking ?: CyclicBooking(
        startDate = LocalDate.now(),
        recurrencePattern = RecurrencePattern.WEEKLY,
        startTime = LocalTime.of(9, 0),
        endTime = LocalTime.of(10, 0),
        user = User(email = "", password = "", firstName = "", lastName = ""),
        service = Service(
            name = "",
            duration = 60,
            price = BigDecimal("0.00"),
            provider = null
        )
    )

    init {
        addClassName("cyclic-booking-form")

        configureFields()

        binder.bindInstanceFields(this)

        // If we have a selected service, set it
        if (currentCyclicBooking.service.id != 0L) {
            service.value = availableServices.find { it.id == currentCyclicBooking.service.id }
        }

        add(
            service,
            recurrencePattern,
            dayOfWeek,
            dayOfMonth,
            startDate,
            endDate,
            startTime,
            endTime,
            notes,
            createButtonsLayout()
        )

        updateRecurrenceFields(currentCyclicBooking.recurrencePattern)
    }

    private fun configureFields() {
        // Set up available services
        service.setItems(availableServices)
        service.setItemLabelGenerator { it.name }
        service.isRequired = true

        // Set up date pickers
        startDate.min = LocalDate.now()
        startDate.isRequired = true

        endDate.min = LocalDate.now().plusWeeks(1)
        endDate.helperText = getTranslation("cyclic.booking.end.date.helper")

        // Set up recurrence pattern selector
        recurrencePattern.setItems(RecurrencePattern.entries.toList())
        recurrencePattern.setItemLabelGenerator {
            when (it) {
                RecurrencePattern.WEEKLY -> getTranslation("cyclic.booking.weekly")
                RecurrencePattern.MONTHLY -> getTranslation("cyclic.booking.monthly")
                null -> ""
            }
        }
        recurrencePattern.isRequired = true

        // Configure day of week options
        dayOfWeek.setItems(DayOfWeek.entries.toList())
        dayOfWeek.setItemLabelGenerator {
            it.getDisplayName(TextStyle.FULL, Locale.getDefault())
        }

        // Configure day of month options
        dayOfMonth.setItems((1..31).toList())

        // Hide appropriate fields based on recurrence pattern
        recurrencePattern.addValueChangeListener {
            updateRecurrenceFields(it.value)
        }

        // Time configuration
        startTime.step = Duration.ofMinutes(15)
        startTime.isRequired = true

        endTime.step = Duration.ofMinutes(15)
        endTime.isRequired = true

        // Update end time when start time or service changes
        startTime.addValueChangeListener { updateEndTime() }
        service.addValueChangeListener { updateEndTime() }

        // Notes field
        notes.placeholder = getTranslation("booking.notes.placeholder")
        notes.maxLength = 500

        configureBinder()
    }

    private fun updateRecurrenceFields(pattern: RecurrencePattern?) {
        when (pattern) {
            RecurrencePattern.WEEKLY -> {
                dayOfWeek.isVisible = true
                dayOfMonth.isVisible = false
            }

            RecurrencePattern.MONTHLY -> {
                dayOfWeek.isVisible = false
                dayOfMonth.isVisible = true
            }

            null -> {
                dayOfWeek.isVisible = false
                dayOfMonth.isVisible = false
            }
        }
    }

    private fun updateEndTime() {
        val start = startTime.value
        if (start != null && service.value != null) {
            // Set end time based on service duration
            endTime.value = start.plusMinutes(service.value!!.duration.toLong())
        }
    }

    private fun configureBinder() {
        binder.forField(service)
            .asRequired(getTranslation("validation.required"))
            .bind({ it.service }, { obj, value -> obj.service = value })

        binder.forField(startDate)
            .asRequired(getTranslation("validation.required"))
            .bind(CyclicBooking::startDate, CyclicBooking::startDate::set)

        binder.forField(endDate)
            .withValidator({ value, context ->
                if (value == null || startDate.value == null || value.isAfter(startDate.value)) {
                    ValidationResult.ok()
                } else {
                    ValidationResult.error(getTranslation("validation.end.date.after.start"))
                }
            })
            .bind(CyclicBooking::endDate, CyclicBooking::endDate::set)

        binder.forField(recurrencePattern)
            .asRequired(getTranslation("validation.required"))
            .bind(CyclicBooking::recurrencePattern, CyclicBooking::recurrencePattern::set)

        binder.forField(dayOfWeek)
            .withValidator({ value, context ->
                if (recurrencePattern.value != RecurrencePattern.WEEKLY || value != null) {
                    ValidationResult.ok()
                } else {
                    ValidationResult.error(getTranslation("validation.day.of.week.required"))
                }
            })
            .bind(CyclicBooking::dayOfWeek, CyclicBooking::dayOfWeek::set)

        binder.forField(dayOfMonth)
            .withValidator({ value, context ->
                if (recurrencePattern.value != RecurrencePattern.MONTHLY || value != null) {
                    ValidationResult.ok()
                } else {
                    ValidationResult.error(getTranslation("validation.day.of.month.required"))
                }
            })
            .bind(CyclicBooking::dayOfMonth, CyclicBooking::dayOfMonth::set)

        binder.forField(startTime)
            .asRequired(getTranslation("validation.required"))
            .bind(CyclicBooking::startTime, CyclicBooking::startTime::set)

        binder.forField(endTime)
            .asRequired(getTranslation("validation.required"))
            .withValidator({ value, context ->
                if (value != null && startTime.value != null && value.isAfter(startTime.value)) {
                    ValidationResult.ok()
                } else {
                    ValidationResult.error(getTranslation("validation.end.time.after.start"))
                }
            })
            .bind(CyclicBooking::endTime, CyclicBooking::endTime::set)

        binder.forField(notes)
            .bind(CyclicBooking::notes, CyclicBooking::notes::set)
    }

    private fun createButtonsLayout(): Component {
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY)
        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY)

        save.addClickShortcut(Key.ENTER)
        cancel.addClickShortcut(Key.ESCAPE)

        save.addClickListener { validateAndSave() }
        cancel.addClickListener { fireEvent(CancelEvent(this)) }

        return HorizontalLayout(save, cancel)
    }

    private fun validateAndSave() {
        if (binder.validate().isOk) {
            try {
                binder.writeBean(currentCyclicBooking)
                fireEvent(SaveEvent(this, currentCyclicBooking))
            } catch (e: Exception) {
                // Show validation error
                com.vaadin.flow.component.notification.Notification
                    .show(e.message)
                    .addThemeVariants(com.vaadin.flow.component.notification.NotificationVariant.LUMO_ERROR)
            }
        }
    }

    // Events
    abstract class CyclicBookingFormEvent(source: CyclicBookingForm, val cyclicBooking: CyclicBooking) :
        ComponentEvent<CyclicBookingForm>(source, false)

    class SaveEvent(source: CyclicBookingForm, cyclicBooking: CyclicBooking) :
        CyclicBookingFormEvent(source, cyclicBooking)

    class CancelEvent(source: CyclicBookingForm) : ComponentEvent<CyclicBookingForm>(source, false)

    // Event registration
    fun addSaveListener(listener: ComponentEventListener<SaveEvent>): Registration {
        return addListener(SaveEvent::class.java, listener)
    }

    fun addCancelListener(listener: ComponentEventListener<CancelEvent>): Registration {
        return addListener(CancelEvent::class.java, listener)
    }
}