package online.kimino.micro.booking.service

import jakarta.transaction.Transactional
import online.kimino.micro.booking.entity.Service
import online.kimino.micro.booking.entity.User
import online.kimino.micro.booking.repository.ServiceRepository
import java.util.*
import org.springframework.stereotype.Service as SpringService

@SpringService
class ServiceService(
    private val serviceRepository: ServiceRepository
) {
    fun findById(id: Long): Optional<Service> {
        return serviceRepository.findById(id)
    }

    fun findAll(): List<Service> {
        return serviceRepository.findAll()
    }

    fun findAllActive(): List<Service> {
        return serviceRepository.findAllActive()
    }

    fun findAllByProvider(providerId: Long): List<Service> {
        return serviceRepository.findAllByProviderId(providerId)
    }

    fun findAllActiveByProvider(provider: User): List<Service> {
        return serviceRepository.findAllByProviderAndActiveTrue(provider)
    }

    fun searchServices(keyword: String): List<Service> {
        return serviceRepository.searchByKeyword(keyword)
    }

    @Transactional
    fun createService(service: Service): Service {
        return serviceRepository.save(service)
    }

    @Transactional
    fun updateService(service: Service): Service {
        return serviceRepository.save(service)
    }

    @Transactional
    fun deleteService(id: Long) {
        serviceRepository.deleteById(id)
    }

    @Transactional
    fun toggleServiceStatus(id: Long): Service {
        val service = serviceRepository.findById(id)
            .orElseThrow { NoSuchElementException("Service not found with id $id") }

        service.active = !service.active
        return serviceRepository.save(service)
    }
}