package io.github.roberaf.routely.api;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.roberaf.routely.optimizer.RouteOptimizer;

@Configuration
public class ApiConfig {

	@Bean
	public RouteOptimizer routeOptimizer() {
		return new RouteOptimizer();
	}
}
