package ru.yandex.practicum.mymarket.test.integration.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.mymarket.controller.OrderController;
import ru.yandex.practicum.mymarket.dto.OrderDto;
import ru.yandex.practicum.mymarket.service.OrderService;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(OrderController.class)
public class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Test
    public void testGetOrders() throws Exception {
        OrderDto order1 = new OrderDto();
        order1.setId(1L);
        order1.setTotalSum(20L);

        OrderDto order2 = new OrderDto();
        order2.setId(2L);
        order2.setTotalSum(10L);

        List<OrderDto> mockOrders = Arrays.asList(order1, order2);

        when(orderService.getAllOrders()).thenReturn(mockOrders);

        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(view().name("orders"))
                .andExpect(model().attributeExists("orders"))
                .andExpect(model().attribute("orders", mockOrders));
    }

    @Test
    public void testGetOrderPage() throws Exception {
        OrderDto mockOrder = new OrderDto();
        mockOrder.setId(10L);
        mockOrder.setTotalSum(20L);

        when(orderService.getOrderById(10L)).thenReturn(mockOrder);

        mockMvc.perform(get("/orders/10")
                        .param("newOrder", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("order"))
                .andExpect(model().attributeExists("order"))
                .andExpect(model().attribute("order", mockOrder))
                .andExpect(model().attribute("newOrder", true));
    }
}