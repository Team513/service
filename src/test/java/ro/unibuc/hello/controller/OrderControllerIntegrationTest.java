package ro.unibuc.hello.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ro.unibuc.hello.data.OrderStatus;
import ro.unibuc.hello.dto.OrderDTO;
import ro.unibuc.hello.service.OrderService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Tag("IntegrationTest")
public class OrderControllerIntegrationTest {

    @Container
    public static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:6.0.20")
            .withExposedPorts(27017)
            .withSharding();

    @BeforeAll
    public static void setUp() {
        mongoDBContainer.start();
    }

    @AfterAll
    public static void tearDown() {
        mongoDBContainer.stop();
    }

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        final String MONGO_URL = "mongodb://localhost:";
        final String PORT = String.valueOf(mongoDBContainer.getMappedPort(27017));
        registry.add("mongodb.connection.url", () -> MONGO_URL + PORT);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderService orderService;

    private final ObjectMapper objectMapper = new ObjectMapper();

  
    @Test
    public void testCreateOrder() throws Exception {
        OrderDTO newOrder = new OrderDTO(
                null, 
                "robot002", 
                OrderStatus.PENDING,
                "item124", 
                2,
                "Aisle 2"
        );

        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newOrder)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.robotId").value("robot002"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.itemId").value("item124"))
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.location").value("Aisle 2"));
    }

    @Test
    public void testCreateOrderWithRobotAlreadyBusy() throws Exception {
        OrderDTO newOrder = new OrderDTO(
                null,
                "robot001", 
                OrderStatus.PENDING,
                "item124",
                2,
                "Aisle 2"
        );

        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newOrder)))
                .andExpect(status().isConflict())
                .andExpect(content().string("This robot already has an active order. Please wait for it to finish."));
    }


    @Test
    public void testUpdateOrderStatus() throws Exception {
        // Update the existing order's status
        mockMvc.perform(put("/orders/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"in_progress\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        // Verify the update
        mockMvc.perform(get("/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    public void testUpdateOrderStatusWithInvalidStatus() throws Exception {
        mockMvc.perform(put("/orders/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"invalid_status\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid status: must be PENDING, IN_PROGRESS, COMPLETED, or CANCELED"));
    }

    @Test
    public void testGetOrderStats() throws Exception {
        mockMvc.perform(get("/orders/stats"))
                .andExpect(status().isOk())
                .andExpect(content().string("Completed Orders: 0, Canceled Orders: 0"));
    }
}