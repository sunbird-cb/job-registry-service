package com.tarento.jobservice.util.service.impl;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tarento.jobservice.constant.Constants;
import com.tarento.jobservice.dto.JWTDetailsDTO;
import com.tarento.jobservice.exception.JobException;
import com.tarento.jobservice.util.ObjectMapperSingleton;
import com.tarento.jobservice.util.service.UtilService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;

@Service
@Slf4j
public class UtilServiceImpl implements UtilService {

    private final RestTemplate restTemplate=new RestTemplate();

    @Value("${jwt.secret.key}")
    private String jwtSecretKey;

    @Override
    public String generateRedisJwtTokenKey(Object requestPayload) {
        if (requestPayload != null) {
            try {
                String reqJsonString = ObjectMapperSingleton.getInstance().writeValueAsString(requestPayload);
                return JWT.create()
                        .withClaim(Constants.REQUEST_PAYLOAD, reqJsonString)
                        .sign(Algorithm.HMAC256(jwtSecretKey));
            } catch (JsonProcessingException e) {
                log.error("Error occurred while converting json object to json string", e);
            }
        }
        return "";
    }


}
