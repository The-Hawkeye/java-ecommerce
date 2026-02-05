package com.impetus.user_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.impetus.user_service.dto.address.AddressResponse;
import com.impetus.user_service.dto.address.CreateAddressRequest;
import com.impetus.user_service.dto.address.UpdateAddressRequest;
import com.impetus.user_service.service.AddressService;
import jakarta.ws.rs.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Standalone MockMvc unit test for AddressController.
 * No new dependencies. We register a lightweight ControllerAdvice that maps
 * jakarta.ws.rs.BadRequestException -> 400 Bad Request so header validation tests pass.
 */
class AddressControllerUnitTest {

    private MockMvc mockMvc;
    private AddressService addressService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        addressService = Mockito.mock(AddressService.class);
        AddressController controller = new AddressController(addressService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                // Inline minimal advice to convert JAX-RS BadRequestException -> 400
                .setControllerAdvice(new Object() {
                    @ResponseStatus(HttpStatus.BAD_REQUEST)
                    @ExceptionHandler(BadRequestException.class)
                    public void handleBadRequest() {
                        // return 400 with empty body
                    }
                })
                .build();

        objectMapper = new ObjectMapper();
    }

    // ---------- Helpers ----------

    private CreateAddressRequest createReq() {
        CreateAddressRequest req = new CreateAddressRequest();
        req.setAddressLabel("Home");
        req.setContactName("John");
        req.setPhone("9999999999");
        req.setAddressLine1("Line1");
        req.setAddressLine2("Line2");
        req.setLocality("Locality");
        req.setCity("City");
        req.setState("State");
        req.setPincode("560001");
        req.setIsDefaultShipping(true);
        return req;
    }

    private UpdateAddressRequest updateReq() {
        UpdateAddressRequest req = new UpdateAddressRequest();
        req.setAddressLabel("Office");
        req.setContactName("Jane");
        req.setPhone("8888888888");
        req.setAddressLine1("NewLine1");
        req.setAddressLine2("NewLine2");
        req.setLocality("NewLocality");
        req.setCity("NewCity");
        req.setState("NewState");
        req.setPincode("560002");
        req.setIsDefaultShipping(false);
        return req;
    }

    private AddressResponse resp(Long id, boolean isDefault) {
        return new AddressResponse(
                id,
                "Home",
                "John",
                "9999999999",
                "Line1",
                "Line2",
                "Locality",
                "City",
                "State",
                "560001",
                isDefault
        );
    }

    // ---------- POST /user/addresses ----------

    @Test
    @DisplayName("POST /user/addresses: 201 Created and ApiResponse payload")
    void createAddress_success() throws Exception {
        AddressResponse response = resp(10L, true);
        when(addressService.createAddress(eq(42L), any(CreateAddressRequest.class))).thenReturn(response);

        mockMvc.perform(post("/user/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "42")
                        .header("X-User-Roles", "USER")
                        .content(objectMapper.writeValueAsString(createReq())))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Address created successfully"))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.addressLabel").value("Home"))
                .andExpect(jsonPath("$.data.isDefaultShipping").value(true));

