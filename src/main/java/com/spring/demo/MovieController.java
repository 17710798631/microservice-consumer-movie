package com.spring.demo;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.spring.demo.entity.User;

@RestController
@RefreshScope
public class MovieController {

	private static final Logger LOGGER = LoggerFactory.getLogger(MovieController.class);
	
	public static final String PROVIDER_Name = "microservice-provider-user";
	
	private final String providerUrl = "http://microservice-provider-user/{id}";
	
	@Autowired
	private RestTemplate restTemplate;
	
	@Autowired
	private LoadBalancerClient loadBalancerClient;
	
	@Value("${profile}")
	private String profile;
	
	@HystrixCommand(fallbackMethod = "findByIdFallback")
	@GetMapping("/user/{id}")
	public User findById(@PathVariable("id") Long id) {
		return restTemplate.getForEntity(providerUrl, User.class, id).getBody();
	}
	
	/**
	 * 验证负载均衡生效
	 */
	@HystrixCommand(fallbackMethod = "logUserFallback")
	@GetMapping("/log-user-instance")
	public Map<String, Object> logUserInstance() {
		ServiceInstance serviceInstance = loadBalancerClient.choose(PROVIDER_Name);
		LOGGER.info("{}:{}:{}", serviceInstance.getServiceId(), serviceInstance.getHost(), serviceInstance.getPort());
		Map<String, Object> info = new LinkedHashMap<>();
		info.put("serviceId", serviceInstance.getServiceId());
		info.put("host", serviceInstance.getHost());
		info.put("port", serviceInstance.getPort());
		return info;
	}
	
	@GetMapping("/profile")
	public String config() {
		return profile;
	}
	
	public Map<String, Object> logUserFallback() {
		Map<String, Object> info = new LinkedHashMap<>();
		info.put("error", "No result reponse!");
		return info;
	}
	
	public User findByIdFallback(Long id, Throwable throwable) {
		LOGGER.error("进入到回退方法，异常为：", throwable);
		User user = new User();
		user.setId(-1L);
		user.setName("默认用户");
		return user;
	}
	
}
