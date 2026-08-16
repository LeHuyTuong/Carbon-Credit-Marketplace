package com.carbonx.marketcarbon;

import com.carbonx.marketcarbon.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P0-A (SC5): 500 responses must not leak internal details (SQL, hosts, class
 * names) to clients; the envelope/trace mechanism is preserved.
 */
class GlobalExceptionHandler500Test {

    @RestController
    static class BoomController {
        @GetMapping("/boom")
        public String boom() {
            throw new RuntimeException("jdbc:mysql://internal-host-3306/db Table 'x' doesn't exist");
        }
    }

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new BoomController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void unexpectedException_returnsGeneric500WithoutInternalDetails() throws Exception {
        String body = mockMvc.perform(get("/boom"))
                .andExpect(status().isInternalServerError())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain("internal-host-3306");
        assertThat(body).doesNotContain("jdbc:");
        assertThat(body).doesNotContain("doesn't exist");
        assertThat(body).contains("unexpected error");
        // envelope preserved
        assertThat(body).contains("requestTrace");
    }
}