        verify(addressService).createAddress(eq(42L), any(CreateAddressRequest.class));
    }

    @Test
    @DisplayName("POST /user/addresses: missing X-User-Id -> 400 Bad Request")
    void createAddress_missingUserIdHeader() throws Exception {
        mockMvc.perform(post("/user/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Roles", "USER")
                        .content(objectMapper.writeValueAsString(createReq())))
                .andExpect(status().isBadRequest());

        verify(addressService, never()).createAddress(anyLong(), org.mockito.ArgumentMatchers.<CreateAddressRequest>any());
    }

    @Test
    @DisplayName("POST /user/addresses: missing X-User-Roles -> 400 Bad Request")
    void createAddress_missingRolesHeader() throws Exception {
        mockMvc.perform(post("/user/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "42")
                        .content(objectMapper.writeValueAsString(createReq())))
                .andExpect(status().isBadRequest());

        verify(addressService, never()).createAddress(anyLong(), org.mockito.ArgumentMatchers.<CreateAddressRequest>any());
    }

    // ---------- GET /user/addresses ----------

    @Test
    @DisplayName("GET /user/addresses: 200 OK and ApiResponse with list")
    void listAddresses_success() throws Exception {
        List<AddressResponse> responses = List.of(resp(1L, true), resp(2L, false));
        when(addressService.listAddresses(42L)).thenReturn(responses);

        mockMvc.perform(get("/user/addresses")
                        .header("X-User-Id", "42")
                        .header("X-User-Roles", "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Fetched all address successfully"))
                .andExpect(jsonPath("$.data", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].isDefaultShipping").value(true))
                .andExpect(jsonPath("$.data[1].id").value(2))
                .andExpect(jsonPath("$.data[1].isDefaultShipping").value(false));

        verify(addressService).listAddresses(42L);
    }

    @Test
    @DisplayName("GET /user/addresses: missing headers -> 400")
    void listAddresses_missingHeaders() throws Exception {
        mockMvc.perform(get("/user/addresses")
                        .header("X-User-Id", "42"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/user/addresses")
                        .header("X-User-Roles", "USER"))
                .andExpect(status().isBadRequest());

        verify(addressService, never()).listAddresses(anyLong());
    }

    // ---------- PUT /user/addresses/{addressId} ----------

    @Test
    @DisplayName("PUT /user/addresses/{id}: 200 OK and ApiResponse payload")
    void updateAddress_success() throws Exception {
        AddressResponse response = resp(99L, false);
        when(addressService.updateAddress(eq(42L), eq(99L), any(UpdateAddressRequest.class))).thenReturn(response);

        mockMvc.perform(put("/user/addresses/{addressId}", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "42")
                        .header("X-User-Roles", "ADMIN")
                        .content(objectMapper.writeValueAsString(updateReq())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Address updated successfully"))
                .andExpect(jsonPath("$.data.id").value(99))
                .andExpect(jsonPath("$.data.isDefaultShipping").value(false));

        verify(addressService).updateAddress(eq(42L), eq(99L), any(UpdateAddressRequest.class));
    }

    @Test
    @DisplayName("PUT /user/addresses/{id}: missing headers -> 400")
    void updateAddress_missingHeaders() throws Exception {
        mockMvc.perform(put("/user/addresses/{addressId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "42")
                        .content(objectMapper.writeValueAsString(updateReq())))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/user/addresses/{addressId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Roles", "USER")
                        .content(objectMapper.writeValueAsString(updateReq())))
                .andExpect(status().isBadRequest());

        verify(addressService, never()).updateAddress(anyLong(), anyLong(), any(UpdateAddressRequest.class));
    }

    // ---------- DELETE /user/addresses/{addressId} ----------

    @Test
    @DisplayName("DELETE /user/addresses/{id}: returns 200 OK and ApiResponse")
    void deleteAddress_success() throws Exception {
        doNothing().when(addressService).deleteAddress(42L, 55L);

        mockMvc.perform(delete("/user/addresses/{addressId}", 55L)
                        .header("X-User-Id", "42")
                        .header("X-User-Roles", "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Address deleted successfully"))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(addressService).deleteAddress(42L, 55L);
    }

    @Test
    @DisplayName("DELETE /user/addresses/{id}: missing headers -> 400")
    void deleteAddress_missingHeaders() throws Exception {
        mockMvc.perform(delete("/user/addresses/{addressId}", 1L)
                        .header("X-User-Id", "42"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(delete("/user/addresses/{addressId}", 1L)
                        .header("X-User-Roles", "USER"))
                .andExpect(status().isBadRequest());

        verify(addressService, never()).deleteAddress(anyLong(), anyLong());
    }

    // ---------- GET /user/addresses/{addressId} ----------

    @Test
    @DisplayName("GET /user/addresses/{id}: 200 OK and ApiResponse payload")
    void getAddressById_success() throws Exception {
        AddressResponse response = resp(77L, true);
        when(addressService.getAddressById(42L, 77L)).thenReturn(response);

        mockMvc.perform(get("/user/addresses/{addressId}", 77L)
                        .header("X-User-Id", "42")
                        .header("X-User-Roles", "USER"))
                .andExpect(status().isOk())
                // NOTE: Controller returns "Address deleted successfully"; assert current behavior.
                .andExpect(jsonPath("$.message").value("Address deleted successfully"))
                .andExpect(jsonPath("$.data.id").value(77))
                .andExpect(jsonPath("$.data.isDefaultShipping").value(true));

        verify(addressService).getAddressById(42L, 77L);
    }

    @Test
    @DisplayName("GET /user/addresses/{id}: missing headers -> 400")
    void getAddressById_missingHeaders() throws Exception {
        mockMvc.perform(get("/user/addresses/{addressId}", 1L)
                        .header("X-User-Id", "42"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/user/addresses/{addressId}", 1L)
                        .header("X-User-Roles", "USER"))
                .andExpect(status().isBadRequest());

        verify(addressService, never()).getAddressById(anyLong(), anyLong());
    }
}
