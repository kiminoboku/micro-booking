package online.kimino.micro.booking.repository

import online.kimino.micro.booking.entity.Service
import online.kimino.micro.booking.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface ServiceRepository : JpaRepository<Service, Long> {
    fun findAllByProviderId(providerId: Long): List<Service>
    fun findAllByProviderAndActiveTrue(provider: User): List<Service>

    @Query("SELECT s FROM Service s WHERE s.active = true")
    fun findAllActive(): List<Service>

    @Query("SELECT s FROM Service s WHERE s.active = true AND LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    fun searchByKeyword(keyword: String): List<Service>
}