package security.service;

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

import security.entity.*;
import security.repository.SecurityRepository;

import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

/**
 * @author fdse
 */
@Service
public class SecurityServiceImpl implements SecurityService
{
    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityServiceImpl.class);

    @Autowired
    RestTemplate restTemplate;

    String success = "Success";

    @Autowired
    private SecurityRepository securityRepository;

    @Value("${ts.order.service.url:ts-order-service}")
    private String tsOrderServiceUrl;

    @Value("${ts.order.service.port:12031}")
    private String tsOrderServicePort;

    @Value("${ts.order.other.service.url:ts-order-other-service}")
    private String tsOrderOtherServiceUrl;

    @Value("${ts.order.other.service.port:12032}")
    private String tsOrderOtherServicePort;

    @Override
    public Response findAllSecurityConfig(HttpHeaders headers)
    {
        ArrayList<SecurityConfig> securityConfigs = securityRepository.findAll();
        if (securityConfigs != null && !securityConfigs.isEmpty()) {
            return new Response<>(1, success, securityConfigs);
        }
        return new Response<>(0, "No Content", null);
    }

    @Override
    public Response addNewSecurityConfig(SecurityConfig info, HttpHeaders headers)
    {
        SecurityConfig sc = securityRepository.findByName(info.getName());
        if (sc != null) {
            return new Response<>(0, "Security Config Already Exist", null);
        } else {
            SecurityConfig config = new SecurityConfig();
            config.setId(UUID.randomUUID());
            config.setName(info.getName());
            config.setValue(info.getValue());
            config.setDescription(info.getDescription());
            securityRepository.save(config);
            return new Response<>(1, success, config);
        }
    }

    @Override
    public Response modifySecurityConfig(SecurityConfig info, HttpHeaders headers)
    {
        SecurityConfig sc = securityRepository.findById(info.getId());
        if (sc == null) {
            return new Response<>(0, "Security Config Not Exist", null);
        } else {
            sc.setName(info.getName());
            sc.setValue(info.getValue());
            sc.setDescription(info.getDescription());
            securityRepository.save(sc);
            return new Response<>(1, success, sc);
        }
    }

    @Override
    public Response deleteSecurityConfig(String id, HttpHeaders headers)
    {
        securityRepository.deleteById(UUID.fromString(id));
        SecurityConfig sc = securityRepository.findById(UUID.fromString(id));
        if (sc == null) {
            return new Response<>(1, success, id);
        } else {
            return new Response<>(0, "Reason Not clear", id);
        }
    }

    @Override
    public Response check(String accountId, HttpHeaders headers)
    {
        //1.Get the orders in the past one hour and the total effective votes
        SecurityServiceImpl.LOGGER.info("[Security Service][Get Order Num Info]");
        OrderSecurity orderResult = getSecurityOrderInfoFromOrder(new Date(), accountId, headers);
        OrderSecurity orderOtherResult = getSecurityOrderOtherInfoFromOrder(new Date(), accountId, headers);
        int orderInOneHour = orderOtherResult.getOrderNumInLastOneHour() + orderResult.getOrderNumInLastOneHour();
        int totalValidOrder = orderOtherResult.getOrderNumOfValidOrder() + orderResult.getOrderNumOfValidOrder();
        //2. get critical configuration information
        SecurityServiceImpl.LOGGER.info("[Security Service][Get Security Config Info]");
        SecurityConfig configMaxInHour = securityRepository.findByName("max_order_1_hour");
        SecurityConfig configMaxNotUse = securityRepository.findByName("max_order_not_use");
        SecurityServiceImpl.LOGGER.info("[Security Service] Max In One Hour: {}  Max Not Use: {}",
            configMaxInHour.getValue(), configMaxNotUse.getValue());
        int oneHourLine = Integer.parseInt(configMaxInHour.getValue());
        int totalValidLine = Integer.parseInt(configMaxNotUse.getValue());
        if (orderInOneHour > oneHourLine || totalValidOrder > totalValidLine) {
            return new Response<>(0, "Too much order in last one hour or too much valid order", accountId);
        } else {
            return new Response<>(1, "Success.r", accountId);
        }
    }

    private OrderSecurity getSecurityOrderInfoFromOrder(Date checkDate, String accountId, HttpHeaders headers)
    {
        SecurityServiceImpl.LOGGER.info("[Security Service][Get Order Info For Security] Getting....");
        HttpEntity requestEntity = new HttpEntity(headers);
        ResponseEntity<Response<OrderSecurity>> re = restTemplate.exchange(
            "http://" + tsOrderServiceUrl + ":" + tsOrderServicePort + "/api/v1/orderservice/order/security/" + checkDate + "/" + accountId,
            HttpMethod.GET,
            requestEntity,
            new ParameterizedTypeReference<Response<OrderSecurity>>()
            {
            });
        Response<OrderSecurity> response = re.getBody();
        OrderSecurity result = response.getData();
        SecurityServiceImpl.LOGGER.info(
            "[Security Service][Get Order Info For Security] Last One Hour: {}  Total Valid Order: {}",
            result.getOrderNumInLastOneHour(), result.getOrderNumOfValidOrder());
        return result;
    }

    private OrderSecurity getSecurityOrderOtherInfoFromOrder(Date checkDate, String accountId, HttpHeaders headers)
    {
        SecurityServiceImpl.LOGGER.info("[Security Service][Get Order Other Info For Security] Getting....");
        HttpEntity requestEntity = new HttpEntity(headers);
        ResponseEntity<Response<OrderSecurity>> re = restTemplate.exchange(
            "http://" + tsOrderOtherServiceUrl + ":" + tsOrderOtherServicePort + "/api/v1/orderOtherService/orderOther/security/" + checkDate + "/"
                + accountId,
            HttpMethod.GET,
            requestEntity,
            new ParameterizedTypeReference<Response<OrderSecurity>>()
            {
            });
        Response<OrderSecurity> response = re.getBody();
        OrderSecurity result = response.getData();
        SecurityServiceImpl.LOGGER.info(
            "[Security Service][Get Order Other Info For Security] Last One Hour: {}  Total Valid Order: {}",
            result.getOrderNumInLastOneHour(), result.getOrderNumOfValidOrder());
        return result;
    }
}
