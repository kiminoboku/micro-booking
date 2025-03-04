package online.kimino.micro.booking.service

import online.kimino.micro.booking.entity.Booking
import online.kimino.micro.booking.entity.User
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter

@Service
class EmailService(
    private val mailSender: JavaMailSender
) {
    @Value("\${spring.mail.username}")
    private lateinit var fromEmail: String

    @Value("\${server.port}")
    private lateinit var serverPort: String

    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    @Async
    fun sendVerificationEmail(user: User, token: String) {
        val subject = "Please verify your email"
        val verificationUrl = "http://localhost:$serverPort/verify?token=$token"

        val content = """
            <html>
                <body>
                    <h2>Hello, ${user.firstName}!</h2>
                    <p>Please click the link below to verify your email address:</p>
                    <p><a href="$verificationUrl">Verify Email</a></p>
                    <p>This link will expire in 24 hours.</p>
                    <p>Thank you,<br/>The Booking SaaS Team</p>
                </body>
            </html>
        """.trimIndent()

        sendEmail(user.email, subject, content)
    }

    @Async
    fun sendPasswordResetEmail(user: User, token: String) {
        val subject = "Reset your password"
        val resetUrl = "http://localhost:$serverPort/reset-password?token=$token"

        val content = """
            <html>
                <body>
                    <h2>Hello, ${user.firstName}!</h2>
                    <p>Please click the link below to reset your password:</p>
                    <p><a href="$resetUrl">Reset Password</a></p>
                    <p>This link will expire in 24 hours.</p>
                    <p>If you did not request a password reset, please ignore this email.</p>
                    <p>Thank you,<br/>The Booking SaaS Team</p>
                </body>
            </html>
        """.trimIndent()

        sendEmail(user.email, subject, content)
    }

    @Async
    fun sendBookingConfirmationEmail(booking: Booking) {
        val user = booking.user
        val service = booking.service
        val provider = service.provider

        val subject = "Booking Confirmation - ${service.name}"

        val startTime = booking.startTime.format(dateTimeFormatter)
        val endTime = booking.endTime.format(dateTimeFormatter)

        val content = """
            <html>
                <body>
                    <h2>Hello, ${user.firstName}!</h2>
                    <p>Your booking has been confirmed:</p>
                    <ul>
                        <li><strong>Service:</strong> ${service.name}</li>
                        <li><strong>Provider:</strong> ${provider!!.fullName()}</li>
                        <li><strong>Date/Time:</strong> $startTime to $endTime</li>
                        <li><strong>Duration:</strong> ${service.duration} minutes</li>
                        <li><strong>Price:</strong> $${service.price}</li>
                    </ul>
                    <p>If you need to cancel or reschedule, please log in to your account.</p>
                    <p>Thank you,<br/>The Booking SaaS Team</p>
                </body>
            </html>
        """.trimIndent()

        sendEmail(user.email, subject, content)
    }

    @Async
    fun sendBookingCancellationEmail(booking: Booking) {
        val user = booking.user
        val service = booking.service

        val subject = "Booking Cancelled - ${service.name}"

        val startTime = booking.startTime.format(dateTimeFormatter)

        val content = """
            <html>
                <body>
                    <h2>Hello, ${user.firstName}!</h2>
                    <p>Your booking for ${service.name} on $startTime has been cancelled.</p>
                    <p>If you would like to make a new booking, please log in to your account.</p>
                    <p>Thank you,<br/>The Booking SaaS Team</p>
                </body>
            </html>
        """.trimIndent()

        sendEmail(user.email, subject, content)
    }

    @Async
    fun sendBookingReminderEmail(booking: Booking) {
        val user = booking.user
        val service = booking.service
        val provider = service.provider

        val subject = "Reminder: Upcoming Booking - ${service.name}"

        val startTime = booking.startTime.format(dateTimeFormatter)

        val content = """
            <html>
                <body>
                    <h2>Hello, ${user.firstName}!</h2>
                    <p>This is a reminder of your upcoming booking:</p>
                    <ul>
                        <li><strong>Service:</strong> ${service.name}</li>
                        <li><strong>Provider:</strong> ${provider!!.fullName()}</li>
                        <li><strong>Date/Time:</strong> $startTime</li>
                    </ul>
                    <p>If you need to cancel or reschedule, please log in to your account immediately.</p>
                    <p>Thank you,<br/>The Booking SaaS Team</p>
                </body>
            </html>
        """.trimIndent()

        sendEmail(user.email, subject, content)
    }

    private fun sendEmail(to: String, subject: String, content: String) {
        val message = mailSender.createMimeMessage()
        val helper = MimeMessageHelper(message, true, "UTF-8")

        helper.setFrom(fromEmail)
        helper.setTo(to)
        helper.setSubject(subject)
        helper.setText(content, true)

        mailSender.send(message)
    }
}