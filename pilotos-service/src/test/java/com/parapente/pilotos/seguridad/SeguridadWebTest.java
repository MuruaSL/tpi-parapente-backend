package com.parapente.pilotos.seguridad;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.parapente.pilotos.agenda.AgendaController;
import com.parapente.pilotos.asignaciones.AsignacionInternaController;
import com.parapente.pilotos.asignaciones.AsignacionService;
import com.parapente.pilotos.asignaciones.PilotoDisponibleDTO;
import com.parapente.pilotos.pilotos.PilotoController;
import com.parapente.pilotos.pilotos.PilotoService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Seguridad (401/403/200) de administración, mi-agenda e internos, sin base ni Keycloak. */
@WebMvcTest({PilotoController.class, AgendaController.class, AsignacionInternaController.class})
@Import(SeguridadConfig.class)
class SeguridadWebTest {

    @Autowired MockMvc mvc;
    @MockitoBean JwtDecoder jwtDecoder;   // el resource server lo exige; acá los JWT se simulan
    @MockitoBean PilotoService pilotoService;
    @MockitoBean AsignacionService asignacionService;

    private RequestPostProcessor rol(String rol) {
        return jwt().jwt(j -> j.subject("22222222-2222-2222-2222-222222222222"))
                .authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    // Administración
    @Test void adminSinTokenEs401() throws Exception { mvc.perform(get("/api/v1/pilotos")).andExpect(status().isUnauthorized()); }
    @Test void adminConPilotoEs403() throws Exception { mvc.perform(get("/api/v1/pilotos").with(rol("PILOTO"))).andExpect(status().isForbidden()); }
    @Test void adminConClienteEs403() throws Exception { mvc.perform(post("/api/v1/pilotos").with(rol("CLIENTE"))).andExpect(status().isForbidden()); }
    @Test void adminConAdminEs200() throws Exception {
        when(pilotoService.listar()).thenReturn(List.of());
        mvc.perform(get("/api/v1/pilotos").with(rol("ADMIN"))).andExpect(status().isOk());
    }

    // mi-agenda
    @Test void agendaSinTokenEs401() throws Exception { mvc.perform(get("/api/v1/pilotos/mi-agenda")).andExpect(status().isUnauthorized()); }
    @Test void agendaConClienteEs403() throws Exception { mvc.perform(get("/api/v1/pilotos/mi-agenda").with(rol("CLIENTE"))).andExpect(status().isForbidden()); }
    @Test void agendaConPilotoEs200() throws Exception {
        when(asignacionService.agendaDe("22222222-2222-2222-2222-222222222222")).thenReturn(List.of());
        mvc.perform(get("/api/v1/pilotos/mi-agenda").with(rol("PILOTO"))).andExpect(status().isOk());
    }
    @Test void agendaConAdminEs200() throws Exception {
        mvc.perform(get("/api/v1/pilotos/mi-agenda").with(rol("ADMIN"))).andExpect(status().isOk());
    }

    // Internos: cualquier token válido, aun CLIENTE
    @Test void internosSinTokenEs401() throws Exception {
        mvc.perform(delete("/api/v1/internos/asignaciones/10")).andExpect(status().isUnauthorized());
    }
    @Test void internosConClienteEs200() throws Exception {
        when(asignacionService.buscarDisponible(any(), any())).thenReturn(new PilotoDisponibleDTO(1L, "Piloto Uno"));
        mvc.perform(get("/api/v1/internos/pilotos/disponibles")
                .param("tipoVuelo", "PRUEBA_APTO").param("fechaHora", "2030-01-10T10:00:00")
                .with(rol("CLIENTE"))).andExpect(status().isOk());
    }
    @Test void internosLiberarEs204() throws Exception {
        mvc.perform(delete("/api/v1/internos/asignaciones/10").with(rol("CLIENTE"))).andExpect(status().isNoContent());
    }
}
