package online.kimino.micro.booking.config

import com.vaadin.flow.spring.security.VaadinWebSecurity
import online.kimino.micro.booking.ui.auth.LoginView
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.util.matcher.AntPathRequestMatcher

@Configuration
@EnableWebSecurity
class SecurityConfig : VaadinWebSecurity() {

    override fun configure(http: HttpSecurity?) {
        http!!
            .csrf { csrf ->
                csrf.disable()
            }
            .authorizeHttpRequests { authorize ->
                authorize
                    .requestMatchers(
                        "/VAADIN/**",
                        "/favicon.ico",
                        "/robots.txt",
                        "/manifest.webmanifest",
                        "/sw.js",
                        "/offline.html",
                        "/icons/**",
                        "/images/**",
                        "/styles/**",
                        "/frontend/**",
                        "/frontend-es5/**",
                        "/frontend-es6/**",
                        "/register",
                        "/login",
                        "/verify",
                        "/reset-password",
                        "/forgot-password"
                    ).permitAll()
                    .requestMatchers("/admin/**").hasRole("ADMIN")
                    .requestMatchers("/provider/**").hasRole("PROVIDER")
//                    .anyRequest().authenticated()
            }
            .formLogin { form ->
                form
                    .loginPage("/login")
                    .defaultSuccessUrl("/")
                    .permitAll()
            }
            .logout { logout ->
                logout
                    .logoutRequestMatcher(AntPathRequestMatcher("/logout"))
                    .logoutSuccessUrl("/login")
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID")
                    .permitAll()
            }

        super.configure(http)
        setLoginView(http, LoginView::class.java)
    }

//    @Bean
//    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
//
//    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }
}