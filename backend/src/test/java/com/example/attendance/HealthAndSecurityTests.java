package com.example.attendance;

import com.example.attendance.auth.LoginRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class HealthAndSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/health should return 200 OK and status UP without authentication")
    void healthCheckShouldReturn200AndStatusUp() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("GET /favicon.ico should return 404 NOT_FOUND instead of 500")
    void faviconShouldReturn404() throws Exception {
        mockMvc.perform(get("/favicon.ico"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Resource not found"));
    }

    @Test
    @DisplayName("GET /random-non-existent-endpoint should return 404 NOT_FOUND instead of 500")
    void randomEndpointShouldReturn404() throws Exception {
        mockMvc.perform(get("/random-non-existent-endpoint"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Resource not found"));
    }

    @Test
    @DisplayName("Secured business API (/api/classes) should return 401 when unauthenticated")
    void securedApiShouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/classes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    @DisplayName("Login, access secured API, and logout flow should work as expected")
    void loginAndLogoutFlowShouldWork() throws Exception {
        LoginRequest loginRequest = new LoginRequest("admin", "admin123");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

        // Access secured API with session
        mockMvc.perform(get("/api/classes").session(session))
                .andExpect(status().isOk());

        // Logout
        mockMvc.perform(post("/api/auth/logout").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));
    }

    @Test
    @DisplayName("CORS preflight request should be handled appropriately")
    void corsPreflightShouldWork() throws Exception {
        mockMvc.perform(options("/api/health")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"));
    }
}
