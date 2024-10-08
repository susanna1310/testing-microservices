package preserve.service;

import edu.fudan.common.util.Response;
import lombok.extern.slf4j.Slf4j;

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

import preserve.entity.*;

import java.util.Date;
import java.util.UUID;

/**
 * @author fdse
 */
@Service
@Slf4j
public class PreserveServiceImpl implements PreserveService
{
    private static final Logger LOGGER = LoggerFactory.getLogger(PreserveServiceImpl.class);

    @Autowired
    private RestTemplate restTemplate;

    @Value("${ts.consign.service.url:ts-consign-service}")
    private String tsConsignServiceUrl;

    @Value("${ts.consign.service.port:16111}")
    private String tsConsignServicePort;

    @Value("${ts.food.service.url:ts-food-service}")
    private String tsFoodServiceUrl;

    @Value("${ts.food.service.port:18856}")
    private String tsFoodServicePort;

    @Value("${ts.contacts.service.url:ts-contacts-service}")
    private String tsContactsServiceUrl;

    @Value("${ts.contacts.service.port:12347}")
    private String tsContactsServicePort;

    @Value("${ts.order.service.url:ts-order-service}")
    private String tsOrderServiceUrl;

    @Value("${ts.order.service.port:12031}")
    private String tsOrderServicePort;

    @Value("${ts.travel.service.url:ts-travel-service}")
    private String tsTravelServiceUrl;

    @Value("${ts.travel.service.port:12346}")
    private String tsTravelServicePort;

    @Value("${ts.security.service.url:ts-security-service}")
    private String tsSecurityServiceUrl;

    @Value("${ts.security.service.port:11188}")
    private String tsSecurityServicePort;

    @Value("${ts.station.service.url:ts-station-service}")
    private String tsStationServiceUrl;

    @Value("${ts.station.service.port:12345}")
    private String tsStationServicePort;

    @Value("${ts.assurance.service.url:ts-assurance-service}")
    private String tsAssuranceServiceUrl;

    @Value("${ts.assurance.service.port:18888}")
    private String tsAssuranceServicePort;

    @Value("${ts.user.service.url:ts-user-service}")
    private String tsUserServiceUrl;

    @Value("${ts.user.service.port:12342}")
    private String tsUserServicePort;

    @Value("${ts.notification.service.url:ts-notification-service}")
    private String tsNotificationServiceUrl;

    @Value("${ts.notification.service.port:17853}")
    private String tsNotificationServicePort;

    @Value("${ts.seat.service.url:ts-seat-service}")
    private String tsSeatServiceUrl;

    @Value("${ts.seat.service.port:18898}")
    private String tsSeatServicePort;

    @Value("${ts.ticketinfo.service.url:ts-ticketinfo-service}")
    private String tsTicketinfoServiceUrl;

    @Value("${ts.ticketinfo.service.port:15681}")
    private String tsTicketinfoServicePort;


