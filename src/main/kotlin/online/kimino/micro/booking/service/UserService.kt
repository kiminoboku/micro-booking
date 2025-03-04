package online.kimino.micro.booking.service

import jakarta.transaction.Transactional
import online.kimino.micro.booking.entity.User
import online.kimino.micro.booking.entity.UserRole
import online.kimino.micro.booking.entity.VerificationToken
import online.kimino.micro.booking.repository.UserRepository
import online.kimino.micro.booking.repository.VerificationTokenRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.util.*

@Service
class UserService(
    private val userRepository: UserRepository,
    private val verificationTokenRepository: VerificationTokenRepository,
    private val passwordEncoder: PasswordEncoder,
    private val emailService: EmailService
) {
    fun findById(id: Long): Optional<User> {
        return userRepository.findById(id)
    }

    fun findByEmail(email: String): Optional<User> {
        return userRepository.findByEmail(email)
    }

    fun getAllUsers(): List<User> {
        return userRepository.findAll()
    }

    fun getAllProviders(): List<User> {
        return userRepository.findAllByRole(UserRole.PROVIDER)
    }

    @Transactional
    fun createUser(user: User, role: UserRole = UserRole.USER): User {
        // Check if email already exists
        if (userRepository.existsByEmail(user.email)) {
            throw IllegalArgumentException("Email already exists")
        }

        // Encode password
        user.password = passwordEncoder.encode(user.password)
        user.role = role

        // Save user
        val savedUser = userRepository.save(user)

        // Create verification token
        val token = VerificationToken(user = savedUser)
        verificationTokenRepository.save(token)

        // Send verification email
        emailService.sendVerificationEmail(savedUser, token.token)

        return savedUser
    }

    @Transactional
    fun updateUser(user: User): User {
        return userRepository.save(user)
    }

    @Transactional
    fun deleteUser(id: Long) {
        userRepository.deleteById(id)
    }

    @Transactional
    fun verifyUser(token: String): Boolean {
        val verificationToken = verificationTokenRepository.findByToken(token)

        if (verificationToken.isPresent && !verificationToken.get().isExpired()) {
            val user = verificationToken.get().user
            user.enabled = true
            userRepository.save(user)
            verificationTokenRepository.delete(verificationToken.get())
            return true
        }

        return false
    }

    @Transactional
    fun changePassword(user: User, newPassword: String): User {
        user.password = passwordEncoder.encode(newPassword)
        return userRepository.save(user)
    }

    @Transactional
    fun initiatePasswordReset(email: String): Boolean {
        val optionalUser = userRepository.findByEmail(email)

        if (optionalUser.isPresent) {
            val user = optionalUser.get()

            // Delete existing tokens if any
            verificationTokenRepository.findByUser(user).ifPresent {
                verificationTokenRepository.delete(it)
            }

            // Create new token
            val token = VerificationToken(user = user)
            verificationTokenRepository.save(token)

            // Send reset email
            emailService.sendPasswordResetEmail(user, token.token)

            return true
        }

        return false
    }

    @Transactional
    fun resetPassword(token: String, newPassword: String): Boolean {
        val verificationToken = verificationTokenRepository.findByToken(token)

        if (verificationToken.isPresent && !verificationToken.get().isExpired()) {
            val user = verificationToken.get().user
            user.password = passwordEncoder.encode(newPassword)
            userRepository.save(user)
            verificationTokenRepository.delete(verificationToken.get())
            return true
        }

        return false
    }
}