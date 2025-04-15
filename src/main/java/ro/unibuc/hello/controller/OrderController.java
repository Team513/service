package ro.unibuc.hello.controller;

import jakarta.validation.Valid;  
import org.springframework.http.ResponseEntity;  
import org.springframework.http.HttpStatus;  
import org.springframework.beans.factory.annotation.Autowired;  
import org.springframework.web.bind.annotation.*;  
import ro.unibuc.hello.dto.OrderDTO;  
import ro.unibuc.hello.exception.EntityNotFoundException;  
import ro.unibuc.hello.service.OrderService;  
import ro.unibuc.hello.data.OrderStatus;  
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.annotation.Counted;


import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;
   
    @Timed(value = "orders_getAll_time", description = "Time taken to fetch all orders")
    @Counted(value = "orders_getAll_count", description = "Number of times all orders were fetched")
    @GetMapping
    public List<OrderDTO> getAllOrders() {
        return orderService.getAllOrders();
    }

    @Timed(value = "orders_getById_time", description = "Time taken to fetch an order by ID")
    @Counted(value = "orders_getById_count", description = "Number of times an order was fetched by ID")
    @GetMapping("/{id}")
    public OrderDTO getOrderById(@PathVariable String id) throws EntityNotFoundException {
        return orderService.getOrderById(id);
    }

    @Timed(value = "orders_create_time", description = "Time taken to create an order")
    @Counted(value = "orders_create_count", description = "Number of times an order was created")
    @PostMapping
    public OrderDTO createOrder(@Valid @RequestBody OrderDTO orderDTO) {
        if (orderService.hasActiveOrderForRobot(orderDTO.getRobotId())) {
            throw new IllegalStateException("This robot already has an active order. Please wait for it to finish.");
        }        
        return orderService.createOrder(orderDTO);
    }

    @Timed(value = "orders_updateStatus_time", description = "Time taken to update an order's status")
    @Counted(value = "orders_updateStatus_count", description = "Number of times an order's status was updated")
    @PutMapping("/{id}/status")
    public OrderDTO updateOrderStatus(@PathVariable String id, @RequestBody Map<String, String> body) throws EntityNotFoundException {
        String status = body.get("status");
        if (status == null) {
            throw new IllegalArgumentException("Status is required");
        }

        try {
            OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
            return orderService.updateOrderStatus(id, orderStatus.name());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status: must be PENDING, IN_PROGRESS, COMPLETED, or CANCELED");
        }
    }

    

    @Timed(value = "orders_delete_time", description = "Time taken to delete an order")
    @Counted(value = "orders_delete_count", description = "Number of times an order was deleted")
    @DeleteMapping("/{id}")
    public void deleteOrder(@PathVariable String id) throws EntityNotFoundException {
        orderService.deleteOrder(id);
    }

    @GetMapping("/stats")
    public ResponseEntity<String> getOrderStats() {
        int completedOrders = orderService.countCompletedOrders();
        int canceledOrders = orderService.countCanceledOrders();
        return ResponseEntity.ok(
            String.format("Completed Orders: %d, Canceled Orders: %d", completedOrders, canceledOrders)
        );
    }

    @ExceptionHandler({EntityNotFoundException.class, IllegalArgumentException.class})
    public ResponseEntity<String> handleExceptions(Exception ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleConflict(Exception ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }
}
