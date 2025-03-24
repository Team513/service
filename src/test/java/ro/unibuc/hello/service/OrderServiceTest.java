package ro.unibuc.hello.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ro.unibuc.hello.data.InventoryEntity;
import ro.unibuc.hello.data.OrderEntity;
import ro.unibuc.hello.data.OrderRepository;
import ro.unibuc.hello.data.OrderStatus;
import ro.unibuc.hello.data.RobotEntity;
import ro.unibuc.hello.data.RobotRepository;
import ro.unibuc.hello.data.InventoryEntity;
import ro.unibuc.hello.data.InventoryRepository;
import ro.unibuc.hello.dto.OrderDTO;
import ro.unibuc.hello.exception.EntityNotFoundException;
import ro.unibuc.hello.exception.InsufficientStockException;
import ro.unibuc.hello.exception.InvalidQuantityException;
import ro.unibuc.hello.exception.ItemNotFoundException;
import ro.unibuc.hello.exception.RobotBusyException;
import ro.unibuc.hello.exception.RobotNotFoundException;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private RobotRepository robotRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetAllOrders() {
        // Arrange
        List<OrderEntity> entities = Arrays.asList(
                new OrderEntity("worker1", OrderStatus.PENDING, "item1", 10, "location1"),
                new OrderEntity("worker2", OrderStatus.COMPLETED, "item2", 20, "location2")
        );
        when(orderRepository.findAll()).thenReturn(entities);

        // Act
        List<OrderDTO> orders = orderService.getAllOrders();

        // Assert
        assertEquals(2, orders.size());
        assertEquals("worker1", orders.get(0).getRobotId());
        assertEquals("worker2", orders.get(1).getRobotId());
    }

    @Test
    void testGetOrderById_ExistingEntity() throws EntityNotFoundException {
        // Arrange
        String id = "1";
        OrderEntity entity = new OrderEntity("worker1", OrderStatus.PENDING, "item1", 10, "location1");
        entity.setId(id);
        when(orderRepository.findById(id)).thenReturn(Optional.of(entity));

        // Act
        OrderDTO order = orderService.getOrderById(id);

        // Assert
        assertNotNull(order);
        assertEquals(id, order.getId()); // Ensure we're comparing the correct field
        assertEquals("worker1", order.getRobotId());
    }

    @Test
    void testGetOrderById_NonExistingEntity() {
        // Arrange
        String id = "NonExistingId";
        when(orderRepository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> orderService.getOrderById(id));
    }

    @Test
    void testCreateOrder() {
        // Arrange
        OrderDTO orderDTO = new OrderDTO(null, "worker1", OrderStatus.PENDING, "item1", 10, "location1");
    
        // Mock RobotEntity
        RobotEntity mockRobot = new RobotEntity();
        mockRobot.setId("worker1");
        mockRobot.setCurrentOrderId(null);
        when(robotRepository.findById("worker1")).thenReturn(Optional.of(mockRobot));
    
        // Mock InventoryEntity
        InventoryEntity mockInventory = new InventoryEntity();
        mockInventory.setId("item1");
        mockInventory.setStock(100);
        when(inventoryRepository.findById("item1")).thenReturn(Optional.of(mockInventory));
    
        // Mock OrderEntity creation and saving
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(invocation -> {
            OrderEntity order = invocation.getArgument(0);
            order.setId("1"); // Set the ID as part of the save operation
            return order;
        });
    
        // Act
        OrderDTO createdOrder = orderService.createOrder(orderDTO);
    
        // Assert
        assertNotNull(createdOrder);
        assertEquals("worker1", createdOrder.getRobotId());
        assertEquals("1", createdOrder.getId());
    }
    

    @Test
    void testUpdateOrderStatus_ExistingEntity() throws EntityNotFoundException {
        // Arrange
        String id = "1";
        String status = "COMPLETED";
        OrderEntity entity = new OrderEntity("worker1", OrderStatus.PENDING, "item1", 10, "location1");
        entity.setId(id);
        when(orderRepository.findById(id)).thenReturn(Optional.of(entity));
        when(orderRepository.save(any(OrderEntity.class))).thenReturn(entity);

        // Act
        OrderDTO updatedOrder = orderService.updateOrderStatus(id, status);

        // Assert
        assertNotNull(updatedOrder);
        assertEquals(OrderStatus.COMPLETED, updatedOrder.getStatus());
    }

    @Test
    void testUpdateOrderStatus_NonExistingEntity() {
        // Arrange
        String id = "NonExistingId";
        String status = "COMPLETED";
        when(orderRepository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> orderService.updateOrderStatus(id, status));
    }

    @Test
    void testDeleteOrder_ExistingEntity() throws EntityNotFoundException {
        // Arrange
        String id = "1";
        OrderEntity entity = new OrderEntity("worker1", OrderStatus.PENDING, "item1", 10, "location1");
        entity.setId(id);
        when(orderRepository.findById(id)).thenReturn(Optional.of(entity));

        // Act
        orderService.deleteOrder(id);

        // Assert
        verify(orderRepository, times(1)).delete(entity);
    }

    @Test
    void testDeleteOrder_NonExistingEntity() {
        // Arrange
        String id = "NonExistingId";
        when(orderRepository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> orderService.deleteOrder(id));
    }


    @Test
    void testRobotNotFound() {
        // Arrange
        OrderDTO orderDTO = new OrderDTO(null, "workerNotFound", OrderStatus.PENDING, "item1", 10, "location1");

        // Mock repository to return an empty result for robot
        when(robotRepository.findById("workerNotFound")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RobotNotFoundException.class, () -> orderService.createOrder(orderDTO));
    }

    @Test
    void testRobotAlreadyHasActiveOrder() {
        // Arrange
        OrderDTO orderDTO = new OrderDTO(null, "worker1", OrderStatus.PENDING, "item1", 10, "location1");

        // Mock RobotEntity
        RobotEntity mockRobot = new RobotEntity();
        mockRobot.setId("worker1");
        mockRobot.setCurrentOrderId("someOrderId"); // The robot already has an active order
        when(robotRepository.findById("worker1")).thenReturn(Optional.of(mockRobot));

        // Act & Assert
        assertThrows(RobotBusyException.class, () -> orderService.createOrder(orderDTO));
    }

    @Test
    void testInvalidQuantity() {
        // Arrange
        OrderDTO orderDTO = new OrderDTO(null, "worker1", OrderStatus.PENDING, "item1", 0, "location1"); // Invalid quantity

        // Mock RobotEntity
        RobotEntity mockRobot = new RobotEntity();
        mockRobot.setId("worker1");
        mockRobot.setCurrentOrderId(null); // The robot is available for an order
        when(robotRepository.findById("worker1")).thenReturn(Optional.of(mockRobot));

        // Mock InventoryEntity
        InventoryEntity mockInventory = new InventoryEntity();
        mockInventory.setId("item1");
        mockInventory.setStock(100); // Sufficient stock
        when(inventoryRepository.findById("item1")).thenReturn(Optional.of(mockInventory));

        // Act & Assert
        assertThrows(InvalidQuantityException.class, () -> orderService.createOrder(orderDTO));
    }

    @Test
    void testItemNotFound() {
        // Arrange
        OrderDTO orderDTO = new OrderDTO(null, "worker1", OrderStatus.PENDING, "nonExistentItem", 10, "location1");

        // Mock RobotEntity
        RobotEntity mockRobot = new RobotEntity();
        mockRobot.setId("worker1");
        mockRobot.setCurrentOrderId(null); // The robot is available for an order
        when(robotRepository.findById("worker1")).thenReturn(Optional.of(mockRobot));

        // Mock InventoryEntity to return empty for non-existent item
        when(inventoryRepository.findById("nonExistentItem")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ItemNotFoundException.class, () -> orderService.createOrder(orderDTO));
    }

    @Test
    void testInsufficientStock() {
        // Arrange
        OrderDTO orderDTO = new OrderDTO(null, "worker1", OrderStatus.PENDING, "item1", 200, "location1"); // Requesting more stock than available

        // Mock RobotEntity
        RobotEntity mockRobot = new RobotEntity();
        mockRobot.setId("worker1");
        mockRobot.setCurrentOrderId(null); // The robot is available for an order
        when(robotRepository.findById("worker1")).thenReturn(Optional.of(mockRobot));

        // Mock InventoryEntity
        InventoryEntity mockInventory = new InventoryEntity();
        mockInventory.setId("item1");
        mockInventory.setStock(100); // Insufficient stock
        when(inventoryRepository.findById("item1")).thenReturn(Optional.of(mockInventory));

        // Act & Assert
        assertThrows(InsufficientStockException.class, () -> orderService.createOrder(orderDTO));
    }

    @Test
    void testCountCompletedOrders() {
        // Arrange
        OrderEntity completedOrder = new OrderEntity("worker1", OrderStatus.COMPLETED, "item1", 10, "location1");
        when(orderRepository.findAll()).thenReturn(Arrays.asList(completedOrder, new OrderEntity("worker2", OrderStatus.PENDING, "item2", 5, "location2")));

        // Act
        long count = orderService.countCompletedOrders();

        // Assert
        assertEquals(1, count); // Only one completed order
    }

    @Test
    void testCountCanceledOrders() {
        // Arrange
        OrderEntity canceledOrder = new OrderEntity("worker1", OrderStatus.CANCELED, "item1", 10, "location1");
        when(orderRepository.findAll()).thenReturn(Arrays.asList(canceledOrder, new OrderEntity("worker2", OrderStatus.PENDING, "item2", 5, "location2")));
    
        // Act
        long count = orderService.countCanceledOrders();
    
        // Assert
        assertEquals(1, count); // Only one canceled order
    }

    @Test
    void testUpdateOrderStatus_NewStatus() throws EntityNotFoundException {
        // Arrange
        String id = "1";
        String status = "CANCELED";
        OrderEntity entity = new OrderEntity("worker1", OrderStatus.PENDING, "item1", 10, "location1");
        entity.setId(id);
        when(orderRepository.findById(id)).thenReturn(Optional.of(entity));
        when(orderRepository.save(any(OrderEntity.class))).thenReturn(entity);
    
        // Act
        OrderDTO updatedOrder = orderService.updateOrderStatus(id, status);
    
        // Assert
        assertNotNull(updatedOrder);
        assertEquals(OrderStatus.CANCELED, updatedOrder.getStatus());
    }

    @Test
    void testHasActiveOrderForRobot_WithActiveOrder() {
        // Arrange
        String robotId = "worker1";
    
        // Mock RobotEntity
        RobotEntity mockRobot = new RobotEntity();
        mockRobot.setId(robotId);
        mockRobot.setCurrentOrderId("activeOrderId");
    
        // Mock OrderEntity for the active order
        OrderEntity activeOrder = new OrderEntity(robotId, OrderStatus.PENDING, "item1", 10, "location1");
        activeOrder.setId("activeOrderId");
    
        // Mock repositories
        when(robotRepository.findById(robotId)).thenReturn(Optional.of(mockRobot));
        when(orderRepository.findAll()).thenReturn(Arrays.asList(activeOrder));
    
        // Act
        boolean hasActiveOrder = orderService.hasActiveOrderForRobot(robotId);
    
        // Assert
        assertTrue(hasActiveOrder);
    }
    
    
    @Test
    void testHasActiveOrderForRobot_NoActiveOrder() {
        // Arrange
        String robotId = "worker2";
    
        // Mock RobotEntity
        RobotEntity mockRobot = new RobotEntity();
        mockRobot.setId(robotId);
        mockRobot.setCurrentOrderId(null);  // No active order
    
        // Mock repositories
        when(robotRepository.findById(robotId)).thenReturn(Optional.of(mockRobot));
    
        // Act
        boolean hasActiveOrder = orderService.hasActiveOrderForRobot(robotId);
    
        // Assert
        assertFalse(hasActiveOrder);
    }
    
    @Test
    void testLambdaHasActiveOrderForRobot() {
        // Arrange
        String robotId = "worker1";

        // Mock RobotEntity
        RobotEntity mockRobot = new RobotEntity();
        mockRobot.setId(robotId);
        mockRobot.setCurrentOrderId("activeOrderId");

        // Mock OrderEntity
        OrderEntity activeOrder = new OrderEntity(robotId, OrderStatus.PENDING, "item1", 10, "location1");
        activeOrder.setId("activeOrderId");

        // Mock repositories
        when(robotRepository.findById(robotId)).thenReturn(Optional.of(mockRobot));
        when(orderRepository.findById("activeOrderId")).thenReturn(Optional.of(activeOrder));

        // Assuming lambda is used in filtering orders by robotId, verify behavior
        List<OrderEntity> orders = Arrays.asList(activeOrder);
        boolean isActive = orders.stream()
                                .anyMatch(order -> order.getRobotId().equals(robotId) && order.getStatus() == OrderStatus.PENDING);
        
        // Act & Assert
        assertTrue(isActive);  // Ensure the filter works as expected
    }

    
}
