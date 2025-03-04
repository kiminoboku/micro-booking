package online.kimino.micro.booking.config

import com.vaadin.flow.component.page.AppShellConfigurator
import com.vaadin.flow.component.page.Push
import com.vaadin.flow.server.AppShellSettings
import com.vaadin.flow.theme.Theme
import com.vaadin.flow.theme.lumo.Lumo
import online.kimino.micro.booking.exception.GlobalExceptionHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@Theme(value = "bookingsaas", variant = Lumo.DARK)
@Push
class VaadinConfiguration : AppShellConfigurator {

    override fun configurePage(settings: AppShellSettings) {
        settings.addFavIcon("icon", "icons/favicon.ico", "16x16")
        settings.addLink("shortcut icon", "icons/favicon.ico")
//        settings.addManifestLink("icons/manifest.json")

        // Set application metadata
        settings.setPageTitle("Online Booking SaaS")
        settings.addMetaTag("description", "A professional online booking solution for service providers")
        settings.addMetaTag("viewport", "width=device-width, initial-scale=1.0")
    }

    @Bean
    fun errorHandler(): GlobalExceptionHandler {
        return GlobalExceptionHandler()
    }
}