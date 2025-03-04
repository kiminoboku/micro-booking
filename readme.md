# Kimino Booking: Online Booking SaaS Platform

Kimino Booking is a flexible, easy-to-use online booking system built with Spring Boot, Kotlin, Vaadin, and PostgreSQL. The application allows service providers to manage their services, schedules, and bookings while providing customers with a seamless booking experience.

## Features

### For Service Providers
- Create and manage service offerings with customizable details (name, description, duration, color)
- Set regular weekly schedules for each service
- Add exceptions (days off, holidays, or special hours)
- View, confirm, complete, or cancel bookings
- Comprehensive calendar views of all bookings
- Email notifications for new bookings, confirmations, and cancellations

### For Customers
- Browse available services
- Book appointments with real-time availability checking
- Receive email confirmations and reminders
- Manage existing bookings (view, reschedule, cancel)

## Technology Stack

- **Backend**: Spring Boot 3.4.3, Kotlin 1.9.25, Java 21
- **Frontend**: Vaadin 24.3.4 (Vaadin Flow)
- **Database**: PostgreSQL
- **Build Tool**: Maven
- **Security**: Spring Security (Form-based authentication)
- **Email**: Spring Mail for notifications
- **Architecture**: Monolithic application with layered architecture

## Project Structure

```
src/main/
├── kotlin/online/kimino/micro/booking/
│   ├── config/ - Application configuration
│   ├── domain/model/ - Domain entities
│   ├── repository/ - Data access layer
│   ├── service/ - Business logic layer
│   ├── ui/ - Vaadin UI components
│   │   ├── layout/ - Layout components
│   │   └── views/ - View components
│   └── web/ - REST API controllers and DTOs
├── resources/
│   ├── application.properties - Application configuration
│   ├── db/migration/ - Flyway database migrations
│   └── META-INF/
└── webapp/
    └── frontend/ - Frontend resources
```

## Getting Started

### Prerequisites

- JDK 21
- Maven 3.8+
- PostgreSQL 14+

### Setup

1. Clone the repository
   ```
   git clone https://github.com/yourusername/kimino-booking.git
   cd kimino-booking
   ```

2. Configure database settings in `application.properties`
   ```
   spring.datasource.url=jdbc:postgresql://localhost:5432/booking_db
   spring.datasource.username=booking_user
   spring.datasource.password=booking_password
   ```

3. Build the application
   ```
   mvn clean install
   ```

4. Run the application
   ```
   mvn spring-boot:run
   ```

5. Access the application
   ```
   http://localhost:8080
   ```

### Default Users

The system comes with three predefined users for testing:

1. Admin User
   - Email: admin@kimino.online
   - Password: admin123
   - Role: ADMIN, USER

2. Service Provider
   - Email: provider@kimino.online
   - Password: provider123
   - Role: SERVICE_PROVIDER, USER

3. Customer
   - Email: customer@example.com
   - Password: customer123
   - Role: USER

## API Documentation

The application provides a RESTful API for integrating with other systems.

### Main API Endpoints:

- `/api/services` - Manage services
- `/api/schedules` - Manage schedules and availability
- `/api/bookings` - Manage bookings
- `/api/availability` - Check availability of services

Full API documentation is available at `/swagger-ui.html` when running the application.

## Customization

### Appearance

The application uses the Lumo theme from Vaadin, which can be customized in the `src/main/webapp/frontend/themes/booking` directory.

### Email Templates

Email templates can be found in `src/main/resources/templates/emails`.

### Application Settings

Various application settings can be configured in `application.properties`, including:

- `app.booking.max-future-days` - Maximum days in advance a booking can be made
- `app.booking.min-duration-minutes` - Minimum duration for a booking
- `app.booking.default-timezone` - Default timezone for the application

## Development

### Build and Test

```
mvn clean test
```

### Run in Development Mode

```
mvn spring-boot:run -Pdev
```

### Production Build

```
mvn clean package -Pproduction
```

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- The Spring Boot and Vaadin teams for their excellent frameworks
- All contributors to this project

---

For more information, please contact team@kimino.online
