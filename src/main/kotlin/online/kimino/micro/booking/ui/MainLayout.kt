package online.kimino.micro.booking.ui

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.applayout.AppLayout
import com.vaadin.flow.component.applayout.DrawerToggle
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.H1
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.icon.Icon
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.RouterLink
import com.vaadin.flow.spring.security.AuthenticationContext
import online.kimino.micro.booking.entity.UserRole
import online.kimino.micro.booking.security.SecurityUtils
import online.kimino.micro.booking.ui.admin.AdminBookingsView
import online.kimino.micro.booking.ui.admin.AdminDashboardView
import online.kimino.micro.booking.ui.admin.AdminServicesView
import online.kimino.micro.booking.ui.admin.AdminUsersView
import online.kimino.micro.booking.ui.booking.BookingListView
import online.kimino.micro.booking.ui.booking.CreateBookingView
import online.kimino.micro.booking.ui.provider.ExceptionPeriodManagementView
import online.kimino.micro.booking.ui.provider.ProviderDashboardView
import online.kimino.micro.booking.ui.provider.ServiceManagementView
import online.kimino.micro.booking.ui.user.ProfileView

class MainLayout(val authenticationContext: AuthenticationContext) : AppLayout(), BeforeEnterObserver {

    init {
        createHeader()
        createDrawer()
    }

    private fun createHeader() {
        val logo = H1("Booking SaaS")
        logo.style.set("font-size", "var(--lumo-font-size-l)")
        logo.style.set("margin", "0")

        val logoutButton = Button("Logout") {
            authenticationContext.logout()
        }
        logoutButton.icon = Icon(VaadinIcon.SIGN_OUT)

        val header = HorizontalLayout(DrawerToggle(), logo, logoutButton)
        header.defaultVerticalComponentAlignment = FlexComponent.Alignment.CENTER
        header.expand(logo)
        header.setWidthFull()
        header.addClassNames("py-0", "px-m")

        addToNavbar(header)
    }

    private fun createDrawer() {
        val drawerLayout = VerticalLayout()
        drawerLayout.setSizeFull()
        drawerLayout.isPadding = false
        drawerLayout.isSpacing = false
        drawerLayout.className = "drawer-section"

        // Add navigation links based on user role
        if (SecurityUtils.isUserLoggedIn()) {
            when (SecurityUtils.getCurrentUserRole()) {
                UserRole.ADMIN -> {
                    addToDrawer(drawerLayout)
                    drawerLayout.add(
                        createNavigationItem(
                            "Admin Dashboard",
                            AdminDashboardView::class.java,
                            VaadinIcon.DASHBOARD
                        )
                    )
                    drawerLayout.add(createNavigationItem("Users", AdminUsersView::class.java, VaadinIcon.USERS))
                    drawerLayout.add(
                        createNavigationItem(
                            "Services",
                            AdminServicesView::class.java,
                            VaadinIcon.CALENDAR_BRIEFCASE
                        )
                    )
                    drawerLayout.add(
                        createNavigationItem(
                            "Bookings",
                            AdminBookingsView::class.java,
                            VaadinIcon.CALENDAR_CLOCK
                        )
                    )
                    drawerLayout.add(createNavigationItem("Profile", ProfileView::class.java, VaadinIcon.USER))
                }

                UserRole.PROVIDER -> {
                    addToDrawer(drawerLayout)
                    drawerLayout.add(
                        createNavigationItem(
                            "Provider Dashboard",
                            ProviderDashboardView::class.java,
                            VaadinIcon.DASHBOARD
                        )
                    )
                    drawerLayout.add(
                        createNavigationItem(
                            "My Services",
                            ServiceManagementView::class.java,
                            VaadinIcon.CALENDAR_BRIEFCASE
                        )
                    )
                    drawerLayout.add(
                        createNavigationItem(
                            "Exception Periods",
                            ExceptionPeriodManagementView::class.java,
                            VaadinIcon.EXCLAMATION
                        )
                    )
                    drawerLayout.add(
                        createNavigationItem(
                            "My Bookings",
                            BookingListView::class.java,
                            VaadinIcon.CALENDAR_CLOCK
                        )
                    )
                    drawerLayout.add(createNavigationItem("Profile", ProfileView::class.java, VaadinIcon.USER))
                }

                UserRole.USER -> {
                    addToDrawer(drawerLayout)
                    drawerLayout.add(
                        createNavigationItem(
                            "Dashboard",
                            DashboardView::class.java,
                            VaadinIcon.DASHBOARD
                        )
                    )
                    drawerLayout.add(
                        createNavigationItem(
                            "Book a Service",
                            CreateBookingView::class.java,
                            VaadinIcon.PLUS
                        )
                    )
                    drawerLayout.add(
                        createNavigationItem(
                            "My Bookings",
                            BookingListView::class.java,
                            VaadinIcon.CALENDAR_CLOCK
                        )
                    )
                    drawerLayout.add(createNavigationItem("Profile", ProfileView::class.java, VaadinIcon.USER))
                }

                else -> {
                    // No navigation links for unauthenticated users
                }
            }
        }
    }

    private fun <T : Component> createNavigationItem(
        text: String,
        navigationTarget: Class<T>,
        icon: VaadinIcon
    ): RouterLink {
        val link = RouterLink(navigationTarget)

        val iconElement = icon.create()
        iconElement.style.set("box-sizing", "border-box")
        iconElement.style.set("margin-inline-end", "var(--lumo-space-m)")
        iconElement.style.set("margin-inline-start", "var(--lumo-space-xs)")
        iconElement.style.set("padding", "var(--lumo-space-xs)")

        val viewName = Span(text)

        link.add(iconElement, viewName)
        link.addClassNames("flex", "mx-s", "p-s", "rounded-m")

        return link
    }

    override fun beforeEnter(event: BeforeEnterEvent) {
        // Redirect to role-specific dashboard if user is trying to access the root URL
        if (event.navigationTarget == DashboardView::class.java) {
            redirectToRoleDashboard(event)
        }
    }

    private fun redirectToRoleDashboard(event: BeforeEnterEvent) {
        if (SecurityUtils.isUserLoggedIn()) {
            when (SecurityUtils.getCurrentUserRole()) {
                UserRole.ADMIN -> event.forwardTo(AdminDashboardView::class.java)
                UserRole.PROVIDER -> event.forwardTo(ProviderDashboardView::class.java)
                UserRole.USER -> {} // No redirect for regular users, they stay on DashboardView
                else -> {} // No redirect if role is not determined
            }
        }
    }
}