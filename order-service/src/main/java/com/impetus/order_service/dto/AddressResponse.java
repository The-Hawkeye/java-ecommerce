package com.impetus.order_service.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AddressResponse {
    Long id;
    String addressLabel;
    String contactName;
    String phone;
    String addressLine1;
    String addressLine2;
    String locality;
    String city;
    String state;
    String pincode;
    Boolean isDefaultShipping;
}
