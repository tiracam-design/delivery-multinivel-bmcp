package com.delivery.modules.admin.service;

import com.delivery.domain.validation.EcuadorianRucValidator;
import com.delivery.infrastructure.adapter.output.persistence.entity.DriverEntity;
import com.delivery.infrastructure.adapter.output.persistence.entity.MerchantEntity;
import com.delivery.infrastructure.adapter.output.persistence.repository.DriverJpaRepository;
import com.delivery.infrastructure.adapter.output.persistence.repository.MerchantJpaRepository;
import com.delivery.modules.admin.dto.DriverApprovalResponseDTO;
import com.delivery.modules.admin.dto.MerchantApprovalResponseDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Servicio de Spring Boot para el Módulo de Administración:
 * Auditoría legal y fiscal de comercios y repartidores en Tulcán, Ecuador.
 *
 * <p>Integra validación criptográfica de RUCs con {@link EcuadorianRucValidator}.</p>
 */
@Service
public class MerchantApprovalsService {

    private final MerchantJpaRepository merchantJpaRepository;
    private final DriverJpaRepository driverJpaRepository;

    public MerchantApprovalsService(MerchantJpaRepository merchantJpaRepository,
                                    DriverJpaRepository driverJpaRepository) {
        this.merchantJpaRepository = merchantJpaRepository;
        this.driverJpaRepository = driverJpaRepository;
    }

    public List<MerchantApprovalResponseDTO> getPendingMerchants() {
        return merchantJpaRepository.findByActiveFalse().stream()
                .map(m -> new MerchantApprovalResponseDTO(
                        m.getId(),
                        m.getName(),
                        m.getAddress(),
                        m.getPhone(),
                        m.getEmail(),
                        m.getCategory(),
                        m.getRating(),
                        m.isActive(),
                        true, // Auditoría SRI activa
                        m.getCreatedAt()
                ))
                .toList();
    }

    @Transactional
    public Optional<MerchantApprovalResponseDTO> approveMerchant(UUID merchantId) {
        return merchantJpaRepository.findById(merchantId).map(m -> {
            m.setActive(true);
            merchantJpaRepository.save(m);
            return new MerchantApprovalResponseDTO(
                    m.getId(), m.getName(), m.getAddress(), m.getPhone(), m.getEmail(),
                    m.getCategory(), m.getRating(), m.isActive(), true, m.getCreatedAt()
            );
        });
    }

    @Transactional
    public Optional<MerchantApprovalResponseDTO> rejectMerchant(UUID merchantId, String reason) {
        return merchantJpaRepository.findById(merchantId).map(m -> {
            m.setActive(false);
            merchantJpaRepository.save(m);
            return new MerchantApprovalResponseDTO(
                    m.getId(), m.getName(), m.getAddress(), m.getPhone(), m.getEmail(),
                    m.getCategory(), m.getRating(), m.isActive(), false, m.getCreatedAt()
            );
        });
    }

    public List<DriverApprovalResponseDTO> getPendingDrivers() {
        return driverJpaRepository.findByActiveFalse().stream()
                .map(d -> new DriverApprovalResponseDTO(
                        d.getId(),
                        d.getUserId(),
                        d.getDeliveryLevel(),
                        d.getVehiclePlate(),
                        d.getVehicleType(),
                        d.getLicenseNumber(),
                        d.getTaxId(),
                        d.isActive(),
                        d.getTaxId() != null && EcuadorianRucValidator.isValid(d.getTaxId()),
                        d.getRating(),
                        d.getTotalDeliveries(),
                        d.getCreatedAt()
                ))
                .toList();
    }

    @Transactional
    public Optional<DriverApprovalResponseDTO> approveDriver(UUID driverId) {
        return driverJpaRepository.findById(driverId).map(d -> {
            d.setActive(true);
            driverJpaRepository.save(d);
            return new DriverApprovalResponseDTO(
                    d.getId(), d.getUserId(), d.getDeliveryLevel(), d.getVehiclePlate(),
                    d.getVehicleType(), d.getLicenseNumber(), d.getTaxId(), d.isActive(),
                    d.getTaxId() != null && EcuadorianRucValidator.isValid(d.getTaxId()),
                    d.getRating(), d.getTotalDeliveries(), d.getCreatedAt()
            );
        });
    }

    @Transactional
    public Optional<DriverApprovalResponseDTO> rejectDriver(UUID driverId, String reason) {
        return driverJpaRepository.findById(driverId).map(d -> {
            d.setActive(false);
            driverJpaRepository.save(d);
            return new DriverApprovalResponseDTO(
                    d.getId(), d.getUserId(), d.getDeliveryLevel(), d.getVehiclePlate(),
                    d.getVehicleType(), d.getLicenseNumber(), d.getTaxId(), d.isActive(),
                    false, d.getRating(), d.getTotalDeliveries(), d.getCreatedAt()
            );
        });
    }
}
