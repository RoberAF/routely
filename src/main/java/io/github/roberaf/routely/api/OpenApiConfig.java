package io.github.roberaf.routely.api;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI routelyOpenApi() {
		return new OpenAPI().info(new Info()
				.title("Routely API")
				.version("v1")
				.description("Routely — route planning for field sales teams. Plans daily visit routes with a "
						+ "nearest-neighbor + 2-opt heuristic over PostGIS data."));
	}
}
