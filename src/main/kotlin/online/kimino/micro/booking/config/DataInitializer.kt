package online.kimino.micro.booking.config

import online.kimino.micro.booking.entity.*
import online.kimino.micro.booking.repository.AvailabilityRepository
import online.kimino.micro.booking.repository.BookingRepository
import online.kimino.micro.booking.repository.ServiceRepository
import online.kimino.micro.booking.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.crypto.password.PasswordEncoder
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

@Configuration
class DataInitializer {

    private val logger = LoggerFactory.getLogger(DataInitializer::class.java)

    @Bean
    @Profile("dev", "test")  // Only run in development and test environments
    fun initData(
        userRepository: UserRepository,
        serviceRepository: ServiceRepository,
        availabilityRepository: AvailabilityRepository,
        bookingRepository: BookingRepository,
        passwordEncoder: PasswordEncoder
    ): CommandLineRunner {
        return CommandLineRunner {
            // Only initialize if the database is empty
            if (userRepository.count() > 0) {
                logger.info("Database already contains data, skipping initialization")
                return@CommandLineRunner
            }

            logger.info("Initializing database with sample data")

            // Create admin user
            val adminUser = User(
                email = "admin@example.com",
                password = passwordEncoder.encode("password"),
                firstName = "Admin",
                lastName = "User",
                phoneNumber = "123-456-7890",
                role = UserRole.ADMIN,
                enabled = true
            )
            userRepository.save(adminUser)

            // Create provider users
            val provider1 = User(
                email = "provider1@example.com",
                password = passwordEncoder.encode("password"),
                firstName = "John",
                lastName = "Doe",
                phoneNumber = "123-456-7891",
                role = UserRole.PROVIDER,
                enabled = true
            )
            userRepository.save(provider1)

            val provider2 = User(
                email = "provider2@example.com",
                password = passwordEncoder.encode("password"),
                firstName = "Jane",
                lastName = "Smith",
                phoneNumber = "123-456-7892",
                role = UserRole.PROVIDER,
                enabled = true
            )
            userRepository.save(provider2)

            // Create regular users
            val user1 = User(
                email = "user1@example.com",
                password = passwordEncoder.encode("password"),
                firstName = "Alice",
                lastName = "Johnson",
                phoneNumber = "123-456-7893",
                role = UserRole.USER,
                enabled = true
            )
            userRepository.save(user1)

            val user2 = User(
                email = "user2@example.com",
                password = passwordEncoder.encode("password"),
                firstName = "Bob",
                lastName = "Brown",
                phoneNumber = "123-456-7894",
                role = UserRole.USER,
                enabled = true
            )
            userRepository.save(user2)

            // Create services for provider1
            val haircut = Service(
                name = "Haircut",
                description = "Standard haircut service including wash and style",
                duration = 60,
                price = BigDecimal("50.00"),
                active = true,
                provider = provider1
            )
            serviceRepository.save(haircut)

            val coloring = Service(
                name = "Hair Coloring",
                description = "Professional hair coloring service",
                duration = 120,
                price = BigDecimal("120.00"),
                active = true,
                provider = provider1
            )
            serviceRepository.save(coloring)

            // Create services for provider2
            val massage = Service(
                name = "Massage Therapy",
                description = "Relaxing full body massage",
                duration = 90,
                price = BigDecimal("85.00"),
                active = true,
                provider = provider2
            )
            serviceRepository.save(massage)

            val facial = Service(
                name = "Facial Treatment",
                description = "Rejuvenating facial treatment",
                duration = 60,
                price = BigDecimal("65.00"),
                active = true,
                provider = provider2
            )
            serviceRepository.save(facial)

            // Create availabilities for provider1's services
            // Haircut availabilities
            addAvailabilitiesForWeekdays(availabilityRepository, haircut)

            // Coloring availabilities (fewer slots)
            val coloringAvailabilities = listOf(
                Availability(
                    dayOfWeek = DayOfWeek.TUESDAY,
                    startTime = LocalTime.of(10, 0),
                    endTime = LocalTime.of(16, 0),
                    service = coloring
                ),
                Availability(
                    dayOfWeek = DayOfWeek.THURSDAY,
                    startTime = LocalTime.of(10, 0),
                    endTime = LocalTime.of(16, 0),
                    service = coloring
                ),
                Availability(
                    dayOfWeek = DayOfWeek.SATURDAY,
                    startTime = LocalTime.of(9, 0),
                    endTime = LocalTime.of(15, 0),
                    service = coloring
                )
            )
            availabilityRepository.saveAll(coloringAvailabilities)

            // Create availabilities for provider2's services
            // Massage availabilities
            addAvailabilitiesForWeekdays(availabilityRepository, massage)

            // Facial availabilities
            addAvailabilitiesForWeekdays(availabilityRepository, facial)

            // Create some sample bookings
            val now = LocalDateTime.now()

            // Past booking (completed)
            val pastBooking = Booking(
                startTime = now.minusDays(5).withHour(14).withMinute(0),
                endTime = now.minusDays(5).withHour(15).withMinute(0),
                status = BookingStatus.COMPLETED,
                notes = "Completed service",
                user = user1,
                service = haircut
            )
            bookingRepository.save(pastBooking)

            // Today's booking (confirmed)
            val todayBooking = Booking(
                startTime = now.withHour(16).withMinute(0),
                endTime = now.withHour(17).withMinute(0),
                status = BookingStatus.CONFIRMED,
                user = user2,
                service = facial
            )
            bookingRepository.save(todayBooking)

            // Future booking (confirmed)
            val futureBooking = Booking(
                startTime = now.plusDays(3).withHour(10).withMinute(0),
                endTime = now.plusDays(3).withHour(11).withMinute(0),
                status = BookingStatus.CONFIRMED,
                user = user1,
                service = massage
            )
            bookingRepository.save(futureBooking)

            // Pending booking
            val pendingBooking = Booking(
                startTime = now.plusDays(7).withHour(13).withMinute(0),
                endTime = now.plusDays(7).withHour(15).withMinute(0),
                status = BookingStatus.PENDING,
                user = user2,
                service = coloring
            )
            bookingRepository.save(pendingBooking)

            // Cancelled booking
            val cancelledBooking = Booking(
                startTime = now.plusDays(2).withHour(9).withMinute(0),
                endTime = now.plusDays(2).withHour(10).withMinute(0),
                status = BookingStatus.CANCELLED,
                notes = "Customer cancelled",
                user = user1,
                service = haircut
            )
            bookingRepository.save(cancelledBooking)

            logger.info("Database initialization completed")
        }
    }

    private fun addAvailabilitiesForWeekdays(
        availabilityRepository: AvailabilityRepository,
        service: Service
    ) {
        val weekdayAvailabilities = listOf(
            Availability(
                dayOfWeek = DayOfWeek.MONDAY,
                startTime = LocalTime.of(9, 0),
                endTime = LocalTime.of(17, 0),
                service = service
            ),
            Availability(
                dayOfWeek = DayOfWeek.TUESDAY,
                startTime = LocalTime.of(9, 0),
                endTime = LocalTime.of(17, 0),
                service = service
            ),
            Availability(
                dayOfWeek = DayOfWeek.WEDNESDAY,
                startTime = LocalTime.of(9, 0),
                endTime = LocalTime.of(17, 0),
                service = service
            ),
            Availability(
                dayOfWeek = DayOfWeek.THURSDAY,
                startTime = LocalTime.of(9, 0),
                endTime = LocalTime.of(17, 0),
                service = service
            ),
            Availability(
                dayOfWeek = DayOfWeek.FRIDAY,
                startTime = LocalTime.of(9, 0),
                endTime = LocalTime.of(17, 0),
                service = service
            )
        )
        availabilityRepository.saveAll(weekdayAvailabilities)
    }
}