package com.impetus.order_service.service.payment;

import com.impetus.order_service.dto.OrderResponse;
import com.impetus.order_service.dto.PaymentInitiationResponse;
import com.impetus.order_service.dto.UpdateInventoryResponse;
import com.impetus.order_service.entity.Order;
import com.impetus.order_service.entity.Payment;
import com.impetus.order_service.enums.OrderStatus;
import com.impetus.order_service.enums.PaymentMode;
import com.impetus.order_service.enums.PaymentStatus;
import com.impetus.order_service.repository.OrderRepository;
import com.impetus.order_service.repository.PaymentRepository;
import com.impetus.order_service.service.OrderService;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
@AllArgsConstructor
public class RazorPayPaymentService implements PaymentService{

    private static final Logger log = LoggerFactory.getLogger(RazorPayPaymentService.class);
    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ModelMapper modelMapper;
    @Override
    public PaymentInitiationResponse initiatePayment(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(()-> new NoSuchElementException("Order Not found"));
        order.setPaymentStatus(PaymentStatus.INITIATED);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setPaymentMode(PaymentMode.UPI);
        //Creating a dummy payment entry
        Payment paymentEntry = new Payment();
        paymentEntry.setUserId(order.getUserId());
        paymentEntry.setOrder(order);
        paymentEntry.setAmountPaisa(order.getTotalAmount()*100);
        paymentEntry.setPaymentStatus(PaymentStatus.PENDING);
        Payment saved = paymentRepository.save(paymentEntry);
        order.setPaymentReference(saved);
        orderRepository.save(order);
        this.handlePaymentSuccess(orderId, saved.getId());
        return null;
    }

    @Override
    public OrderResponse handlePaymentSuccess(Long orderId, Long paymentReference) {
        //Update inventory and reservations
        // no inventory available initiate refund
        com.impetus.order_service.entity.Order order = orderRepository.findById(orderId).orElseThrow(() -> new NoSuchElementException("Order not found"));
        Payment payment = paymentRepository.findById(paymentReference).orElseThrow(()-> new NoSuchElementException("Payment Not found for this order"));
        if(order == null){
            log.error("This should never happen");
            log.error("Order not found but payment is succes for that order with Id {}"+orderId);
            throw new RuntimeException("Unable to process order");
        }

//        CONSIDERINF PAYMENT IS DONE SUCCESSFULL WITH EXACT AMOUNT FOR TESTING
//        if(order.getPaymentStatus().equals(PaymentStatus.CAPTURED)){
//            log.error("Payment already done need to initiate refund");
//            this.initiateRefund(orderId, order.getTotalAmount());
//            return modelMapper.map(order, OrderResponse.class);
//        }

//        if(!orderResponse.getTotalAmount().equals(payment.getAmountPaisa())){
//            log.error("Invalid amount paid by user : "+ orderResponse.getUserId());
//            this.initiateRefund(orderId, payment.getAmountPaisa());
//        }

        UpdateInventoryResponse response = orderService.updateInventory(order);
        if(response == null || !response.isSuccess()){
            this.initiateRefund(orderId, order.getTotalAmount());
            throw new RuntimeException("Failed to update inventory");
        }

        order.setStatus(OrderStatus.CONFIRMED);
        order.setPaymentStatus(PaymentStatus.CAPTURED);
        order.setPaymentStatus(PaymentStatus.CAPTURED);
        orderRepository.save(order);
        return null;
    }

    @Override
    public void handlePaymentFailure(Long orderId) {
//        Update reservations
        return;
    }

    @Override
    public Void initiateRefund(Long orderId, Integer amountInPaisa) {
        return null;
    }
}
