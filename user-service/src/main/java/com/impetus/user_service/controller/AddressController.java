package com.impetus.user_service.controller;

import com.impetus.user_service.dto.address.AddressResponse;
import com.impetus.user_service.dto.address.CreateAddressRequest;
import com.impetus.user_service.dto.address.UpdateAddressRequest;
import com.impetus.user_service.exception.BadRequestException;
import com.impetus.user_service.response.ApiResponse;
import com.impetus.user_service.service.AddressService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

@RestController
@RequestMapping("/user/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;
    private static final Logger log = LoggerFactory.getLogger(AddressController.class);

    private Long getCurrentUserId(HttpServletRequest request){

        String raw = request.getHeader("X-User-Id");
        if (raw == null) {
            log.error("Gateway failed to assign X-User-Id header");
            throw new BadRequestException("X-User-Id header missing");
        }
            Long userId = Long.valueOf(request.getHeader("X-User-Id"));
        if(userId == null){
            log.error("Gateway failed to assign X-User-Id header");
            throw new BadRequestException("Header missing");
        }
        log.info("UserId: "+userId);
        log.info("Roles from Header : "+ getCurrentUserRoles(request));
        return userId;
    }

    private String getCurrentUserRoles(HttpServletRequest request){
        String roles =  request.getHeader("X-User-Roles");
        if(roles == null){
            log.error("Gateway failed to assign X-User-Roles header");
            throw new com.impetus.user_service.exception.BadRequestException("Header missing");
        }
        return roles;
    }

    @PostMapping
//    @PreAuthorize("hasRole('ADMIN') or #userId.toString() == authentication.name")
    public ResponseEntity<ApiResponse<AddressResponse>> createAddress( @RequestBody CreateAddressRequest req, HttpServletRequest request){
        Long userId = getCurrentUserId(request);
        AddressResponse res = addressService.createAddress(userId, req);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ApiResponse<AddressResponse>("Address created successfully", res));
    }

    @GetMapping
//    @PreAuthorize("hasRole('ADMIN') or #userId.toString() == authentication.name")
    public ResponseEntity<ApiResponse<List<AddressResponse>>> listAddresses(HttpServletRequest request){
        Long userId = getCurrentUserId(request);
        List<AddressResponse> res = addressService.listAddresses(userId);
        return ResponseEntity.ok(new ApiResponse<List<AddressResponse>>("Fetched all address successfully", res));
    }

    @PutMapping("/{addressId}")
//    @PreAuthorize("hasRole('ADMIN') or #userId.toString() == authentication.name")
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(@PathVariable("addressId") Long addressId, @RequestBody UpdateAddressRequest req, HttpServletRequest request){
        Long userId = getCurrentUserId(request);
        AddressResponse res = addressService.updateAddress(userId, addressId, req);
        return ResponseEntity.ok(new ApiResponse<AddressResponse>("Address updated successfully", res));
    }

    @DeleteMapping("/{addressId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
//    @PreAuthorize("hasRole('ADMIN') or #userId.toString() == authentication.name")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(@PathVariable("addressId") Long addressId, HttpServletRequest request){
        Long userId = getCurrentUserId(request);
        addressService.deleteAddress(userId, addressId);
        return ResponseEntity.ok(new ApiResponse<>("Address deleted successfully", null));
    }

    @GetMapping("/{addressId}")
//    @ResponseStatus(HttpStatus.OK)
//    @PreAuthorize("hasRole('ADMIN') or #userId.toString() == authentication.name")
    public ResponseEntity<ApiResponse<AddressResponse>> getAddressById(@PathVariable("addressId") Long addressId, HttpServletRequest request){
        Long userId = getCurrentUserId(request);
        AddressResponse addressResponse = addressService.getAddressById(userId, addressId);
        return ResponseEntity.ok(new ApiResponse<>("Address deleted successfully", addressResponse));
    }

}
