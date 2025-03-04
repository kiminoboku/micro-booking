package online.kimino.micro.booking.ui.provider

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.ComponentEvent
import com.vaadin.flow.component.ComponentEventListener
import com.vaadin.flow.component.Key
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.combobox.ComboBox
import com.vaadin.flow.component.formlayout.FormLayout
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.timepicker.TimePicker
import com.vaadin.flow.data.binder.BeanValidationBinder
import com.vaadin.flow.data.binder.ValidationResult
import com.vaadin.flow.shared.Registration
import online.kimino.micro.booking.entity.Availability
import online.kimino.micro.booking.entity.Service
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.*

class AvailabilityForm(availability: Availability?, private val service: Service) : FormLayout() {

    private val dayOfWeek = ComboBox<DayOfWeek>("Day of Week")
    private val startTime = TimePicker("Start Time")
    private val endTime = TimePicker("End Time")

    private val save = Button("Save")
    private val cancel = Button("Cancel")

    private val binder = BeanValidationBinder(Availability::class.java)
    private var currentAvailability = availability ?: Availability(
        dayOfWeek = DayOfWeek.MONDAY,
        startTime = LocalTime.of(9, 0),
        endTime = LocalTime.of(17, 0),
        service = service
    )

    init {
        addClassName("availability-form")

        configureFields()

        binder.bindInstanceFields(this)
        binder.readBean(currentAvailability)

        add(
            dayOfWeek,
            startTime,
            endTime,
            createButtonsLayout()
        )
    }

    private fun configureFields() {
        dayOfWeek.setItems(DayOfWeek.entries.toList())
        dayOfWeek.setItemLabelGenerator {
            it.getDisplayName(TextStyle.FULL, Locale.getDefault())
        }
        dayOfWeek.isRequired = true

        startTime.isRequired = true
        startTime.step = Duration.ofMinutes(15)
        startTime.isAutoOpen = false

        endTime.isRequired = true
        endTime.step = Duration.ofMinutes(15)
        endTime.isAutoOpen = false

        startTime.addValueChangeListener { updateEndTimeMin() }

        configureBinder()
    }

    private fun updateEndTimeMin() {
        val start = startTime.value
        if (start != null) {
            // Set minimum end time to start time + 15 minutes
            endTime.min = start.plusMinutes(15)

            // If current end time is null or before new minimum, update it
            if (endTime.value == null || endTime.value.isBefore(endTime.min)) {
                endTime.value = endTime.min
            }
        }
    }

    private fun configureBinder() {
        binder.forField(dayOfWeek)
            .asRequired("Day of week is required")
            .bind(Availability::dayOfWeek, Availability::dayOfWeek::set)

        binder.forField(startTime)
            .asRequired("Start time is required")
            .bind(Availability::startTime, Availability::startTime::set)

        binder.forField(endTime)
            .asRequired("End time is required")
            .withValidator({ value, context ->
                if (value != null && value.isAfter(startTime.value))
                    ValidationResult.ok()
                else
                    ValidationResult.error("End time must be after start time")
            })
            .bind(Availability::endTime, Availability::endTime::set)
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
            // Additional validation to ensure end time is after start time
            if (startTime.value != null && endTime.value != null) {
                if (!endTime.value.isAfter(startTime.value)) {
                    Notification.show("End time must be after start time")
                    return
                }
            }

            binder.writeBean(currentAvailability)
            fireEvent(SaveEvent(this, currentAvailability))
        }
    }

    // Events
    abstract class AvailabilityFormEvent(source: AvailabilityForm, val availability: Availability) :
        ComponentEvent<AvailabilityForm>(source, false)

    class SaveEvent(source: AvailabilityForm, availability: Availability) : AvailabilityFormEvent(source, availability)

    class CancelEvent(source: AvailabilityForm) : ComponentEvent<AvailabilityForm>(source, false)

    // Event registration
    fun addSaveListener(listener: ComponentEventListener<SaveEvent>): Registration {
        return addListener(SaveEvent::class.java, listener)
    }

    fun addCancelListener(listener: ComponentEventListener<CancelEvent>): Registration {
        return addListener(CancelEvent::class.java, listener)
    }
}