    @Override
    public Response preserve(OrderTicketsInfo oti, HttpHeaders headers)
    {
        //1.detect ticket scalper
        PreserveServiceImpl.LOGGER.info("[Preserve Service] [Step 1] Check Security");

        Response result = checkSecurity(oti.getAccountId(), headers);
        if (result.getStatus() == 0) {
            return new Response<>(0, result.getMsg(), null);
        }
        PreserveServiceImpl.LOGGER.info("[Preserve Service] [Step 1] Check Security Complete");
        //2.Querying contact information -- modification, mediated by the underlying information micro service
        PreserveServiceImpl.LOGGER.info("[Preserve Service] [Step 2] Find contacts");
        PreserveServiceImpl.LOGGER.info("[Preserve Service] [Step 2] Contacts Id: {}", oti.getContactsId());

        Response<Contacts> gcr = getContactsById(oti.getContactsId(), headers);
        if (gcr.getStatus() == 0) {
            PreserveServiceImpl.LOGGER.info("[Preserve Service][Get Contacts] Fail. {}", gcr.getMsg());
            return new Response<>(0, gcr.getMsg(), null);
        }
        PreserveServiceImpl.LOGGER.info("[Preserve Service][Step 2] Complete");
        //3.Check the info of train and the number of remaining tickets
        PreserveServiceImpl.LOGGER.info("[Preserve Service] [Step 3] Check tickets num");
        TripAllDetailInfo gtdi = new TripAllDetailInfo();

        gtdi.setFrom(oti.getFrom());
        gtdi.setTo(oti.getTo());

        gtdi.setTravelDate(oti.getDate());
        gtdi.setTripId(oti.getTripId());
        PreserveServiceImpl.LOGGER.info("[Preserve Service] [Step 3] TripId: {}", oti.getTripId());
        Response<TripAllDetail> response = getTripAllDetailInformation(gtdi, headers);
        TripAllDetail gtdr = response.getData();
        log.info("TripAllDetail:" + gtdr.toString());
        if (response.getStatus() == 0) {
            PreserveServiceImpl.LOGGER.info("[Preserve Service][Search For Trip Detail Information] {}",
                response.getMsg());
            return new Response<>(0, response.getMsg(), null);
        } else {
            TripResponse tripResponse = gtdr.getTripResponse();
            log.info("TripResponse:" + tripResponse.toString());
            if (oti.getSeatType() == SeatClass.FIRSTCLASS.getCode()) {
                if (tripResponse.getConfortClass() == 0) {
                    PreserveServiceImpl.LOGGER.info("[Preserve Service][Check seat is enough] ");
                    return new Response<>(0, "Seat Not Enough", null);
                }
            } else {
                if (tripResponse.getEconomyClass() == SeatClass.SECONDCLASS.getCode()
                    && tripResponse.getConfortClass() == 0)
                {
                    PreserveServiceImpl.LOGGER.info("[Preserve Service][Check seat is enough] ");
                    return new Response<>(0, "Seat Not Enough", null);
                }
            }
        }
        Trip trip = gtdr.getTrip();
        PreserveServiceImpl.LOGGER.info("[Preserve Service] [Step 3] Tickets Enough");
        //4.send the order request and set the order information
        PreserveServiceImpl.LOGGER.info("[Preserve Service] [Step 4] Do Order");
        Contacts contacts = gcr.getData();
        Order order = new Order();
        UUID orderId = UUID.randomUUID();
        order.setId(orderId);
        order.setTrainNumber(oti.getTripId());
        order.setAccountId(UUID.fromString(oti.getAccountId()));

        String fromStationId = queryForStationId(oti.getFrom(), headers);
        String toStationId = queryForStationId(oti.getTo(), headers);

        order.setFrom(fromStationId);
        order.setTo(toStationId);
        order.setBoughtDate(new Date());
        order.setStatus(OrderStatus.NOTPAID.getCode());
        order.setContactsDocumentNumber(contacts.getDocumentNumber());
        order.setContactsName(contacts.getName());
        order.setDocumentType(contacts.getDocumentType());

        Travel query = new Travel();
        query.setTrip(trip);
        query.setStartingPlace(oti.getFrom());
        query.setEndPlace(oti.getTo());
        query.setDepartureTime(new Date());

        HttpEntity requestEntity = new HttpEntity(query, headers);
        ResponseEntity<Response<TravelResult>> re = restTemplate.exchange(
                "http://"+ tsTicketinfoServiceUrl + ":" + tsTicketinfoServicePort + "/api/v1/ticketinfoservice/ticketinfo",
            HttpMethod.POST,
            requestEntity,
            new ParameterizedTypeReference<Response<TravelResult>>()
            {
            });
        TravelResult resultForTravel = re.getBody().getData();

        order.setSeatClass(oti.getSeatType());
        PreserveServiceImpl.LOGGER.info("[Preserve Service][Order] Order Travel Date: {}", oti.getDate().toString());
        order.setTravelDate(oti.getDate());
        order.setTravelTime(gtdr.getTripResponse().getStartingTime());

        //Dispatch the seat
        if (oti.getSeatType() == SeatClass.FIRSTCLASS.getCode()) {
            Ticket ticket =
                dipatchSeat(oti.getDate(),
                    order.getTrainNumber(), fromStationId, toStationId,
                    SeatClass.FIRSTCLASS.getCode(), headers);
            order.setSeatNumber("" + ticket.getSeatNo());
            order.setSeatClass(SeatClass.FIRSTCLASS.getCode());
            order.setPrice(resultForTravel.getPrices().get("confortClass"));
        } else {
            Ticket ticket =
                dipatchSeat(oti.getDate(),
                    order.getTrainNumber(), fromStationId, toStationId,
                    SeatClass.SECONDCLASS.getCode(), headers);
            order.setSeatClass(SeatClass.SECONDCLASS.getCode());
            order.setSeatNumber("" + ticket.getSeatNo());
            order.setPrice(resultForTravel.getPrices().get("economyClass"));
        }

        PreserveServiceImpl.LOGGER.info("[Preserve Service][Order Price] Price is: {}", order.getPrice());

        Response<Order> cor = createOrder(order, headers);
        if (cor.getStatus() == 0) {
            PreserveServiceImpl.LOGGER.info("[Preserve Service][Create Order Fail] Create Order Fail.  Reason: {}",
                cor.getMsg());
            return new Response<>(0, cor.getMsg(), null);
        }
        PreserveServiceImpl.LOGGER.info("[Preserve Service] [Step 4] Do Order Complete");

        Response returnResponse = new Response<>(1, "Success.", cor.getMsg());
        //5.Check insurance options
        if (oti.getAssurance() == 0) {
            PreserveServiceImpl.LOGGER.info("[Preserve Service][Step 5] Do not need to buy assurance");
        } else {
            Response addAssuranceResult = addAssuranceForOrder(
                oti.getAssurance(), cor.getData().getId().toString(), headers);
            if (addAssuranceResult.getStatus() == 1) {
                PreserveServiceImpl.LOGGER.info("[Preserve Service][Step 5] Buy Assurance Success");
            } else {
                PreserveServiceImpl.LOGGER.info("[Preserve Service][Step 5] Buy Assurance Fail.");
                returnResponse.setMsg("Success.But Buy Assurance Fail.");
            }
        }

        //6.Increase the food order
        if (oti.getFoodType() != 0) {

            FoodOrder foodOrder = new FoodOrder();
            foodOrder.setOrderId(cor.getData().getId());
            foodOrder.setFoodType(oti.getFoodType());
            foodOrder.setFoodName(oti.getFoodName());
            foodOrder.setPrice(oti.getFoodPrice());

            if (oti.getFoodType() == 2) {
                foodOrder.setStationName(oti.getStationName());
                foodOrder.setStoreName(oti.getStoreName());
                PreserveServiceImpl.LOGGER.info("[Food Service]!!!!!!!!!!!!!!!foodstore= {}   {}   {}",
                    foodOrder.getFoodType(), foodOrder.getStationName(), foodOrder.getStoreName());
            }
            Response afor = createFoodOrder(foodOrder, headers);
            if (afor.getStatus() == 1) {
                PreserveServiceImpl.LOGGER.info("[Preserve Service][Step 6] Buy Food Success");
            } else {
                PreserveServiceImpl.LOGGER.info("[Preserve Service][Step 6] Buy Food Fail.");
                returnResponse.setMsg("Success.But Buy Food Fail.");
            }
        } else {
            PreserveServiceImpl.LOGGER.info("[Preserve Service][Step 6] Do not need to buy food");
        }

        //7.add consign
        if (null != oti.getConsigneeName() && !"".equals(oti.getConsigneeName())) {

            Consign consignRequest = new Consign();
            consignRequest.setOrderId(cor.getData().getId());
            consignRequest.setAccountId(cor.getData().getAccountId());
            consignRequest.setHandleDate(oti.getHandleDate());
            consignRequest.setTargetDate(cor.getData().getTravelDate().toString());
            consignRequest.setFrom(cor.getData().getFrom());
            consignRequest.setTo(cor.getData().getTo());
            consignRequest.setConsignee(oti.getConsigneeName());
            consignRequest.setPhone(oti.getConsigneePhone());
            consignRequest.setWeight(oti.getConsigneeWeight());
            consignRequest.setWithin(oti.isWithin());
            log.info("CONSIGN INFO : " + consignRequest.toString());
            Response icresult = createConsign(consignRequest, headers);
            if (icresult.getStatus() == 1) {
                PreserveServiceImpl.LOGGER.info("[Preserve Service][Step 7] Consign Success");
            } else {
                PreserveServiceImpl.LOGGER.info("[Preserve Service][Step 7] Consign Fail.");
                returnResponse.setMsg("Consign Fail.");
            }
        } else {
            PreserveServiceImpl.LOGGER.info("[Preserve Service][Step 7] Do not need to consign");
        }

        //8.send notification
        PreserveServiceImpl.LOGGER.info("[Preserve Service]");

        User getUser = getAccount(order.getAccountId().toString(), headers);

        NotifyInfo notifyInfo = new NotifyInfo();
        notifyInfo.setDate(new Date().toString());

        notifyInfo.setEmail(getUser.getEmail());
        notifyInfo.setStartingPlace(order.getFrom());
        notifyInfo.setEndPlace(order.getTo());
        notifyInfo.setUsername(getUser.getUserName());
        notifyInfo.setSeatNumber(order.getSeatNumber());
        notifyInfo.setOrderNumber(order.getId().toString());
        notifyInfo.setPrice(order.getPrice());
        notifyInfo.setSeatClass(SeatClass.getNameByCode(order.getSeatClass()));
        notifyInfo.setStartingTime(order.getTravelTime().toString());

        sendEmail(notifyInfo, headers);

        return returnResponse;
    }

