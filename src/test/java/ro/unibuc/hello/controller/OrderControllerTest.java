package ro.unibuc.hello.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ro.unibuc.hello.data.OrderStatus;
import ro.unibuc.hello.dto.OrderDTO;
import ro.unibuc.hello.exception.EntityNotFoundException;
import ro.unibuc.hello.service.OrderService;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController orderController;

    private MockMvc mockMvc;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(orderController).build();
    }

    @Test
    void testGetAllOrders() throws Exception {
        List<OrderDTO> orders = Arrays.asList(
                new OrderDTO("1", "worker1", OrderStatus.PENDING, "item1", 10, "location1"),
                new OrderDTO("2", "worker2", OrderStatus.COMPLETED, "item2", 20, "location2")
        );
        when(orderService.getAllOrders()).thenReturn(orders);

        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("1"))
                .andExpect(jsonPath("$[1].id").value("2"));
    }

    @Test
    void testGetOrderById_ExistingEntity() throws Exception {
        String id = "1";
        OrderDTO order = new OrderDTO(id, "worker1", OrderStatus.PENDING, "item1", 10, "location1");
        when(orderService.getOrderById(id)).thenReturn(order);

        mockMvc.perform(get("/orders/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
    }

    @Test
    void testCreateOrder() throws Exception {
        OrderDTO createdOrder = new OrderDTO("1", "worker1", OrderStatus.PENDING, "item1", 10, "location1");
        when(orderService.createOrder(any(OrderDTO.class))).thenReturn(createdOrder);

        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"robotId\":\"worker1\",\"status\":\"PENDING\",\"itemId\":\"item1\",\"quantity\":10,\"location\":\"location1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"));
    }

    @Test
    void testCreateOrder_Conflict() throws Exception {
        when(orderService.hasActiveOrderForRobot("worker1")).thenReturn(true);

        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"robotId\":\"worker1\",\"status\":\"PENDING\",\"itemId\":\"item1\",\"quantity\":10,\"location\":\"location1\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void testUpdateOrderStatus_ExistingEntity() throws Exception {
        String id = "1";
        OrderDTO updatedOrder = new OrderDTO(id, "worker1", OrderStatus.COMPLETED, "item1", 10, "location1");
        when(orderService.updateOrderStatus(eq(id), eq("COMPLETED"))).thenReturn(updatedOrder);

        mockMvc.perform(put("/orders/{id}/status", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void testUpdateOrderStatus_MissingStatus() throws Exception {
        mockMvc.perform(put("/orders/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Status is required"));
    }

    @Test
    void testUpdateOrderStatus_InvalidStatus() throws Exception {
        mockMvc.perform(put("/orders/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"INVALID\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid status: must be PENDING, IN_PROGRESS, COMPLETED, or CANCELED"));
    }

    @Test
    void testDeleteOrder_ExistingEntity() throws Exception {
        String id = "1";
        doNothing().when(orderService).deleteOrder(id);

        mockMvc.perform(delete("/orders/{id}", id))
                .andExpect(status().isOk());
    }

    @Test
    void testGetOrderStats() throws Exception {
        when(orderService.countCompletedOrders()).thenReturn(5);
        when(orderService.countCanceledOrders()).thenReturn(2);

        mockMvc.perform(get("/orders/stats"))
                .andExpect(status().isOk())
                .andExpect(content().string("Completed Orders: 5, Canceled Orders: 2"));
    }

    @Test
    void testHandleEntityNotFoundException() throws Exception {
        String nonExistentId = "nonexistent";
        when(orderService.getOrderById(nonExistentId)).thenThrow(new EntityNotFoundException("Order"));

        mockMvc.perform(get("/orders/{id}", nonExistentId))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Entity: Order was not found"));
    }
}