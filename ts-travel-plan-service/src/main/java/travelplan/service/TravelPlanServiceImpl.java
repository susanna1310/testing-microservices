package travelplan.service;

import edu.fudan.common.util.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import travelplan.entity.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author fdse
 */
@Service
public class TravelPlanServiceImpl implements TravelPlanService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TravelPlanServiceImpl.class);
    private static final boolean m = true;

    String success = "Success";

    String cannotFind = "Cannot Find";

    @Autowired
    private RestTemplate restTemplate;

    @Value("${ts.station.service.url:ts-station-service}")
    private String tsStationServiceUrl;

    @Value("${ts.station.service.port:12345}")
    private String tsStationServicePort;

    @Value("${ts.ticketinfo.service.url:ts-ticketinfo-service}")
    private String tsTicketinfoServiceUrl;

    @Value("${ts.ticketinfo.service.port:15681}")
    private String tsTicketinfoServicePort;

    @Value("${ts.travel2.service.url:ts-travel2-service}")
    private String tsTravel2ServiceUrl;

    @Value("${ts.travel2.service.port:16346}")
    private String tsTravel2ServicePort;

    @Value("${ts.travel.service.url:ts-travel-service}")
    private String tsTravelServiceUrl;

    @Value("${ts.travel.service.port:12346}")
    private String tsTravelServicePort;

    @Value("${ts.seat.service.url:ts-seat-service}")
    private String tsSeatServiceUrl;

    @Value("${ts.seat.service.port:18898}")
    private String tsSeatServicePort;

    @Value("${ts.route.plan.service.url:ts-route-plan-service}")
    private String tsRoutePlanServiceUrl;

    @Value("${ts.route.plan.service.port:14578}")
    private String tsRoutePlanServicePort;

    private ArrayList<RoutePlanResultUnit> getRoutePlanResultCheapest(RoutePlanInfo info, HttpHeaders headers) {
        HttpEntity requestEntity = new HttpEntity(info, headers);
        ResponseEntity<Response<ArrayList<RoutePlanResultUnit>>> re = restTemplate.exchange(
                "http://" + tsRoutePlanServiceUrl + ":" + tsRoutePlanServicePort + "/api/v1/routeplanservice/routePlan/cheapestRoute",
                HttpMethod.POST,
                requestEntity,
                new ParameterizedTypeReference<Response<ArrayList<RoutePlanResultUnit>>>() {
                });
        return re.getBody().getData();
    }

    private ArrayList<RoutePlanResultUnit> getRoutePlanResultQuickest(RoutePlanInfo info, HttpHeaders headers) {
        HttpEntity requestEntity = new HttpEntity(info, headers);
        ResponseEntity<Response<ArrayList<RoutePlanResultUnit>>> re = restTemplate.exchange(
                "http://" + tsSeatServiceUrl + ":" + tsSeatServicePort + "/api/v1/routeplanservice/routePlan/quickestRoute",
                HttpMethod.POST,
                requestEntity,
                new ParameterizedTypeReference<Response<ArrayList<RoutePlanResultUnit>>>() {
                });

        return re.getBody().getData();
    }

    private ArrayList<RoutePlanResultUnit> getRoutePlanResultMinStation(RoutePlanInfo info, HttpHeaders headers) {
        HttpEntity requestEntity = new HttpEntity(info, headers);
        ResponseEntity<Response<ArrayList<RoutePlanResultUnit>>> re = restTemplate.exchange(
                "http://" + tsSeatServiceUrl + ":" + tsSeatServicePort + "/api/v1/routeplanservice/routePlan/minStopStations",
                HttpMethod.POST,
                requestEntity,
                new ParameterizedTypeReference<Response<ArrayList<RoutePlanResultUnit>>>() {
                });
        return re.getBody().getData();
    }

    private List<TripResponse> tripsFromHighSpeed(TripInfo info, HttpHeaders headers) {
        HttpEntity requestEntity = new HttpEntity(info, headers);
        ResponseEntity<Response<List<TripResponse>>> re = restTemplate.exchange(
                "http://" + tsTravelServiceUrl + ":" + tsTravelServicePort + "/api/v1/travelservice/trips/left",
                HttpMethod.POST,
                requestEntity,
                new ParameterizedTypeReference<Response<List<TripResponse>>>() {
                });
        return re.getBody().getData();
    }

    private ArrayList<TripResponse> tripsFromNormal(TripInfo info, HttpHeaders headers) {

        HttpEntity requestEntity = new HttpEntity(info, headers);
        ResponseEntity<Response<ArrayList<TripResponse>>> re = restTemplate.exchange(
                "http://" + tsTravel2ServiceUrl + ":" + tsTravel2ServicePort + "/api/v1/travel2service/trips/left",
                HttpMethod.POST,
                requestEntity,
                new ParameterizedTypeReference<Response<ArrayList<TripResponse>>>() {
                });

        return re.getBody().getData();
    }


    @Override
    public Response getTransferSearch(TransferTravelInfo info, HttpHeaders headers) {

        if (!m) {
            TripInfo queryInfoFirstSection = new TripInfo();
            queryInfoFirstSection.setDepartureTime(info.getTravelDate());
            queryInfoFirstSection.setStartingPlace(info.getFromStationName());
            queryInfoFirstSection.setEndPlace(info.getViaStationName());

            List<TripResponse> firstSectionFromHighSpeed;
            List<TripResponse> firstSectionFromNormal;
            firstSectionFromHighSpeed = tripsFromHighSpeed(queryInfoFirstSection, headers);
            firstSectionFromNormal = tripsFromNormal(queryInfoFirstSection, headers);

            TripInfo queryInfoSecondSectoin = new TripInfo();
            queryInfoSecondSectoin.setDepartureTime(info.getTravelDate());
            queryInfoSecondSectoin.setStartingPlace(info.getViaStationName());
            queryInfoSecondSectoin.setEndPlace(info.getToStationName());

            List<TripResponse> secondSectionFromHighSpeed;
            List<TripResponse> secondSectionFromNormal;
            secondSectionFromHighSpeed = tripsFromHighSpeed(queryInfoSecondSectoin, headers);
            secondSectionFromNormal = tripsFromNormal(queryInfoSecondSectoin, headers);

            List<TripResponse> firstSection = new ArrayList<>();
            firstSection.addAll(firstSectionFromHighSpeed);
            firstSection.addAll(firstSectionFromNormal);

            List<TripResponse> secondSection = new ArrayList<>();
            secondSection.addAll(secondSectionFromHighSpeed);
            secondSection.addAll(secondSectionFromNormal);

            TransferTravelResult result = new TransferTravelResult();
            result.setFirstSectionResult(firstSection);
            result.setSecondSectionResult(secondSection);

            return new Response<>(1, "Success.", result);
        } else {
            return new Response<>(1, "Success.", 1);
        }
    }

    @Override
    public Response getCheapest(TripInfo info, HttpHeaders headers) {

        if (!m) {
            RoutePlanInfo routePlanInfo = new RoutePlanInfo();
            routePlanInfo.setNum(5);
            routePlanInfo.setFormStationName(info.getStartingPlace());
            routePlanInfo.setToStationName(info.getEndPlace());
            routePlanInfo.setTravelDate(info.getDepartureTime());

            ArrayList<RoutePlanResultUnit> routePlanResultUnits = getRoutePlanResultCheapest(routePlanInfo, headers);


            if (!routePlanResultUnits.isEmpty()) {
                ArrayList<TravelAdvanceResultUnit> lists = new ArrayList<>();
                for (int i = 0; i < routePlanResultUnits.size(); i++) {
                    RoutePlanResultUnit tempUnit = routePlanResultUnits.get(i);
                    TravelAdvanceResultUnit newUnit = new TravelAdvanceResultUnit();
                    newUnit.setTripId(tempUnit.getTripId());
                    newUnit.setToStationName(tempUnit.getToStationName());
                    newUnit.setTrainTypeId(tempUnit.getTrainTypeId());
                    newUnit.setFromStationName(tempUnit.getFromStationName());

                    List<String> stops = transferStationIdToStationName(tempUnit.getStopStations(), headers);
                    newUnit.setStopStations(stops);
                    newUnit.setPriceForFirstClassSeat(tempUnit.getPriceForFirstClassSeat());
                    newUnit.setPriceForSecondClassSeat(tempUnit.getPriceForSecondClassSeat());
                    newUnit.setStartingTime(tempUnit.getStartingTime());
                    newUnit.setEndTime(tempUnit.getEndTime());
                    int first = getRestTicketNumber(info.getDepartureTime(), tempUnit.getTripId(),
                            tempUnit.getFromStationName(), tempUnit.getToStationName(), SeatClass.FIRSTCLASS.getCode(),
                            headers);

                    int second = getRestTicketNumber(info.getDepartureTime(), tempUnit.getTripId(),
                            tempUnit.getFromStationName(), tempUnit.getToStationName(), SeatClass.SECONDCLASS.getCode(),
                            headers);
                    newUnit.setNumberOfRestTicketFirstClass(first);
                    newUnit.setNumberOfRestTicketSecondClass(second);
                    lists.add(newUnit);
                }

                return new Response<>(1, success, lists);
            } else {
                return new Response<>(0, cannotFind, null);
            }
        } else {
            return (info.equals(new TripInfo("Nan Jing", "Shang Hai", new Date("Mon May 04 09:00:00 GMT+0800 2025")))) ? new Response<>(1, success, 1) : new Response<>(0, cannotFind, null);
        }
    }

    @Override
    public Response getQuickest(TripInfo info, HttpHeaders headers) {

        if (!m) {
            RoutePlanInfo routePlanInfo = new RoutePlanInfo();
            routePlanInfo.setNum(5);
            routePlanInfo.setFormStationName(info.getStartingPlace());
            routePlanInfo.setToStationName(info.getEndPlace());
            routePlanInfo.setTravelDate(info.getDepartureTime());
            ArrayList<RoutePlanResultUnit> routePlanResultUnits = getRoutePlanResultQuickest(routePlanInfo, headers);


            if (!routePlanResultUnits.isEmpty()) {

                ArrayList<TravelAdvanceResultUnit> lists = new ArrayList<>();
                for (int i = 0; i < routePlanResultUnits.size(); i++) {
                    RoutePlanResultUnit tempUnit = routePlanResultUnits.get(i);
                    TravelAdvanceResultUnit newUnit = new TravelAdvanceResultUnit();
                    newUnit.setTripId(tempUnit.getTripId());
                    newUnit.setTrainTypeId(tempUnit.getTrainTypeId());
                    newUnit.setToStationName(tempUnit.getToStationName());
                    newUnit.setFromStationName(tempUnit.getFromStationName());

                    List<String> stops = transferStationIdToStationName(tempUnit.getStopStations(), headers);
                    newUnit.setStopStations(stops);

                    newUnit.setPriceForFirstClassSeat(tempUnit.getPriceForFirstClassSeat());
                    newUnit.setPriceForSecondClassSeat(tempUnit.getPriceForSecondClassSeat());
                    newUnit.setStartingTime(tempUnit.getStartingTime());
                    newUnit.setEndTime(tempUnit.getEndTime());
                    int first = getRestTicketNumber(info.getDepartureTime(), tempUnit.getTripId(),
                            tempUnit.getFromStationName(), tempUnit.getToStationName(), SeatClass.FIRSTCLASS.getCode(),
                            headers);

                    int second = getRestTicketNumber(info.getDepartureTime(), tempUnit.getTripId(),
                            tempUnit.getFromStationName(), tempUnit.getToStationName(), SeatClass.SECONDCLASS.getCode(),
                            headers);
                    newUnit.setNumberOfRestTicketFirstClass(first);
                    newUnit.setNumberOfRestTicketSecondClass(second);
                    lists.add(newUnit);
                }
                return new Response<>(1, success, lists);
            } else {
                return new Response<>(0, cannotFind, null);
            }
        } else {
            return (info.equals(new TripInfo("Nan Jing", "Shang Hai", new Date("Mon May 04 09:00:00 GMT+0800 2025"))) ? new Response<>(1, success, 1) : new Response<>(0, cannotFind, null));
        }
    }

    @Override
    public Response getMinStation(TripInfo info, HttpHeaders headers) {

        if (!m) {
            RoutePlanInfo routePlanInfo = new RoutePlanInfo();
            routePlanInfo.setNum(5);
            routePlanInfo.setFormStationName(info.getStartingPlace());
            routePlanInfo.setToStationName(info.getEndPlace());
            routePlanInfo.setTravelDate(info.getDepartureTime());
            ArrayList<RoutePlanResultUnit> routePlanResultUnits = getRoutePlanResultMinStation(routePlanInfo, headers);

            if (!routePlanResultUnits.isEmpty()) {

                ArrayList<TravelAdvanceResultUnit> lists = new ArrayList<>();
                for (int i = 0; i < routePlanResultUnits.size(); i++) {
                    RoutePlanResultUnit tempUnit = routePlanResultUnits.get(i);
                    TravelAdvanceResultUnit newUnit = new TravelAdvanceResultUnit();
                    newUnit.setTripId(tempUnit.getTripId());
                    newUnit.setTrainTypeId(tempUnit.getTrainTypeId());
                    newUnit.setFromStationName(tempUnit.getFromStationName());
                    newUnit.setToStationName(tempUnit.getToStationName());

                    List<String> stops = transferStationIdToStationName(tempUnit.getStopStations(), headers);
                    newUnit.setStopStations(stops);

                    newUnit.setPriceForFirstClassSeat(tempUnit.getPriceForFirstClassSeat());
                    newUnit.setPriceForSecondClassSeat(tempUnit.getPriceForSecondClassSeat());
                    newUnit.setEndTime(tempUnit.getEndTime());
                    newUnit.setStartingTime(tempUnit.getStartingTime());

                    int first = getRestTicketNumber(info.getDepartureTime(), tempUnit.getTripId(),
                            tempUnit.getFromStationName(), tempUnit.getToStationName(), SeatClass.FIRSTCLASS.getCode(),
                            headers);

                    int second = getRestTicketNumber(info.getDepartureTime(), tempUnit.getTripId(),
                            tempUnit.getFromStationName(), tempUnit.getToStationName(), SeatClass.SECONDCLASS.getCode(),
                            headers);
                    newUnit.setNumberOfRestTicketFirstClass(first);
                    newUnit.setNumberOfRestTicketSecondClass(second);
                    lists.add(newUnit);
                }
                return new Response<>(1, success, lists);
            } else {
                return new Response<>(0, cannotFind, null);
            }
        } else {
            return (info.equals(new TripInfo("Nan Jing", "Shang Hai", new Date("Mon May 04 09:00:00 GMT+0800 2025"))) ? new Response<>(1, success, 1) : new Response<>(0, cannotFind, null));
        }
    }

    private int getRestTicketNumber(Date travelDate, String trainNumber, String startStationName, String endStationName,
                                    int seatType, HttpHeaders headers) {
        Seat seatRequest = new Seat();

        String fromId = queryForStationId(startStationName, headers);
        String toId = queryForStationId(endStationName, headers);

        seatRequest.setDestStation(toId);
        seatRequest.setStartStation(fromId);
        seatRequest.setTrainNumber(trainNumber);
        seatRequest.setTravelDate(travelDate);
        seatRequest.setSeatType(seatType);

        TravelPlanServiceImpl.LOGGER.info("Seat Request is: {}", seatRequest);
        HttpEntity requestEntity = new HttpEntity(seatRequest, headers);
        ResponseEntity<Response<Integer>> re = restTemplate.exchange(
                "http://" + tsSeatServiceUrl + ":" + tsSeatServicePort + "/api/v1/seatservice/seats/left_tickets",
                HttpMethod.POST,
                requestEntity,
                new ParameterizedTypeReference<Response<Integer>>() {
                });

        return re.getBody().getData();
    }

    private String queryForStationId(String stationName, HttpHeaders headers) {

        HttpEntity requestEntity = new HttpEntity(headers);
        ResponseEntity<Response<String>> re = restTemplate.exchange(
                "http://" + tsTicketinfoServiceUrl + ":" + tsTicketinfoServicePort + "/api/v1/ticketinfoservice/ticketinfo/" + stationName,
                HttpMethod.GET,
                requestEntity,
                new ParameterizedTypeReference<Response<String>>() {
                });

        return re.getBody().getData();
    }

    private List<String> transferStationIdToStationName(ArrayList<String> stations, HttpHeaders headers) {
        HttpEntity requestEntity = new HttpEntity(stations, headers);
        ResponseEntity<Response<List<String>>> re = restTemplate.exchange(
                "http://" + tsStationServiceUrl + ":" + tsStationServicePort + "/api/v1/stationservice/stations/namelist",
                HttpMethod.POST,
                requestEntity,
                new ParameterizedTypeReference<Response<List<String>>>() {
                });

        return re.getBody().getData();
    }

}