    public Ticket dipatchSeat(Date date, String tripId, String startStationId, String endStataionId, int seatType,
        HttpHeaders httpHeaders)
    {
        Seat seatRequest = new Seat();
        seatRequest.setTravelDate(date);
        seatRequest.setTrainNumber(tripId);
        seatRequest.setStartStation(startStationId);
        seatRequest.setDestStation(endStataionId);
        seatRequest.setSeatType(seatType);

        HttpEntity requestEntityTicket = new HttpEntity(seatRequest, httpHeaders);
        ResponseEntity<Response<Ticket>> reTicket = restTemplate.exchange(
                "http://"+ tsSeatServiceUrl + ":" + tsSeatServicePort + "/api/v1/seatservice/seats",
            HttpMethod.POST,
            requestEntityTicket,
            new ParameterizedTypeReference<Response<Ticket>>()
            {
            });

        return reTicket.getBody().getData();
    }

    public boolean sendEmail(NotifyInfo notifyInfo, HttpHeaders httpHeaders)
    {
        PreserveServiceImpl.LOGGER.info("[Preserve Service][Send Email]");
        HttpEntity requestEntitySendEmail = new HttpEntity(notifyInfo, httpHeaders);
        ResponseEntity<Boolean> reSendEmail = restTemplate.exchange(
                "http://"+ tsNotificationServiceUrl + ":" + tsNotificationServicePort + "/api/v1/notifyservice/notification/preserve_success",
            HttpMethod.POST,
            requestEntitySendEmail,
            Boolean.class);

        return reSendEmail.getBody();
    }

