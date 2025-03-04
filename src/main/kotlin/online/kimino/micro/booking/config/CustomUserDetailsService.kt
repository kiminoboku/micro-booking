package online.kimino.micro.booking.security

import online.kimino.micro.booking.repository.UserRepository
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class CustomUserDetailsService(
    private val userRepository: UserRepository
) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails {
        val user = userRepository.findByEmail(username)
            .orElseThrow { UsernameNotFoundException("User not found with email: $username") }

        // Create authorities based on user role
        val authorities = mutableListOf(SimpleGrantedAuthority("ROLE_${user.role}"))

        return User.builder()
            .username(user.email)
            .password(user.password)
            .authorities(authorities)
            .disabled(!user.enabled)
            .accountExpired(false)
            .accountLocked(false)
            .credentialsExpired(false)
            .build()
    }
}