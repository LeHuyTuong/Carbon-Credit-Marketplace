package com.carbonx.marketcarbon;

import com.carbonx.marketcarbon.config.ProfitSharingProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(ProfitSharingProperties.class)
public class  MarketCarbonApplication {

	public static void main(String[] args) {
		SpringApplication.run(MarketCarbonApplication.class, args);
	}

}
