package com.impetus.user_service.repository;

import com.impetus.user_service.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findAllByUserId(Long userId);
    List<Address> findAllByUserIdOrderByIsDefaultShippingDesc(Long userId);
    Address findByUserIdAndId(Long userId, Long addressId);
}
