package com.trainticket.controller;

import com.alibaba.fastjson.JSONObject;
import com.trainticket.entity.Payment;
import com.trainticket.service.PaymentService;

import edu.fudan.common.util.Response;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@RunWith(JUnit4.class)
public class PaymentControllerTest
{
    @InjectMocks
    private PaymentController paymentController;

    @Mock
    private PaymentService service;

    private MockMvc mockMvc;

    private Response response = new Response();

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(paymentController).build();
    }

    @Test
    public void testHome() throws Exception
    {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/paymentservice/welcome"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().string("Welcome to [ Payment Service ] !"));
    }

    @Test
    public void testPay() throws Exception
    {
        Payment info = new Payment();
        Mockito.when(service.pay(Mockito.any(Payment.class), Mockito.any(HttpHeaders.class))).thenReturn(response);
        String requestJson = JSONObject.toJSONString(info);
        String result = mockMvc.perform(
                MockMvcRequestBuilders.post("/api/v1/paymentservice/payment").contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn().getResponse().getContentAsString();
        Assert.assertEquals(response, JSONObject.parseObject(result, Response.class));
    }

    @Test
    public void testAddMoney() throws Exception
    {
        Payment info = new Payment();
        Mockito.when(service.addMoney(Mockito.any(Payment.class), Mockito.any(HttpHeaders.class))).thenReturn(response);
        String requestJson = JSONObject.toJSONString(info);
        String result = mockMvc.perform(
                MockMvcRequestBuilders.post("/api/v1/paymentservice/payment/money").contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn().getResponse().getContentAsString();
        Assert.assertEquals(response, JSONObject.parseObject(result, Response.class));
    }

    @Test
    public void testQuery() throws Exception
    {
        Mockito.when(service.query(Mockito.any(HttpHeaders.class))).thenReturn(response);
        String result = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/paymentservice/payment"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn().getResponse().getContentAsString();
        Assert.assertEquals(response, JSONObject.parseObject(result, Response.class));
    }
}
