package com.ydong.orderservice.service;

import com.ydong.orderservice.dto.OrderLineItemsDto;
import com.ydong.orderservice.dto.OrderRequest;
import com.ydong.orderservice.model.Order;
import com.ydong.orderservice.model.OrderLineItems;
import com.ydong.orderservice.repository.OrderRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient webClient;
    public void placeOrder(OrderRequest orderRequest){
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());

        List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsDtoList()
            .stream()
            .map(this::mapToDto)
            .toList();

        order.setOrderLineItemsList(orderLineItems);

        // Call Inventory Service, and Place order if product is in Stock
        Boolean result = webClient.get()
            .uri("http://localhost:8094/api/inventory")
            .retrieve()
            .bodyToMono(Boolean.class)
            .block();

        if(result){
            orderRepository.save(order);
        }else{
            throw new IllegalArgumentException("Product is not in stock, please try again later");
        }
    }

    private OrderLineItems mapToDto(OrderLineItemsDto orderLineItemsDto) {
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setPrice(orderLineItemsDto.getPrice());
        orderLineItems.setQuantity(orderLineItemsDto.getQuantity());
        orderLineItems.setSkuCode(orderLineItemsDto.getSkuCode());

        return orderLineItems;
    }


}
