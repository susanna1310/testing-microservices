package travel.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import travel.entity.TravelInfo;
import travel.entity.Trip;
import travel.entity.TripId;
import travel.entity.Type;
import travel.repository.TripRepository;

import java.util.Date;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
public abstract class BaseComponentTest {

    @Container
    private static final MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:latest")
            .withExposedPorts(27017);
    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected TripRepository tripRepository;
    @Autowired
    protected ObjectMapper objectMapper;

    @BeforeAll
    static void beforeAll() {
        System.setProperty("spring.data.mongodb.host", mongoDBContainer.getHost());
        System.setProperty("spring.data.mongodb.port", mongoDBContainer.getMappedPort(27017).toString());
        mongoDBContainer.start();
    }

    @BeforeEach
    public void setUp() {
        tripRepository.deleteAll();
    }


    protected Trip createSampleTrip() {
        TripId tripId = new TripId(Type.D, "12355");
        Trip trip = new Trip(tripId, "2", "123", "321", "21", new Date("Mon May 04 09:00:00 GMT+0800 2013"), new Date("Mon May 04 15:51:52 GMT+0800 2013"));
        trip.setRouteId("92708982-77af-4318-be25-57ccb0ff69ad");
        return trip;
    }

    protected TravelInfo createSampleTravelInfo() {
        TravelInfo info = new TravelInfo();

        info.setTripId("D12355");
        info.setTrainTypeId("2");
        info.setRouteId("92708982-77af-4318-be25-57ccb0ff69ad");
        info.setStartingStationId("123");
        info.setStationsId("321");
        info.setTerminalStationId("21");
        info.setStartingTime(new Date("Mon May 04 09:00:00 GMT+0800 2013"));
        info.setEndTime(new Date("Mon May 04 15:51:52 GMT+0800 2013"));
        return info;
    }


}
