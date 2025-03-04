package online.kimino.micro.booking.security

import com.vaadin.flow.server.VaadinServletRequest
import online.kimino.micro.booking.entity.UserRole
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler

object SecurityUtils {
    /**
     * Check if user is logged in
     */
    fun isUserLoggedIn(): Boolean {
        val context = SecurityContextHolder.getContext()
        return context.authentication != null && context.authentication.isAuthenticated &&
                context.authentication.name != "anonymousUser"
    }

    /**
     * Get the current user's username/email
     */
    fun getCurrentUsername(): String? {
        return if (isUserLoggedIn()) {
            SecurityContextHolder.getContext().authentication.name
        } else {
            null
        }
    }

    /**
     * Get the current user's role
     */
    fun getCurrentUserRole(): UserRole? {
        if (!isUserLoggedIn()) {
            return null
        }

        val authentication = SecurityContextHolder.getContext().authentication
        val authorities = authentication.authorities

        return when {
            authorities.any { it.authority == "ROLE_ADMIN" } -> UserRole.ADMIN
            authorities.any { it.authority == "ROLE_PROVIDER" } -> UserRole.PROVIDER
            authorities.any { it.authority == "ROLE_USER" } -> UserRole.USER
            else -> null
        }
    }

    /**
     * Check if the current user has the specified role
     */
    fun hasRole(role: UserRole): Boolean {
        return getCurrentUserRole() == role
    }

    /**
     * Logout the current user
     */
    fun logout() {
        val request = VaadinServletRequest.getCurrent().httpServletRequest
        val logoutHandler = SecurityContextLogoutHandler()
        logoutHandler.logout(request, null, null)
        SecurityContextHolder.clearContext()
    }
}