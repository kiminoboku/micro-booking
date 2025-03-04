package online.kimino.micro.booking.repository

import online.kimino.micro.booking.entity.User
import online.kimino.micro.booking.entity.VerificationToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface VerificationTokenRepository : JpaRepository<VerificationToken, Long> {
    fun findByToken(token: String): Optional<VerificationToken>
    fun findByUser(user: User): Optional<VerificationToken>
    fun deleteByUser(user: User)
}