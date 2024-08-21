package ticketinfo.service;

import edu.fudan.common.util.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import ticketinfo.entity.Travel;

/**
 * Created by Chenjie Xu on 2017/6/6.
 */
@Service
public class TicketInfoServiceImpl implements TicketInfoService
{
    @Autowired
    private RestTemplate restTemplate;

    @Value("${ts.basic.service.url:ts-basic-service}")
    private String tsBasicServiceUrl;

    @Value("${ts.basic.service.port:15680}")
    private String tsBasicServicePort;

    @Override
    public Response queryForTravel(Travel info, HttpHeaders headers)
    {
        HttpEntity requestEntity = new HttpEntity(info, headers);
        ResponseEntity<Response> re = restTemplate.exchange(
            "http://" + tsBasicServiceUrl + ":" + tsBasicServicePort + "/api/v1/basicservice/basic/travel",
            HttpMethod.POST,
            requestEntity,
            Response.class);
        return re.getBody();
    }

    @Override
    public Response queryForStationId(String name, HttpHeaders headers)
    {
        HttpEntity requestEntity = new HttpEntity(headers);
        ResponseEntity<Response> re = restTemplate.exchange(
            "http://" + tsBasicServiceUrl + ":" + tsBasicServicePort + "/api/v1/basicservice/basic/" + name,
            HttpMethod.GET,
            requestEntity,
            Response.class);

        return re.getBody();
    }
}