    public User getAccount(String accountId, HttpHeaders httpHeaders)
    {
        PreserveServiceImpl.LOGGER.info("[Cancel Order Service][Get Order By Id]");

        HttpEntity requestEntitySendEmail = new HttpEntity(httpHeaders);
        ResponseEntity<Response<User>> getAccount = restTemplate.exchange(
                "http://"+ tsUserServiceUrl + ":" + tsUserServicePort + "/api/v1/userservice/users/id/" + accountId,
            HttpMethod.GET,
            requestEntitySendEmail,
            new ParameterizedTypeReference<Response<User>>()
            {
            });
        Response<User> result = getAccount.getBody();
        return result.getData();
    }

    private Response addAssuranceForOrder(int assuranceType, String orderId, HttpHeaders httpHeaders)
    {
        PreserveServiceImpl.LOGGER.info("[Preserve Service][Add Assurance For Order]");
        HttpEntity requestAddAssuranceResult = new HttpEntity(httpHeaders);
        ResponseEntity<Response> reAddAssuranceResult = restTemplate.exchange(
                "http://"+ tsAssuranceServiceUrl + ":" + tsAssuranceServicePort + "/api/v1/assuranceservice/assurances/" + assuranceType + "/" + orderId,
            HttpMethod.GET,
            requestAddAssuranceResult,
            Response.class);

        return reAddAssuranceResult.getBody();
    }

    private String queryForStationId(String stationName, HttpHeaders httpHeaders)
    {
        PreserveServiceImpl.LOGGER.info("[Preserve Other Service][Get Station Name]");

        HttpEntity requestQueryForStationId = new HttpEntity(httpHeaders);
        ResponseEntity<Response<String>> reQueryForStationId = restTemplate.exchange(
                "http://"+ tsStationServiceUrl + ":" + tsStationServicePort + "/api/v1/stationservice/stations/id/" + stationName,
            HttpMethod.GET,
            requestQueryForStationId,
            new ParameterizedTypeReference<Response<String>>()
            {
            });

        return reQueryForStationId.getBody().getData();
    }

