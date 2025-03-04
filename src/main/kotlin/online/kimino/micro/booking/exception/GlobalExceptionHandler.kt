package online.kimino.micro.booking.exception

import com.vaadin.flow.server.ErrorEvent
import com.vaadin.flow.server.ErrorHandler
import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component
import java.lang.reflect.InvocationTargetException

@Component
class GlobalExceptionHandler : ErrorHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    override fun error(event: ErrorEvent) {
        var t = event.throwable

        // Unwrap InvocationTargetExceptions
        if (t is InvocationTargetException) {
            t = t.targetException
        }

        // Log the exception
        logException(t)

        // Show appropriate error messages for common exceptions
        when (t) {
            is DataIntegrityViolationException -> {
                // Database constraint violation
                showErrorNotification("Data integrity violation. Please check your input.")
            }

            is DataAccessException -> {
                // Other database-related exceptions
                showErrorNotification("Database error. Please try again later.")
            }

            is BookingException -> {
                // Our own custom booking exceptions
                showErrorNotification(t.message ?: "Error processing booking.")
            }

            is AccessDeniedException -> {
                // Access denied
                showErrorNotification("You don't have permission to perform this action.")
            }

            else -> {
                // Generic error message for other exceptions
                showErrorNotification("An unexpected error occurred. Please try again later.")
            }
        }
    }

    private fun logException(throwable: Throwable) {
        logger.error("Uncaught UI exception", throwable)
    }

    private fun showErrorNotification(message: String) {
        // Use Vaadin's UI.getCurrent() to show notification
        com.vaadin.flow.component.UI.getCurrent()?.access {
            com.vaadin.flow.component.notification.Notification.show(
                message,
                5000,
                com.vaadin.flow.component.notification.Notification.Position.MIDDLE
            ).apply {
                addThemeVariants(com.vaadin.flow.component.notification.NotificationVariant.LUMO_ERROR)
            }
        }
    }
}

class AccessDeniedException(message: String) : RuntimeException(message)