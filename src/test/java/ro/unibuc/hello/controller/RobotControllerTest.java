package ro.unibuc.hello.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ro.unibuc.hello.dto.RobotDTO;
import ro.unibuc.hello.service.RobotService;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RobotControllerTest {

    @Mock
    private RobotService robotService;

    @InjectMocks
    private RobotController robotController;

    private MockMvc mockMvc;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(robotController).build();
    }

    @Test
    void testGetAllRobots() throws Exception {
        List<RobotDTO> robots = Arrays.asList(
                new RobotDTO("1", "IDLE", null, 10, "none"),
                new RobotDTO("2", "IN_PROGRESS", "order123", 5, "none")
        );
        when(robotService.getAllRobots()).thenReturn(robots);

        mockMvc.perform(get("/robots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("1"))
                .andExpect(jsonPath("$[0].status").value("IDLE"))
                .andExpect(jsonPath("$[1].id").value("2"))
                .andExpect(jsonPath("$[1].status").value("IN_PROGRESS"));
    }

    @Test
    void testCreateRobot() throws Exception {
        RobotDTO newRobot = new RobotDTO(null, "IDLE", null, 0, "none");
        RobotDTO createdRobot = new RobotDTO("1", "IDLE", null, 0, "none");
        when(robotService.createRobot(any(RobotDTO.class))).thenReturn(createdRobot);

        mockMvc.perform(post("/robots")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"IDLE\",\"currentOrderId\":null,\"completedOrders\":0,\"errors\":\"none\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.status").value("IDLE"));
    }

    @Test
    void testUpdateRobotStatus_ExistingEntity() throws Exception {
        String id = "1";
        String status = "COMPLETED";
        RobotDTO updatedRobot = new RobotDTO(id, status, null, 10, "none");
        when(robotService.updateRobotStatus(eq(id), eq(status))).thenReturn(updatedRobot);

        mockMvc.perform(put("/robots/{id}/status", id)
                .param("status", status))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.status").value(status));
    }
    
    @Test
    void testUpdateCompletedOrders_Successful() throws Exception {
        String id = "1";
        RobotDTO updatedRobot = new RobotDTO(id, "IDLE", null, 20, "none");
        when(robotService.updateCompletedOrders(eq(id), eq(20))).thenReturn(updatedRobot);

        mockMvc.perform(put("/robots/{id}/completedOrders", id)
                .param("completedOrders", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.completedOrders").value(20));
    }
}
