package notification.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import notification.entity.NotifyInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters=false)
public abstract class BaseComponentTest {
    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected NotifyInfo createSampleNotifyInfo() {
        NotifyInfo notifyInfo = new NotifyInfo();
        notifyInfo.setEmail("test@test.com");
        notifyInfo.setOrderNumber("1");
        notifyInfo.setUsername("user");
        notifyInfo.setStartingPlace("start");
        notifyInfo.setEndPlace("end");
        notifyInfo.setStartingTime("startTime");
        notifyInfo.setDate("date");
        notifyInfo.setSeatClass("seatClass");
        notifyInfo.setSeatNumber("1");
        notifyInfo.setPrice("1");
        return notifyInfo;
    }
}
