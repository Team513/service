package ro.unibuc.hello.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ro.unibuc.hello.data.RobotEntity;
import ro.unibuc.hello.data.RobotRepository;
import ro.unibuc.hello.data.RobotStatus;
import ro.unibuc.hello.dto.RobotDTO;
import ro.unibuc.hello.exception.EntityNotFoundException;
import ro.unibuc.hello.exception.ValidationException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RobotService {

    private static final Set<String> ALLOWED_STATUSES = Set.of("ACTIVE", "INACTIVE", "IN_PROGRESS", "IDLE", "COMPLETED", "ERROR");


    @Autowired
    private RobotRepository robotRepository;

    public List<RobotDTO> getAllRobots() {
        List<RobotEntity> entities = robotRepository.findAll();
        return entities.stream()
                .map(entity -> new RobotDTO(
                        entity.getId(), 
                        entity.getStatus(), 
                        entity.getCurrentOrderId(), 
                        entity.getCompletedOrders(), 
                        entity.getErrors()))
                .collect(Collectors.toList());
    }

    public RobotDTO getRobotById(String id) throws EntityNotFoundException {
        RobotEntity entity = robotRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Robot with ID " + id + " not found"));
        return new RobotDTO(
                entity.getId(), 
                entity.getStatus(), 
                entity.getCurrentOrderId(), 
                entity.getCompletedOrders(), 
                entity.getErrors());
    }

    public RobotDTO createRobot(RobotDTO robotDTO) {
        validateRobot(robotDTO);

        RobotEntity robot = new RobotEntity(
                robotDTO.getStatus(),
                robotDTO.getCurrentOrderId(),
                robotDTO.getCompletedOrders(),
                robotDTO.getErrors()
        );
        robotRepository.save(robot);
        return new RobotDTO(
                robot.getId(), 
                robot.getStatus(), 
                robot.getCurrentOrderId(), 
                robot.getCompletedOrders(), 
                robot.getErrors());
    }

    public RobotDTO updateRobotStatus(String id, String newStatus) throws EntityNotFoundException {
        RobotEntity robot = robotRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Robot with ID " + id + " not found"));

        if ("IN_PROGRESS".equalsIgnoreCase(newStatus)) {
            if (robot.getCurrentOrderId() == null || robot.getCurrentOrderId().isBlank()) {
                throw new ValidationException("Robot with status IN_PROGRESS must have a current order ID");
            }
        }
        else if ("IDLE".equalsIgnoreCase(newStatus) || "COMPLETED".equalsIgnoreCase(newStatus)) {
            robot.setCurrentOrderId(null);
        }

        robot.setStatus(newStatus);
        robot.setLastUpdatedAt(java.time.LocalDateTime.now());
        robotRepository.save(robot);
        return new RobotDTO(
                robot.getId(), 
                robot.getStatus(), 
                robot.getCurrentOrderId(), 
                robot.getCompletedOrders(), 
                robot.getErrors());
    }

    public RobotDTO updateCompletedOrders(String id, Integer completedOrders) throws EntityNotFoundException {
        if (completedOrders == null || completedOrders < 0) {
            throw new ValidationException("Completed orders cannot be negative");
        }
        RobotEntity robot = robotRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Robot with ID " + id + " not found"));
        robot.setCompletedOrders(completedOrders);
        robot.setLastUpdatedAt(java.time.LocalDateTime.now());
        robotRepository.save(robot);
        return new RobotDTO(
                robot.getId(), 
                robot.getStatus(), 
                robot.getCurrentOrderId(), 
                robot.getCompletedOrders(), 
                robot.getErrors());
    }

    public void deleteRobot(String id) throws EntityNotFoundException {
        RobotEntity robot = robotRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Robot with ID " + id + " not found"));
        robotRepository.delete(robot);
    }


    private void validateRobot(RobotDTO robotDTO) {
        // Verify that the provided status is allowed
        if (robotDTO.getStatus() == null || !ALLOWED_STATUSES.contains(robotDTO.getStatus().toUpperCase())) {
            throw new ValidationException("Invalid status: " + robotDTO.getStatus());
        }
        
        if (robotDTO.getCompletedOrders() != null && robotDTO.getCompletedOrders() < 0) {
            throw new ValidationException("Completed orders cannot be negative");
        }
        
        if ("IN_PROGRESS".equalsIgnoreCase(robotDTO.getStatus())) {
            if (robotDTO.getCurrentOrderId() == null || robotDTO.getCurrentOrderId().isBlank()) {
                throw new ValidationException("Robot with status IN_PROGRESS must have a current order ID");
            }
        }
        
        if (("IDLE".equalsIgnoreCase(robotDTO.getStatus()) || "COMPLETED".equalsIgnoreCase(robotDTO.getStatus()))
                && robotDTO.getCurrentOrderId() != null) {
            throw new ValidationException("Robot with status IDLE or COMPLETED should not have a current order ID");
        }
    }
}
