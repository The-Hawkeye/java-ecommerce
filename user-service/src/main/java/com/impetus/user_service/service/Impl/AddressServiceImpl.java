package com.impetus.user_service.service.Impl;

import com.impetus.user_service.dto.address.AddressResponse;
import com.impetus.user_service.dto.address.CreateAddressRequest;
import com.impetus.user_service.dto.address.UpdateAddressRequest;
import com.impetus.user_service.entity.Address;
import com.impetus.user_service.repository.AddressRepository;
import com.impetus.user_service.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private static final Logger log = LoggerFactory.getLogger(AddressServiceImpl.class);
    private final AddressRepository addressRepository;

    @Override
    public AddressResponse createAddress(Long userId, CreateAddressRequest req) {
        if(Boolean.TRUE.equals(req.getIsDefaultShipping())){
            List<Address> existingAddress = addressRepository.findAllByUserId(userId);
            for (Address a : existingAddress){
                if(Boolean.TRUE.equals(a.getIsDefaultShipping())){
                    a.setIsDefaultShipping(false);
                }
            }
            addressRepository.saveAll(existingAddress);
        }

        Address newAddress = new Address();
        newAddress.setUserId(userId);
        newAddress.setAddressLabel(req.getAddressLabel());
        newAddress.setContactName(req.getContactName());
        newAddress.setPhone(req.getPhone());
        newAddress.setAddressLine1(req.getAddressLine1());
        newAddress.setAddressLine2(req.getAddressLine2());
        newAddress.setLocality(req.getLocality());
        newAddress.setCity(req.getCity());
        newAddress.setState(req.getState());
        newAddress.setPincode(req.getPincode());
        newAddress.setIsDefaultShipping(req.getIsDefaultShipping() != null ? req.getIsDefaultShipping() : false);

        Address saved = addressRepository.save(newAddress);
        return mapToResponse(saved);


    }

    @Override
    public List<AddressResponse> listAddresses(Long userId) {
        return addressRepository.findAllByUserIdOrderByIsDefaultShippingDesc(userId).stream().map(this::mapToResponse).collect(Collectors.toUnmodifiableList());
    }

    @Override
    public AddressResponse updateAddress(Long userId, Long addressId, UpdateAddressRequest req) {
        Address address = addressRepository.findById(addressId).orElseThrow(()-> new IllegalArgumentException("Address Not found"));

        if(!address.getUserId().equals(userId)){
            throw new SecurityException("Not Owner");
        }

        address.setAddressLabel(req.getAddressLabel());
        address.setContactName(req.getContactName());
        address.setPhone(req.getPhone());
        address.setAddressLine1(req.getAddressLine1());
        address.setAddressLine2(req.getAddressLine2());
        address.setLocality(req.getLocality());
        address.setCity(req.getCity());
        address.setState(req.getState());
        address.setPincode(req.getPincode());

        if(req.getIsDefaultShipping() != null && req.getIsDefaultShipping()){
            List<Address> existingAddress = addressRepository.findAllByUserId(userId);
            for (Address a : existingAddress){
                if(!a.getId().equals(addressId) && Boolean.TRUE.equals(a.getIsDefaultShipping())){
                    a.setIsDefaultShipping(false);
                }
            }
            addressRepository.saveAll(existingAddress);
            address.setIsDefaultShipping(true);
        }

        Address saved = addressRepository.save(address);
        return mapToResponse(saved);
    }

    @Override
    public void deleteAddress(Long userId, Long addressId) {
        Address address = addressRepository.findById(addressId).orElseThrow();
        if(!address.getUserId().equals(userId)){
            throw new SecurityException("Not Owner");
        }

        addressRepository.delete(address);
    }

    @Override
    public AddressResponse getAddressById(Long userId, Long addressId) {
        log.info("Fetching address for userId: "+userId +"and Addressid: "+addressId);
        Address address = addressRepository.findByUserIdAndId(userId, addressId);
        log.info("Address fetched: "+ address.getId());
        return mapToResponse(address);
    }

    private AddressResponse mapToResponse(Address a){
        return new AddressResponse(a.getId(), a.getAddressLabel(), a.getContactName(), a.getPhone(), a.getAddressLine1(), a.getAddressLine2(), a.getLocality(), a.getCity(), a.getState(), a.getPincode(), a.getIsDefaultShipping());
    }
}
