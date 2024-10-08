package fdse.basic.service;

import edu.fudan.common.util.JsonUtils;
import edu.fudan.common.util.Response;
import fdse.basic.entity.*;
import lombok.extern.slf4j.Slf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;

/**
 * @author fdse
 */
@Service
@Slf4j
public class BasicServiceImpl implements BasicService
{
    private static final Logger LOGGER = LoggerFactory.getLogger(BasicServiceImpl.class);

    @Autowired
    private RestTemplate restTemplate;

    @Value("${ts.station.service.url:ts-station-service}")
    private String tsStationServiceUrl;

    @Value("${ts.station.service.port:12345}")
    private String tsStationServicePort;

    @Value("${ts.train.service.url:ts-train-service}")
    private String tsTrainServiceUrl;

    @Value("${ts.train.service.port:14567}")
    private String tsTrainServicePort;

    @Value("${ts.route.service.url:ts-route-service}")
    private String tsRouteServiceUrl;

    @Value("${ts.route.service.port:11178}")
    private String tsRouteServicePort;

    @Value("${ts.price.service.url:ts-price-service}")
    private String tsPriceServiceUrl;

    @Value("${ts.price.service.port:16579}")
    private String tsPriceServicePort;



    @Override
    public Response queryForTravel(Travel info, HttpHeaders headers)
    {

        Response response = new Response<>();
        TravelResult result = new TravelResult();
        result.setStatus(true);
        response.setStatus(1);
        response.setMsg("Success");
        boolean startingPlaceExist = checkStationExists(info.getStartingPlace(), headers);
        boolean endPlaceExist = checkStationExists(info.getEndPlace(), headers);
        if (!startingPlaceExist || !endPlaceExist) {
            result.setStatus(false);
            response.setStatus(0);
            response.setMsg("Start place or end place not exist!");
        }

        TrainType trainType = queryTrainType(info.getTrip().getTrainTypeId(), headers);
        if (trainType == null) {
            BasicServiceImpl.LOGGER.info("traintype doesn't exist");
            result.setStatus(false);
            response.setStatus(0);
            response.setMsg("Train type doesn't exist");
        } else {
            result.setTrainType(trainType);
        }

        String routeId = info.getTrip().getRouteId();
        String trainTypeString = "";
        if (trainType != null) {
            trainTypeString = trainType.getId();
        }
        Route route = getRouteByRouteId(routeId, headers);
        PriceConfig priceConfig = queryPriceConfigByRouteIdAndTrainType(routeId, trainTypeString, headers);

        String startingPlaceId = (String) queryForStationId(info.getStartingPlace(), headers).getData();
        String endPlaceId = (String) queryForStationId(info.getEndPlace(), headers).getData();

        log.info("startingPlaceId : " + startingPlaceId + "endPlaceId : " + endPlaceId);

        int indexStart = 0;
        int indexEnd = 0;
        if (route != null) {
            indexStart = route.getStations().indexOf(startingPlaceId);
            indexEnd = route.getStations().indexOf(endPlaceId);
        }

        log.info("indexStart : " + indexStart + " __ " + "indexEnd : " + indexEnd);
        if (route != null) {
            log.info("route.getDistances().size : " + route.getDistances().size());
        }
        HashMap<String, String> prices = new HashMap<>();
        try {
            int distance = 0;
            if (route != null) {
                distance = route.getDistances().get(indexEnd) - route.getDistances().get(indexStart);
            }

            /**
             * We need the price Rate and distance (starting station).
             */
            double priceForEconomyClass = distance * priceConfig.getBasicPriceRate();
            double priceForConfortClass = distance * priceConfig.getFirstClassPriceRate();
            prices.put("economyClass", "" + priceForEconomyClass);
            prices.put("confortClass", "" + priceForConfortClass);
        } catch (Exception e) {
            prices.put("economyClass", "95.0");
            prices.put("confortClass", "120.0");
        }
        result.setPrices(prices);
        result.setPercent(1.0);
        response.setData(result);
        return response;
    }

    @Override
    public Response queryForStationId(String stationName, HttpHeaders headers)
    {
        BasicServiceImpl.LOGGER.info("[Basic Information Service][Query For Station Id] Station Id: {}", stationName);
        HttpEntity requestEntity = new HttpEntity(headers);
        ResponseEntity<Response> re = restTemplate.exchange(
            "http://" + tsStationServiceUrl + ":" + tsStationServicePort + "/api/v1/stationservice/stations/id/" + stationName,
            HttpMethod.GET,
            requestEntity,
            Response.class);
        return re.getBody();
    }

    public boolean checkStationExists(String stationName, HttpHeaders headers)
    {
        BasicServiceImpl.LOGGER.info("[Basic Information Service][Check Station Exists] Station Name: {}", stationName);
        HttpEntity requestEntity = new HttpEntity(headers);
        ResponseEntity<Response> re = restTemplate.exchange(
            "http://" + tsStationServiceUrl + ":" + tsStationServicePort + "/api/v1/stationservice/stations/id/" + stationName,
            HttpMethod.GET,
            requestEntity,
            Response.class);
        Response exist = re.getBody();

        return exist.getStatus() == 1;
    }

    public TrainType queryTrainType(String trainTypeId, HttpHeaders headers)
    {
        BasicServiceImpl.LOGGER.info("[Basic Information Service][Query Train Type] Train Type: {}", trainTypeId);
        HttpEntity requestEntity = new HttpEntity(headers);
        ResponseEntity<Response> re = restTemplate.exchange(
            "http://" + tsTrainServiceUrl + ":" + tsTrainServicePort + "/api/v1/trainservice/trains/" + trainTypeId,
            HttpMethod.GET,
            requestEntity,
            Response.class);
        Response response = re.getBody();

        return JsonUtils.conveterObject(response.getData(), TrainType.class);
    }

    private Route getRouteByRouteId(String routeId, HttpHeaders headers)
    {
        BasicServiceImpl.LOGGER.info("[Basic Information Service][Get Route By Id] Route ID：{}", routeId);
        HttpEntity requestEntity = new HttpEntity(headers);
        ResponseEntity<Response> re = restTemplate.exchange(
            "http://" + tsRouteServiceUrl + ":" + tsRouteServicePort + "/api/v1/routeservice/routes/" + routeId,
            HttpMethod.GET,
            requestEntity,
            Response.class);
        Response result = re.getBody();
        if (result.getStatus() == 0) {
            BasicServiceImpl.LOGGER.info("[Basic Information Service][Get Route By Id] Fail. {}", result.getMsg());
            return null;
        } else {
            BasicServiceImpl.LOGGER.info("[Basic Information Service][Get Route By Id] Success.");
            return JsonUtils.conveterObject(result.getData(), Route.class);
        }
    }

    private PriceConfig queryPriceConfigByRouteIdAndTrainType(String routeId, String trainType, HttpHeaders headers)
    {
        BasicServiceImpl.LOGGER.info("[Basic Information Service][Query For Price Config] RouteId: {} ,TrainType: {}",
            routeId, trainType);
        HttpEntity requestEntity = new HttpEntity(null, headers);
        ResponseEntity<Response> re = restTemplate.exchange(
            "http://" + tsPriceServiceUrl + ":" + tsPriceServicePort + "/api/v1/priceservice/prices/" + routeId + "/" + trainType,
            HttpMethod.GET,
            requestEntity,
            Response.class);
        Response result = re.getBody();

        BasicServiceImpl.LOGGER.info("Response Resutl to String {}", result.toString());
        return JsonUtils.conveterObject(result.getData(), PriceConfig.class);
    }
}
