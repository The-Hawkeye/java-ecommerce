package com.impetus.order_service.service.payment;


import com.impetus.order_service.enums.PaymentGatewayType;

public abstract class BasePaymentGateway implements PaymentGateway {

    protected final GatewayUrls urls;

    protected BasePaymentGateway(GatewayUrls urls) {
        this.urls = urls;
    }

    public GatewayUrls urls() {
        return urls;
    }

    // Common helpers you may reuse across gateways
    protected String withQuery(String base, java.util.Map<String,String> params) {
        StringBuilder sb = new StringBuilder(base);
        if (!params.isEmpty()) {
            sb.append(base.contains("?") ? "&" : "?");
            params.forEach((k, v) -> sb.append(k).append("=").append(v).append("&"));
            sb.setLength(sb.length() - 1); // remove trailing '&'
        }
        return sb.toString();
    }

    // Shared contract defaults
    @Override
    public PaymentGatewayType type() { return PaymentGatewayType.RAZORPAY; }
}
