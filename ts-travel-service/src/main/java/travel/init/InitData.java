package travel.init;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import travel.entity.TravelInfo;
import travel.service.TravelService;

import java.util.Calendar;
import java.util.Date;

/**
 * @author fdse
 */
@Component
public class InitData implements CommandLineRunner {
    @Autowired
    TravelService service;

    String gaoTieOne = "GaoTieOne";

    String shanghai = "shanghai";

    String suzhou = "suzhou";

    String taiyuan = "taiyuan";

    @Override
    public void run(String... args) throws Exception {
        TravelInfo info = new TravelInfo();

        info.setTripId("G1234");
        info.setTrainTypeId(gaoTieOne);
        info.setRouteId("92708982-77af-4318-be25-57ccb0ff69ad");
        info.setStartingStationId(shanghai);
        info.setStationsId(suzhou);
        info.setTerminalStationId(taiyuan);
        info.setStartingTime(new Date("Mon May 04 09:00:00 GMT+0800 2013")); //NOSONAR
        info.setEndTime(new Date("Mon May 04 15:51:52 GMT+0800 2013")); //NOSONAR
        service.create(info, null);

        info.setTripId("G1234");
        info.setTrainTypeId("ZhiDa");
        info.setRouteId("9fc9c261-3263-4bfa-82f8-bb44e06b2f52");
        info.setStartingStationId(shanghai);
        info.setStationsId("nanjing");
        info.setTerminalStationId("beijing");
        info.setStartingTime(new Date("Mon May 04 11:31:52 GMT+0800 2013")); //NOSONAR
        info.setEndTime(new Date("Mon May 04 17:51:52 GMT+0800 2013")); //NOSONAR
        service.create(info, null);

        info.setTripId("G1236");
        info.setTrainTypeId(gaoTieOne);
        info.setRouteId("a3f256c1-0e43-4f7d-9c21-121bf258101f");
        info.setStartingStationId(shanghai);
        info.setStationsId(suzhou);
        info.setTerminalStationId(taiyuan);
        info.setStartingTime(new Date("Mon May 04 14:00:00 GMT+0800 2013")); //NOSONAR
        info.setEndTime(new Date("Mon May 04 20:51:52 GMT+0800 2013")); //NOSONAR
        service.create(info, null);

        info.setTripId("G1237");
        info.setTrainTypeId("GaoTieTwo");
        info.setRouteId("084837bb-53c8-4438-87c8-0321a4d09917");
        info.setStartingStationId(shanghai);
        info.setStationsId(suzhou);
        info.setTerminalStationId(taiyuan);
        info.setStartingTime(new Date("Mon May 04 08:00:00 GMT+0800 2013")); //NOSONAR
        info.setEndTime(new Date("Mon May 04 17:21:52 GMT+0800 2013")); //NOSONAR
        service.create(info, null);

        info.setTripId("D1345");
        info.setTrainTypeId("DongCheOne");
        info.setRouteId("f3d4d4ef-693b-4456-8eed-59c0d717dd08");
        info.setStartingStationId(shanghai);
        info.setStationsId(suzhou);
        info.setTerminalStationId(taiyuan);
        info.setStartingTime(new Date("Mon May 04 07:00:00 GMT+0800 2013")); //NOSONAR
        info.setEndTime(new Date("Mon May 04 19:59:52 GMT+0800 2013")); //NOSONAR
        service.create(info, null);

        insertForPreserve();
        insertForRebook();
    }

    private void insertForPreserve() {
        TravelInfo info = new TravelInfo();
        info.setTripId("G1432");
        info.setTrainTypeId(gaoTieOne);
        info.setRouteId("92708982-77af-4318-be25-57ccb0ff69ad");
        info.setStartingStationId(shanghai);
        info.setStationsId(suzhou);
        info.setTerminalStationId(taiyuan);
        info.setStartingTime(new Date(2026, 0, 1));
        info.setEndTime(new Date(2026, 0, 1));
        service.create(info, null);
    }

    private void insertForRebook() {
        TravelInfo info = new TravelInfo();
        info.setTripId("G8134");
        info.setTrainTypeId("test");
        info.setRouteId("92708982-77af-4318-be25-57ccb0ff69ad");
        info.setStartingStationId(shanghai);
        info.setStationsId(suzhou);
        info.setTerminalStationId(taiyuan);
        info.setStartingTime(new Date("Mon May 04 09:00:00 GMT+0800 2013")); //NOSONAR
        info.setEndTime(new Date("Mon May 04 15:51:52 GMT+0800 2013")); //NOSONAR
        service.create(info, null);
    }
}
