package com.ydong.orderservice.service;

import com.ydong.orderservice.dto.InventoryResponse;
import com.ydong.orderservice.dto.OrderLineItemsDto;
import com.ydong.orderservice.dto.OrderRequest;
import com.ydong.orderservice.event.OrderPlacedEvent;
import com.ydong.orderservice.model.Order;
import com.ydong.orderservice.model.OrderLineItems;
import com.ydong.orderservice.repository.OrderRepository;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;
    private final KafkaTemplate kafkaTemplate;

    public String placeOrder(OrderRequest orderRequest){
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());

        List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsDtoList()
            .stream()
            .map(this::mapToDto)
            .toList();

        order.setOrderLineItemsList(orderLineItems);

        List<String> skuCodes = order.getOrderLineItemsList().stream()
            .map(OrderLineItems::getSkuCode)
            .toList();

        // Call Inventory Service, and Place order if product is in Stock
        InventoryResponse[] inventoryResponsesArray = webClientBuilder.build().get()
            .uri("http://inventory-service/api/inventory",
                uriBuilder -> uriBuilder.queryParam("skuCode",skuCodes).build())
            .retrieve()
            .bodyToMono(InventoryResponse[].class)
            .block();

        boolean allProductsInStock = Arrays.stream(inventoryResponsesArray)
            .allMatch(InventoryResponse::isInStock);

        if(allProductsInStock){
            orderRepository.save(order);
            kafkaTemplate.send("notificationTopic", new OrderPlacedEvent(order.getOrderNumber()));
            return "Order Placed Sucessfully!";
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
