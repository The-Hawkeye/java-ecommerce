package com.impetus.order_service.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UpdateInventoryDto{
    List<UpdateInventoryRequest> item;
}
