package online.kimino.micro.booking.ui.provider

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.ComponentEvent
import com.vaadin.flow.component.ComponentEventListener
import com.vaadin.flow.component.Key
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.datetimepicker.DateTimePicker
import com.vaadin.flow.component.formlayout.FormLayout
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.textfield.TextArea
import com.vaadin.flow.data.binder.BeanValidationBinder
import com.vaadin.flow.data.binder.ValidationResult
import com.vaadin.flow.shared.Registration
import online.kimino.micro.booking.entity.ExceptionPeriod
import online.kimino.micro.booking.entity.User
import java.time.Duration
import java.time.LocalDateTime

class ExceptionPeriodForm(exceptionPeriod: ExceptionPeriod?, provider: User) : FormLayout() {

    private val startTime = DateTimePicker(getTranslation("availability.start.time"))
    private val endTime = DateTimePicker(getTranslation("availability.end.time"))
    private val description = TextArea(getTranslation("service.description"))

    private val save = Button(getTranslation("common.save"))
    private val cancel = Button(getTranslation("common.cancel"))

    private val binder = BeanValidationBinder(ExceptionPeriod::class.java)
    private var currentExceptionPeriod = exceptionPeriod ?: ExceptionPeriod(
        startTime = LocalDateTime.now(),
        endTime = LocalDateTime.now().plusHours(1),
        description = "",
        provider = provider
    )

    init {
        addClassName("exception-period-form")

        configureFields()

        binder.bindInstanceFields(this)
        binder.readBean(currentExceptionPeriod)

        add(
            startTime,
            endTime,
            description,
            createButtonsLayout()
        )
    }

    private fun configureFields() {
        startTime.isRequiredIndicatorVisible = true
        startTime.step = Duration.ofMinutes(15)
        startTime.min = LocalDateTime.now()

        endTime.isRequiredIndicatorVisible = true
        endTime.step = Duration.ofMinutes(15)
        endTime.min = LocalDateTime.now().plusMinutes(15)

        description.placeholder = getTranslation("booking.notes.placeholder")

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
        binder.forField(startTime)
            .asRequired(getTranslation("validation.required"))
            .bind(ExceptionPeriod::startTime, ExceptionPeriod::startTime::set)

        binder.forField(endTime)
            .asRequired(getTranslation("validation.required"))
            .withValidator({ value, context ->
                if (value != null && value.isAfter(startTime.value))
                    ValidationResult.ok()
                else
                    ValidationResult.error(getTranslation("validation.availability.end.after.start"))
            })
            .bind(ExceptionPeriod::endTime, ExceptionPeriod::endTime::set)

        binder.forField(description)
            .bind(ExceptionPeriod::description, ExceptionPeriod::description::set)
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
                    Notification.show(getTranslation("validation.availability.end.after.start"))
                    return
                }
            }

            binder.writeBean(currentExceptionPeriod)
            fireEvent(SaveEvent(this, currentExceptionPeriod))
        }
    }

    // Events
    abstract class ExceptionPeriodFormEvent(source: ExceptionPeriodForm, val exceptionPeriod: ExceptionPeriod) :
        ComponentEvent<ExceptionPeriodForm>(source, false)

    class SaveEvent(source: ExceptionPeriodForm, exceptionPeriod: ExceptionPeriod) :
        ExceptionPeriodFormEvent(source, exceptionPeriod)

    class CancelEvent(source: ExceptionPeriodForm) : ComponentEvent<ExceptionPeriodForm>(source, false)

    // Event registration
    fun addSaveListener(listener: ComponentEventListener<SaveEvent>): Registration {
        return addListener(SaveEvent::class.java, listener)
    }

    fun addCancelListener(listener: ComponentEventListener<CancelEvent>): Registration {
        return addListener(CancelEvent::class.java, listener)
    }
}