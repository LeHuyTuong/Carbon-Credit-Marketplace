package com.carbonx.marketcarbon;

import com.carbonx.marketcarbon.service.FileStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class MarketCarbonApplicationTests {

	@MockBean
	private FileStorageService fileStorageService;

	@Test
	void contextLoads() {
	}

}
