package com.parapente.clima.seguridad;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.parapente.clima.errores.GlobalExceptionHandler;
import com.parapente.clima.veredicto.VeredictoController;
import com.parapente.clima.veredicto.VeredictoDTO;
import com.parapente.clima.veredicto.VeredictoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(VeredictoController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class VeredictoControllerSeguridadTest {

    private static final String URL = "/api/v1/clima/veredicto"
            + "?latitud=-31.4&longitud=-64.5&fechaHora=2030-01-01T10:00:00&vientoMaxKmh=25&margenKmh=10";

    @Autowired MockMvc mvc;
    @MockitoBean VeredictoService servicio;
    @MockitoBean JwtDecoder jwtDecoder; // el resource server lo exige; no se valida contra Keycloak

    @Test
    void sinTokenDa401() throws Exception {
        mvc.perform(get(URL)).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void accesoDenegadoDa403ConJson() throws Exception {
        // /actuator/info no está permitido: con token válido pasa a authenticated(); se prueba el handler directo
        var handler = new SecurityConfig();
        var res = new org.springframework.mock.web.MockHttpServletResponse();
        var m = SecurityConfig.class.getDeclaredMethod("json",
                jakarta.servlet.http.HttpServletResponse.class, int.class, String.class);
        m.setAccessible(true);
        m.invoke(handler, res, 403, "acceso denegado");
        org.junit.jupiter.api.Assertions.assertEquals(403, res.getStatus());
        org.junit.jupiter.api.Assertions.assertTrue(res.getContentAsString().contains("\"status\":403"));
    }

    @Test
    void conTokenDeCualquierRolDa200() throws Exception {
        when(servicio.evaluar(anyDouble(), anyDouble(), any(), anyDouble(), anyDouble()))
                .thenReturn(new VeredictoDTO("APTO", 10, 15, 5, "ok"));
        mvc.perform(get(URL).with(jwt().authorities(() -> "ROLE_CLIENTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.veredicto").value("APTO"));
    }

    @Test
    void parametrosFaltantesDa400() throws Exception {
        mvc.perform(get("/api/v1/clima/veredicto?latitud=-31.4").with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
