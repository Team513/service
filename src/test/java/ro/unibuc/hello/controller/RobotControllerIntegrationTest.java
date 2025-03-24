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
import ro.unibuc.hello.dto.RobotDTO;
import ro.unibuc.hello.service.RobotService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Tag("IntegrationTest")
public class RobotControllerIntegrationTest {

    @Container
    public static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:6.0.20")
            .withExposedPorts(27017)
            .withEnv("MONGO_INITDB_ROOT_USERNAME", "root")
            .withEnv("MONGO_INITDB_ROOT_PASSWORD", "example")
            .withEnv("MONGO_INITDB_DATABASE", "testdb")
            .withCommand("--auth");

    @BeforeAll
    public static void setUpContainer() {
        mongoDBContainer.start();
    }

    @AfterAll
    public static void tearDownContainer() {
        mongoDBContainer.stop();
    }

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        final String MONGO_URL = "mongodb://root:example@localhost:";
        final String PORT = String.valueOf(mongoDBContainer.getMappedPort(27017));
        registry.add("mongodb.connection.url", () -> MONGO_URL + PORT);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RobotService robotService;

    private String robotId1;
    private String robotId2;

    @BeforeEach
    public void cleanUpAndAddTestData() {
        robotService.getAllRobots().forEach(robot -> {
            try {
                robotService.deleteRobot(robot.getId());
            } catch (Exception e) { }
        });
        RobotDTO robot1 = new RobotDTO(null, "IDLE", null, 10, "none");
        RobotDTO robot2 = new RobotDTO(null, "IN_PROGRESS", "order123", 5, "none");

        RobotDTO createdRobot1 = robotService.createRobot(robot1);
        RobotDTO createdRobot2 = robotService.createRobot(robot2);

        robotId1 = createdRobot1.getId();
        robotId2 = createdRobot2.getId();
    }

    @Test
    public void testGetAllRobots() throws Exception {
        mockMvc.perform(get("/robots"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].status").value("IDLE"))
                .andExpect(jsonPath("$[1].status").value("IN_PROGRESS"));
    }

    @Test
    public void testGetRobotById() throws Exception {
        mockMvc.perform(get("/robots/" + robotId1))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(robotId1))
                .andExpect(jsonPath("$.status").value("IDLE"));
    }

    @Test
    public void testCreateRobot_Successful() throws Exception {
        RobotDTO robot = new RobotDTO(null, "IDLE", null, 0, "none");
        mockMvc.perform(post("/robots")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(robot)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.status").value("IDLE"));

        mockMvc.perform(get("/robots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    public void testCreateRobot_InProgressWithoutOrder() throws Exception {
        RobotDTO robot = new RobotDTO(null, "IN_PROGRESS", null, 5, "none");
        mockMvc.perform(post("/robots")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(robot)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testUpdateRobotStatus_IncorrectConditions() throws Exception {
        // Try updating robotId1 (IDLE without order) to IN_PROGRESS: should fail.
        mockMvc.perform(put("/robots/" + robotId1 + "/status")
                .param("status", "IN_PROGRESS")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testUpdateRobotStatus_Successful() throws Exception {
        // For robotId2, update status from IN_PROGRESS to COMPLETED.
        mockMvc.perform(put("/robots/" + robotId2 + "/status")
                .param("status", "COMPLETED")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    public void testUpdateCompletedOrders_Negative() throws Exception {
        mockMvc.perform(put("/robots/" + robotId1 + "/completedOrders")
                .param("completedOrders", "-5")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testUpdateCompletedOrders_Successful() throws Exception {
        mockMvc.perform(put("/robots/" + robotId1 + "/completedOrders")
                .param("completedOrders", "15")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedOrders").value(15));
    }

    @Test
    public void testDeleteRobot() throws Exception {
        mockMvc.perform(delete("/robots/" + robotId1))
                .andExpect(status().isOk());
        mockMvc.perform(get("/robots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
}
