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
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;
    private final Counter getAllOrdersCounter;
    private final Timer getAllOrdersTimer;
    private final Counter getOrderByIdCounter;
    private final Timer getOrderByIdTimer;

    @Autowired
    public OrderController(OrderService orderService, MeterRegistry meterRegistry) {
        this.orderService = orderService;

        // Explicitly register custom metrics
        this.getAllOrdersCounter = meterRegistry.counter("orders.getAll.count");
        this.getAllOrdersTimer = meterRegistry.timer("orders.getAll.time");
        this.getOrderByIdCounter = meterRegistry.counter("orders.getById.count");
        this.getOrderByIdTimer = meterRegistry.timer("orders.getById.time");
    }

    @GetMapping
    public List<OrderDTO> getAllOrders() {
        getAllOrdersCounter.increment();
        return getAllOrdersTimer.record(() -> orderService.getAllOrders());
    }

    @GetMapping("/{id}")
    public OrderDTO getOrderById(@PathVariable String id) throws EntityNotFoundException {
        getOrderByIdCounter.increment();
        return getOrderByIdTimer.record(() -> orderService.getOrderById(id));
    }

    @PostMapping
    public OrderDTO createOrder(@Valid @RequestBody OrderDTO orderDTO) {
        Counter createOrderCounter = meterRegistry.counter("orders.create.count");
        Timer createOrderTimer = meterRegistry.timer("orders.create.time");

        createOrderCounter.increment();
        return createOrderTimer.record(() -> {
            if (orderService.hasActiveOrderForRobot(orderDTO.getRobotId())) {
                throw new IllegalStateException("This robot already has an active order. Please wait for it to finish.");
            }
            return orderService.createOrder(orderDTO);
        });
    }

    @PutMapping("/{id}/status")
    public OrderDTO updateOrderStatus(@PathVariable String id, @RequestBody Map<String, String> body) throws EntityNotFoundException {
        Counter updateStatusCounter = meterRegistry.counter("orders.updateStatus.count");
        Timer updateStatusTimer = meterRegistry.timer("orders.updateStatus.time");

        updateStatusCounter.increment();
        return updateStatusTimer.record(() -> {
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
        });
    }

    @DeleteMapping("/{id}")
    public void deleteOrder(@PathVariable String id) throws EntityNotFoundException {
        Counter deleteOrderCounter = meterRegistry.counter("orders.delete.count");
        Timer deleteOrderTimer = meterRegistry.timer("orders.delete.time");

        deleteOrderCounter.increment();
        deleteOrderTimer.record(() -> orderService.deleteOrder(id));
    }

    @GetMapping("/stats")
    public ResponseEntity<String> getOrderStats() {
        return ResponseEntity.ok(
            String.format("Completed Orders: %d, Canceled Orders: %d",
                orderService.countCompletedOrders(),
                orderService.countCanceledOrders()
            )
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