    private Response checkSecurity(String accountId, HttpHeaders httpHeaders)
    {
        PreserveServiceImpl.LOGGER.info("[Preserve Other Service][Check Security] Checking....");

        HttpEntity requestCheckResult = new HttpEntity(httpHeaders);
        ResponseEntity<Response> reCheckResult = restTemplate.exchange(
                "http://"+ tsSecurityServiceUrl + ":" + tsSecurityServicePort + "/api/v1/securityservice/securityConfigs/" + accountId,
            HttpMethod.GET,
            requestCheckResult,
            Response.class);

        return reCheckResult.getBody();
    }

    private Response<TripAllDetail> getTripAllDetailInformation(TripAllDetailInfo gtdi, HttpHeaders httpHeaders)
    {
        PreserveServiceImpl.LOGGER.info("[Preserve Other Service][Get Trip All Detail Information] Getting....");

        HttpEntity requestGetTripAllDetailResult = new HttpEntity(gtdi, httpHeaders);
        ResponseEntity<Response<TripAllDetail>> reGetTripAllDetailResult = restTemplate.exchange(
                "http://"+ tsTravelServiceUrl + ":" + tsTravelServicePort + "/api/v1/travelservice/trip_detail",
            HttpMethod.POST,
            requestGetTripAllDetailResult,
            new ParameterizedTypeReference<Response<TripAllDetail>>()
            {
            });

        return reGetTripAllDetailResult.getBody();
    }

    private Response<Contacts> getContactsById(String contactsId, HttpHeaders httpHeaders)
    {
        PreserveServiceImpl.LOGGER.info("[Preserve Other Service][Get Contacts By Id] Getting....");

        HttpEntity requestGetContactsResult = new HttpEntity(httpHeaders);
        ResponseEntity<Response<Contacts>> reGetContactsResult = restTemplate.exchange(
                "http://"+ tsContactsServiceUrl + ":" + tsContactsServicePort + "/api/v1/contactservice/contacts/" + contactsId,
            HttpMethod.GET,
            requestGetContactsResult,
            new ParameterizedTypeReference<Response<Contacts>>()
            {
            });

        return reGetContactsResult.getBody();
    }

    private Response createOrder(Order coi, HttpHeaders httpHeaders)
    {
        PreserveServiceImpl.LOGGER.info("[Preserve Other Service][Get Contacts By Id] Creating....");

        HttpEntity requestEntityCreateOrderResult = new HttpEntity(coi, httpHeaders);
        ResponseEntity<Response<Order>> reCreateOrderResult = restTemplate.exchange(
                "http://"+ tsOrderServiceUrl + ":" + tsOrderServicePort + "/api/v1/orderservice/order",
            HttpMethod.POST,
            requestEntityCreateOrderResult,
            new ParameterizedTypeReference<Response<Order>>()
            {
            });

        return reCreateOrderResult.getBody();
    }

    private Response createFoodOrder(FoodOrder afi, HttpHeaders httpHeaders)
    {
        PreserveServiceImpl.LOGGER.info("[Preserve Service][Add food Order] Creating....");

        HttpEntity requestEntityAddFoodOrderResult = new HttpEntity(afi, httpHeaders);
        ResponseEntity<Response> reAddFoodOrderResult = restTemplate.exchange(
                "http://"+ tsFoodServiceUrl + ":" + tsFoodServicePort + "/api/v1/foodservice/orders",
            HttpMethod.POST,
            requestEntityAddFoodOrderResult,
            Response.class);

        return reAddFoodOrderResult.getBody();
    }

    private Response createConsign(Consign cr, HttpHeaders httpHeaders)
    {
        PreserveServiceImpl.LOGGER.info("[Preserve Service][Add Condign] Creating....");

        HttpEntity requestEntityResultForTravel = new HttpEntity(cr, httpHeaders);
        ResponseEntity<Response> reResultForTravel = restTemplate.exchange(
            "http://"+ tsConsignServiceUrl + ":" + tsConsignServicePort + "/api/v1/consignservice/consigns",
            HttpMethod.POST,
            requestEntityResultForTravel,
            Response.class);
        return reResultForTravel.getBody();
    }
}
