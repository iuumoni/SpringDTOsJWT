package com.example.demo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static String userToken;
    private static String adminToken;
    private static String createdProductId;
    private static String createdCommentId;
    private static String createdImageId;

    private String loginAndExtractToken(String username, String password) throws Exception {
        String body = objectMapper.writeValueAsString(
                java.util.Map.of("username", username, "password", password)
        );

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        if (json.hasNonNull("accessToken")) {
            return json.get("accessToken").asText();
        }
        return json.get("token").asText();
    }

    @Test
    @Order(1)
    void getEndpointsRespondWithoutToken() throws Exception {
        mockMvc.perform(get("/productos"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/imagenes"))
                .andExpect(status().isOk());
    }

    @Test
    @Order(2)
    void protectedWriteWithoutTokenReturns401() throws Exception {
        String validProduct = """
                {
                  "nombre": "Agua Sin Gas Test",
                  "descripcion": "Botella de prueba",
                  "lote": "LOTE-TEST-01",
                  "precioUnitario": 15.0,
                  "fechaCaducidad": "2027-12-31"
                }
                """;

        mockMvc.perform(post("/producto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validProduct))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(3)
    void registrationAlwaysCreatesUserRoleAndLoginIssuesJwtOr401OnInvalid() throws Exception {
        String suffix = String.valueOf(System.currentTimeMillis());
        String username = "test_user_" + suffix;
        String password = "Password123!";

        String registerBody = String.format("""
                {
                  "username": "%s",
                  "password": "%s"
                }
                """, username, password);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().is2xxSuccessful());

        String assignedRole = jdbcTemplate.queryForObject(
                "SELECT role FROM app_users WHERE username = ?",
                String.class,
                username
        );
        org.assertj.core.api.Assertions.assertThat(assignedRole).isEqualTo("USER");

        userToken = loginAndExtractToken(username, password);
        org.assertj.core.api.Assertions.assertThat(userToken).isNotBlank();

        String badLoginBody = String.format("""
                {
                  "username": "%s",
                  "password": "WrongPassword999"
                }
                """, username);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badLoginBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(4)
    void userTokenGets403WhenWritingProductsOrCategories() throws Exception {
        String productPayload = """
                {
                  "nombre": "Jugo Prohibido USER",
                  "descripcion": "No debe crearse con USER",
                  "lote": "LOTE-403",
                  "precioUnitario": 20.0,
                  "fechaCaducidad": "2027-10-10"
                }
                """;

        mockMvc.perform(post("/producto")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productPayload))
                .andExpect(status().isForbidden());

        String categoryPayload = """
                {
                  "nombre": "Bebidas Test"
                }
                """;

        mockMvc.perform(post("/categoria")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoryPayload))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(5)
    void invalidDtoReturns400() throws Exception {
        jdbcTemplate.update("UPDATE app_users SET role = 'ADMIN' WHERE username = 'monica_dev'");
        adminToken = loginAndExtractToken("monica_dev", "password123");

        String invalidProductPayload = """
                {
                  "nombre": "",
                  "precioUnitario": -5.0
                }
                """;

        mockMvc.perform(post("/producto")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidProductPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(6)
    void adminCreatesProductAndPatchModifiesOnlySentFields() throws Exception {
        String createPayload = """
                {
                  "nombre": "Bebida Original",
                  "descripcion": "Descripcion inicial",
                  "lote": "LOTE-PATCH-01",
                  "precioUnitario": 25.0,
                  "fechaCaducidad": "2027-09-01"
                }
                """;

        MvcResult createResult = mockMvc.perform(post("/producto")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre", is("Bebida Original")))
                .andExpect(jsonPath("$.precioUnitario", is(25.0)))
                .andReturn();

        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        createdProductId = created.get("id").asText();

        String patchPayload = """
                {
                  "precioUnitario": 30.0
                }
                """;

        mockMvc.perform(patch("/producto/" + createdProductId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patchPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precioUnitario", is(30.0)))
                .andExpect(jsonPath("$.nombre", is("Bebida Original")))
                .andExpect(jsonPath("$.descripcion", is("Descripcion inicial")));
    }

    @Test
    @Order(7)
    void userOrAdminCanWriteCommentsAndImages() throws Exception {
        String imagePayload = """
                {
                  "url": "https://example.com/test-img.jpg",
                  "prioridad": 1,
                  "tamanio": "800x800",
                  "responsiva": true
                }
                """;

        MvcResult imgRes = mockMvc.perform(post("/imagen/" + createdProductId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(imagePayload))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        createdImageId = objectMapper.readTree(imgRes.getResponse().getContentAsString()).get("id").asText();

        String commentPayload = """
                {
                  "username": "monica_dev",
                  "comentario_corto": "Muy buen producto",
                  "estrellas": 5,
                  "comentario_largo": "Excelente producto de prueba en integracion",
                  "imagen1": "https://example.com/img1.jpg",
                  "imagen2": "https://example.com/img2.jpg"
                }
                """;

        MvcResult commentRes = mockMvc.perform(post("/comentario/" + createdProductId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentPayload))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        createdCommentId = objectMapper.readTree(commentRes.getResponse().getContentAsString()).get("id").asText();

        // Limpieza de registros creados durante la prueba
        if (createdCommentId != null) {
            mockMvc.perform(delete("/comentario/" + createdCommentId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().is2xxSuccessful());
        }
        if (createdImageId != null) {
            mockMvc.perform(delete("/imagen/" + createdImageId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().is2xxSuccessful());
        }
        if (createdProductId != null) {
            mockMvc.perform(delete("/producto/" + createdProductId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().is2xxSuccessful());
        }
    }

    @Test
    @Order(8)
    void openApiPublishesBearerJwtScheme() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type", is("http")))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme", equalToIgnoringCase("bearer")))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat", equalToIgnoringCase("JWT")));
    }
} 