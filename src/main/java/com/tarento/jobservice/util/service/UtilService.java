package com.tarento.jobservice.util.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tarento.jobservice.dto.JWTDetailsDTO;

import javax.servlet.http.HttpServletRequest;

public interface UtilService {

    String generateRedisJwtTokenKey(Object requestPayload);


    }
