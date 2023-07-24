package com.acceldata.awsresourcemanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients( "com.acceldata.awsresourcemanagement" )
public class AwsResourceManagementApplication {

	public static void main(String[] args) {
		SpringApplication.run(AwsResourceManagementApplication.class, args);
	}

}
