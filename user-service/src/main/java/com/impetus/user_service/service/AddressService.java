package com.impetus.user_service.service;

import com.impetus.user_service.dto.address.AddressResponse;
import com.impetus.user_service.dto.address.CreateAddressRequest;
import com.impetus.user_service.dto.address.UpdateAddressRequest;

import java.util.List;

public interface AddressService {
    AddressResponse createAddress(Long userId, CreateAddressRequest req);
    List<AddressResponse> listAddresses(Long userId);
    AddressResponse updateAddress(Long userId, Long addressId, UpdateAddressRequest req);
    void deleteAddress(Long userId, Long addressId);
    AddressResponse getAddressById(Long userId, Long addressId);
}
