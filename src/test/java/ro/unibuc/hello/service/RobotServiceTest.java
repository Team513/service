package ro.unibuc.hello.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import ro.unibuc.hello.data.RobotEntity;
import ro.unibuc.hello.data.RobotRepository;
import ro.unibuc.hello.dto.RobotDTO;
import ro.unibuc.hello.exception.EntityNotFoundException;
import ro.unibuc.hello.exception.ValidationException;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class RobotServiceTest {

    @Mock
    private RobotRepository robotRepository;

    @InjectMocks
    private RobotService robotService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetAllRobots() {
        List<RobotEntity> entities = Arrays.asList(
                new RobotEntity("IDLE", null, 5, null),
                new RobotEntity("IN_PROGRESS", "order2", 10, null)
        );
        when(robotRepository.findAll()).thenReturn(entities);

        List<RobotDTO> robots = robotService.getAllRobots();

        assertEquals(2, robots.size());
        assertNull(robots.get(0).getCurrentOrderId());
        assertEquals("order2", robots.get(1).getCurrentOrderId());
    }

    @Test
    void testGetRobotById_ExistingEntity() throws EntityNotFoundException {
        String id = "1";
        RobotEntity entity = new RobotEntity("IDLE", null, 5, null);
        entity.setId(id);
        when(robotRepository.findById(id)).thenReturn(Optional.of(entity));

        RobotDTO robot = robotService.getRobotById(id);

        assertNotNull(robot);
        assertEquals(id, robot.getId());
        assertNull(robot.getCurrentOrderId());
    }

    @Test
    void testGetRobotById_NonExistingEntity() {
        String id = "NonExistingId";
        when(robotRepository.findById(id)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> robotService.getRobotById(id));
    }

    @Test
    void testCreateRobot_Successful() {
        RobotDTO robotDTO = new RobotDTO(null, "IDLE", null, 5, null);
        RobotEntity entity = new RobotEntity("IDLE", null, 5, null);
        when(robotRepository.save(any(RobotEntity.class))).thenReturn(entity);

        RobotDTO createdRobot = robotService.createRobot(robotDTO);
        assertNotNull(createdRobot);
        assertNull(createdRobot.getCurrentOrderId());
    }

    @Test
    void testCreateRobot_InProgressWithoutOrder() {
        RobotDTO robotDTO = new RobotDTO(null, "IN_PROGRESS", null, 5, null);
        assertThrows(ValidationException.class, () -> robotService.createRobot(robotDTO));
    }

    @Test
    void testCreateRobot_IdleWithOrder() {
        RobotDTO robotDTO = new RobotDTO(null, "IDLE", "order1", 5, null);
        assertThrows(ValidationException.class, () -> robotService.createRobot(robotDTO));
    }

    @Test
    void testCreateRobot_CompletedOrdersNegative() {
        RobotDTO robotDTO = new RobotDTO(null, "IDLE", null, -1, null);
        assertThrows(ValidationException.class, () -> robotService.createRobot(robotDTO));
    }

    @Test
    void testUpdateRobotStatus_InProgressWithoutOrder() {
        String id = "1";
        RobotEntity entity = new RobotEntity("IDLE", null, 5, null);
        entity.setId(id);
        when(robotRepository.findById(id)).thenReturn(Optional.of(entity));
        assertThrows(ValidationException.class, () -> robotService.updateRobotStatus(id, "IN_PROGRESS"));
    }

    // Updated test: now updating to IDLE should succeed by clearing the order.
    @Test
    void testUpdateRobotStatus_IdleWithOrder() throws EntityNotFoundException {
        String id = "1";
        RobotEntity entity = new RobotEntity("IN_PROGRESS", "order1", 5, null);
        entity.setId(id);
        when(robotRepository.findById(id)).thenReturn(Optional.of(entity));
        when(robotRepository.save(any(RobotEntity.class))).thenReturn(entity);

        RobotDTO updated = robotService.updateRobotStatus(id, "IDLE");
        assertNotNull(updated);
        assertEquals("IDLE", updated.getStatus());
        assertNull(updated.getCurrentOrderId());
    }

    @Test
    void testUpdateRobotStatus_Successful() throws EntityNotFoundException {
        String id = "1";
        // Initially, robot is IN_PROGRESS with a valid order.
        RobotEntity entity = new RobotEntity("IN_PROGRESS", "order1", 5, null);
        entity.setId(id);
        when(robotRepository.findById(id)).thenReturn(Optional.of(entity));
        when(robotRepository.save(any(RobotEntity.class))).thenReturn(entity);

        // Update to COMPLETED should clear the order.
        RobotDTO updatedRobot = robotService.updateRobotStatus(id, "COMPLETED");
        assertNotNull(updatedRobot);
        assertEquals("COMPLETED", updatedRobot.getStatus());
        assertNull(updatedRobot.getCurrentOrderId());
    }

    @Test
    void testUpdateCompletedOrders_Negative() {
        String id = "1";
        assertThrows(ValidationException.class, () -> robotService.updateCompletedOrders(id, -5));
    }

    @Test
    void testUpdateCompletedOrders_Successful() throws EntityNotFoundException {
        String id = "1";
        RobotEntity entity = new RobotEntity("IDLE", null, 5, null);
        entity.setId(id);
        when(robotRepository.findById(id)).thenReturn(Optional.of(entity));
        when(robotRepository.save(any(RobotEntity.class))).thenReturn(entity);

        RobotDTO updated = robotService.updateCompletedOrders(id, 10);
        assertNotNull(updated);
        assertEquals(10, updated.getCompletedOrders());
    }

    @Test
    void testDeleteRobot_ExistingEntity() throws EntityNotFoundException {
        String id = "1";
        RobotEntity entity = new RobotEntity("idle", null, 5, null);
        entity.setId(id);
        when(robotRepository.findById(id)).thenReturn(Optional.of(entity));
        robotService.deleteRobot(id);
        verify(robotRepository, times(1)).delete(entity);
    }

    @Test
    void testDeleteRobot_NonExistingEntity() {
        String id = "NonExistingId";
        when(robotRepository.findById(id)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> robotService.deleteRobot(id));
    }
}
