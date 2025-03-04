package online.kimino.micro.booking.ui.provider

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.ComponentEvent
import com.vaadin.flow.component.ComponentEventListener
import com.vaadin.flow.component.Key
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.combobox.ComboBox
import com.vaadin.flow.component.formlayout.FormLayout
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.textfield.BigDecimalField
import com.vaadin.flow.component.textfield.TextArea
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.binder.BeanValidationBinder
import com.vaadin.flow.data.validator.BigDecimalRangeValidator
import com.vaadin.flow.shared.Registration
import online.kimino.micro.booking.entity.Service
import java.math.BigDecimal

class ServiceForm(service: Service?) : FormLayout() {

    private val name = TextField("Service Name")
    private val description = TextArea("Description")
    private val duration = ComboBox<Int>("Duration (minutes)")
    private val price = BigDecimalField("Price")
    private val active = Checkbox("Active")

    private val save = Button("Save")
    private val cancel = Button("Cancel")
    private val delete = Button("Delete")

    private val binder = BeanValidationBinder(Service::class.java)
    private var currentService = service ?: Service(
        name = "",
        duration = 60,
        price = BigDecimal("0.00"),
        active = true,
        provider = null  // Will be set when saving
    )

    init {
        addClassName("service-form")

        configureFields()

        binder.bindInstanceFields(this)
        binder.readBean(currentService)

        add(
            name,
            description,
            duration,
            price,
            active,
            createButtonsLayout()
        )
    }

    private fun configureFields() {
        name.isRequired = true
        name.minLength = 2

        description.maxLength = 1000

        duration.setItems(15, 30, 45, 60, 90, 120, 180, 240)
        duration.setItemLabelGenerator { "$it minutes" }
        duration.isRequired = true

//        price.min = 0
//        price.hasControls = true
//        price.step = 1
        price.isRequired = true

        configureBinder()
    }

    private fun configureBinder() {
        binder.forField(name)
            .asRequired("Service name is required")
            .withValidator({ it.length >= 2 }, "Name must be at least 2 characters")
            .bind(Service::name, Service::name::set)

        binder.forField(description)
            .bind(Service::description, Service::description::set)

        binder.forField(duration)
            .asRequired("Duration is required")
            .bind(Service::duration, Service::duration::set)

        binder.forField(price)
            .asRequired("Price is required")
            .withValidator(BigDecimalRangeValidator("Price must be at least 0", BigDecimal.ZERO, null))
            .bind(Service::price, Service::price::set)

        binder.forField(active)
            .bind(Service::active, Service::active::set)
    }

    private fun createButtonsLayout(): Component {
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY)
        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY)
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY)

        save.addClickShortcut(Key.ENTER)
        cancel.addClickShortcut(Key.ESCAPE)

        save.addClickListener { validateAndSave() }
        cancel.addClickListener { fireEvent(CancelEvent(this)) }
        delete.addClickListener { delete() }

        // Only show delete button for existing services
        delete.isVisible = currentService.id != 0L

        return HorizontalLayout(save, cancel, delete)
    }

    private fun validateAndSave() {
        if (binder.validate().isOk) {
            binder.writeBean(currentService)
            fireEvent(SaveEvent(this, currentService))
        }
    }

    private fun delete() {
        fireEvent(DeleteEvent(this, currentService))
    }

    // Events
    abstract class ServiceFormEvent(source: ServiceForm, val service: Service) :
        ComponentEvent<ServiceForm>(source, false)

    class SaveEvent(source: ServiceForm, service: Service) : ServiceFormEvent(source, service)

    class DeleteEvent(source: ServiceForm, service: Service) : ServiceFormEvent(source, service)

    class CancelEvent(source: ServiceForm) : ComponentEvent<ServiceForm>(source, false)

    // Event registration
    fun addSaveListener(listener: ComponentEventListener<SaveEvent>): Registration {
        return addListener(SaveEvent::class.java, listener)
    }

    fun addDeleteListener(listener: ComponentEventListener<DeleteEvent>): Registration {
        return addListener(DeleteEvent::class.java, listener)
    }

    fun addCancelListener(listener: ComponentEventListener<CancelEvent>): Registration {
        return addListener(CancelEvent::class.java, listener)
    }
}