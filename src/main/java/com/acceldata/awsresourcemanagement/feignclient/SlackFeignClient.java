/* Copyright(C) 2020-21. Nuvepro Pvt. Ltd. All rights reserved */

package com.acceldata.awsresourcemanagement.feignclient;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@FeignClient(value = "slack",url="https://hooks.slack.com")
public interface SlackFeignClient {

    @PostMapping("/services/T05J161R7RC/B05JLQVT3C1/FHFPGdpZIPTdEffP21jnEkfT")
    ResponseEntity<String> publishMessage( @RequestBody String body )
            throws Exception;

}
