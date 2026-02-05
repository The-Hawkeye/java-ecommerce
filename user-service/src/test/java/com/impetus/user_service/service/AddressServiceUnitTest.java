package com.impetus.user_service.service;

import com.impetus.user_service.dto.address.AddressResponse;
import com.impetus.user_service.dto.address.CreateAddressRequest;
import com.impetus.user_service.dto.address.UpdateAddressRequest;
import com.impetus.user_service.entity.Address;
import com.impetus.user_service.repository.AddressRepository;
import com.impetus.user_service.service.Impl.AddressServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddressServiceUnitTest {

    @Mock
    private AddressRepository addressRepository;

    @InjectMocks
    private AddressServiceImpl addressService;

    private final Long userId = 101L;

    @Captor
    private ArgumentCaptor<List<Address>> addressListCaptor;

    @Captor
    private ArgumentCaptor<Address> addressCaptor;

    // --------- Helpers ---------

    private Address defaultAddress(Long id) {
        Address a = new Address();
        a.setId(id);
        a.setUserId(userId);
        a.setIsDefaultShipping(true);
        a.setAddressLabel("Default " + id);
        return a;
    }

    private Address nonDefaultAddress(Long id) {
        Address a = new Address();
        a.setId(id);
        a.setUserId(userId);
        a.setIsDefaultShipping(false);
        a.setAddressLabel("Addr " + id);
        return a;
    }

    private CreateAddressRequest baseCreateReq(Boolean isDefault) {
        CreateAddressRequest req = new CreateAddressRequest();
        req.setAddressLabel("Home");
        req.setContactName("John Doe");
        req.setPhone("9999999999");
        req.setAddressLine1("Line1");
        req.setAddressLine2("Line2");
        req.setLocality("Locality");
        req.setCity("City");
        req.setState("State");
        req.setPincode("560001");
        req.setIsDefaultShipping(isDefault);
        return req;
    }

    private UpdateAddressRequest baseUpdateReq(Boolean isDefault) {
        UpdateAddressRequest req = new UpdateAddressRequest();
        req.setAddressLabel("Updated Label");
        req.setContactName("Jane Doe");
        req.setPhone("8888888888");
        req.setAddressLine1("Upd Line1");
        req.setAddressLine2("Upd Line2");
        req.setLocality("Upd Locality");
        req.setCity("Upd City");
        req.setState("Upd State");
        req.setPincode("560002");
        req.setIsDefaultShipping(isDefault);
        return req;
    }

    // ------------------ createAddress ------------------

    @Test
    @DisplayName("createAddress: when isDefaultShipping=true, unset existing defaults and save new default")
    void createAddress_unsetExistingDefaultsAndSetNew() {
        List<Address> existing = Arrays.asList(
                defaultAddress(1L),
                nonDefaultAddress(2L)
        );
        when(addressRepository.findAllByUserId(userId)).thenReturn(existing);

        Address persisted = new Address();
        persisted.setId(10L);
        persisted.setUserId(userId);
        persisted.setIsDefaultShipping(true);
        persisted.setAddressLabel("Home");
        when(addressRepository.save(any(Address.class))).thenReturn(persisted);

        CreateAddressRequest req = baseCreateReq(true);

        AddressResponse resp = addressService.createAddress(userId, req);

        // Verify existing defaults were unset
        verify(addressRepository).saveAll(addressListCaptor.capture());
        List<Address> savedList = addressListCaptor.getValue();
        assertEquals(2, savedList.size());
        for (Address a : savedList) {
            assertFalse(a.getIsDefaultShipping(), "Existing default should be unset");
        }

        // Verify new address save
        verify(addressRepository).save(addressCaptor.capture());
        Address savedNew = addressCaptor.getValue();
        assertEquals(userId, savedNew.getUserId());
        assertTrue(savedNew.getIsDefaultShipping());

        // Response mapping checks
        assertEquals(10L, resp.getId());
        assertTrue(resp.getIsDefaultShipping());
        assertEquals("Home", resp.getAddressLabel());
    }

    @Test
    @DisplayName("createAddress: when isDefaultShipping=null, default should be false")
    void createAddress_defaultNullBecomesFalse() {
        CreateAddressRequest req = baseCreateReq(null);

        Address persisted = new Address();
        persisted.setId(20L);
        persisted.setUserId(userId);
        persisted.setIsDefaultShipping(false);
        when(addressRepository.save(any(Address.class))).thenReturn(persisted);

        AddressResponse resp = addressService.createAddress(userId, req);

        verify(addressRepository, never()).saveAll(anyList());
        verify(addressRepository).save(addressCaptor.capture());

        Address saved = addressCaptor.getValue();
        assertFalse(saved.getIsDefaultShipping());
        assertFalse(resp.getIsDefaultShipping());
    }

    @Test
    @DisplayName("createAddress: when isDefaultShipping=false, do not alter existing addresses")
    void createAddress_nonDefaultDoesNotAlterExisting() {
        CreateAddressRequest req = baseCreateReq(false);

        Address persisted = new Address();
        persisted.setId(21L);
        persisted.setUserId(userId);
        persisted.setIsDefaultShipping(false);
        when(addressRepository.save(any(Address.class))).thenReturn(persisted);

        AddressResponse resp = addressService.createAddress(userId, req);

        verify(addressRepository, never()).findAllByUserId(anyLong());
        verify(addressRepository, never()).saveAll(anyList());
        verify(addressRepository).save(any(Address.class));
        assertFalse(resp.getIsDefaultShipping());
    }

    // ------------------ listAddresses ------------------

    @Test
    @DisplayName("listAddresses: maps and returns in order (default first)")
    void listAddresses_orderAndMap() {
        List<Address> ordered = Arrays.asList(
                defaultAddress(3L),
                nonDefaultAddress(4L),
                nonDefaultAddress(5L)
        );
        when(addressRepository.findAllByUserIdOrderByIsDefaultShippingDesc(userId)).thenReturn(ordered);

        List<AddressResponse> responses = addressService.listAddresses(userId);

        assertEquals(3, responses.size());
        assertEquals(3L, responses.get(0).getId());
        assertTrue(responses.get(0).getIsDefaultShipping());
        assertFalse(responses.get(1).getIsDefaultShipping());
        assertFalse(responses.get(2).getIsDefaultShipping());
    }

    // ------------------ updateAddress ------------------

    @Test
    @DisplayName("updateAddress: not owner should throw SecurityException")
    void updateAddress_notOwnerThrows() {
        Long addressId = 100L;
        Address address = new Address();
        address.setId(addressId);
        address.setUserId(999L); // different user

        when(addressRepository.findById(addressId)).thenReturn(Optional.of(address));

        UpdateAddressRequest req = baseUpdateReq(false);

        SecurityException ex = assertThrows(SecurityException.class,
                () -> addressService.updateAddress(userId, addressId, req));
        assertEquals("Not Owner", ex.getMessage());

        verify(addressRepository, never()).save(any(Address.class));
    }

    @Test
    @DisplayName("updateAddress: when isDefaultShipping=true, unset others and set this as default")
    void updateAddress_setDefaultUnsetsOthers() {
        Long addressId = 200L;
        Address current = nonDefaultAddress(addressId);
        current.setUserId(userId);

        when(addressRepository.findById(addressId)).thenReturn(Optional.of(current));

        List<Address> existing = Arrays.asList(
                defaultAddress(201L),
                nonDefaultAddress(202L),
                current
        );
        when(addressRepository.findAllByUserId(userId)).thenReturn(existing);

        when(addressRepository.save(any(Address.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateAddressRequest req = baseUpdateReq(true);

        AddressResponse resp = addressService.updateAddress(userId, addressId, req);

        // Unset others
        verify(addressRepository).saveAll(addressListCaptor.capture());
        List<Address> savedList = addressListCaptor.getValue();

        Address previouslyDefault = savedList.stream().filter(a -> a.getId().equals(201L)).findFirst().orElse(null);
        assertNotNull(previouslyDefault);
        assertFalse(previouslyDefault.getIsDefaultShipping());

        // Current set to default
        verify(addressRepository).save(addressCaptor.capture());
        Address saved = addressCaptor.getValue();
        assertTrue(saved.getIsDefaultShipping());
        assertEquals("Updated Label", saved.getAddressLabel());

        assertTrue(resp.getIsDefaultShipping());
        assertEquals("Updated Label", resp.getAddressLabel());
    }

    @Test
    @DisplayName("updateAddress: when isDefaultShipping is null, keep default flag unchanged")
    void updateAddress_defaultFlagUnchangedIfNull() {
        Long addressId = 300L;
        Address address = defaultAddress(addressId);
        address.setUserId(userId);

        when(addressRepository.findById(addressId)).thenReturn(Optional.of(address));
        when(addressRepository.save(any(Address.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateAddressRequest req = baseUpdateReq(null);

        AddressResponse resp = addressService.updateAddress(userId, addressId, req);

        verify(addressRepository, never()).saveAll(anyList());
        verify(addressRepository).save(addressCaptor.capture());

        Address saved = addressCaptor.getValue();
        assertTrue(saved.getIsDefaultShipping()); // unchanged
        assertTrue(resp.getIsDefaultShipping());
    }

    @Test
    @DisplayName("updateAddress: throws IllegalArgumentException when address not found")
    void updateAddress_addressNotFoundThrows() {
        Long addressId = 999L;
        when(addressRepository.findById(addressId)).thenReturn(Optional.empty());

        UpdateAddressRequest req = baseUpdateReq(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> addressService.updateAddress(userId, addressId, req));
        assertEquals("Address Not found", ex.getMessage());
    }

    // ------------------ deleteAddress ------------------

    @Test
    @DisplayName("deleteAddress: not owner should throw SecurityException")
    void deleteAddress_notOwnerThrows() {
        Long addressId = 400L;
        Address address = new Address();
        address.setId(addressId);
        address.setUserId(888L);

        when(addressRepository.findById(addressId)).thenReturn(Optional.of(address));

        SecurityException ex = assertThrows(SecurityException.class,
                () -> addressService.deleteAddress(userId, addressId));
        assertEquals("Not Owner", ex.getMessage());

        verify(addressRepository, never()).delete(any(Address.class));
    }

    @Test
    @DisplayName("deleteAddress: owner deletes successfully")
    void deleteAddress_ownerDeletes() {
        Long addressId = 401L;
        Address address = new Address();
        address.setId(addressId);
        address.setUserId(userId);

        when(addressRepository.findById(addressId)).thenReturn(Optional.of(address));

        addressService.deleteAddress(userId, addressId);

        verify(addressRepository).delete(address);
    }

    @Test
    @DisplayName("deleteAddress: throws NoSuchElementException when address not found")
    void deleteAddress_addressNotFoundThrows() {
        Long addressId = 777L;
        when(addressRepository.findById(addressId)).thenReturn(Optional.empty());

        assertThrows(java.util.NoSuchElementException.class,
                () -> addressService.deleteAddress(userId, addressId));
    }

    // ------------------ getAddressById ------------------

    @Test
    @DisplayName("getAddressById: returns mapped response when found")
    void getAddressById_returnsResponse() {
        Long addressId = 500L;
        Address address = new Address();
        address.setId(addressId);
        address.setUserId(userId);
        address.setAddressLabel("Office");
        address.setContactName("John");
        address.setPhone("7777777777");
        address.setAddressLine1("A1");
        address.setAddressLine2("A2");
        address.setLocality("Loc");
        address.setCity("City");
        address.setState("State");
        address.setPincode("560003");
        address.setIsDefaultShipping(false);

        when(addressRepository.findByUserIdAndId(userId, addressId)).thenReturn(address);

        AddressResponse resp = addressService.getAddressById(userId, addressId);

        assertEquals(addressId, resp.getId());
        assertEquals("Office", resp.getAddressLabel());
        assertFalse(resp.getIsDefaultShipping());
    }
}
