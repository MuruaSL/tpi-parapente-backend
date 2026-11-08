package com.parapente.vuelos.seguridad;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.data.domain.Pageable.unpaged;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.parapente.vuelos.errores.GlobalExceptionHandler;
import com.parapente.vuelos.reservas.EstadoReserva;
import com.parapente.vuelos.reservas.ReservaController;
import com.parapente.vuelos.reservas.ReservaDTO;
import com.parapente.vuelos.reservas.ReservaRechazadaException;
import com.parapente.vuelos.reservas.ReservaService;

/** Seguridad de los endpoints clave de reservas: 401 sin token, 403 por rol, 200/201 con el rol correcto. */
@WebMvcTest(ReservaController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class })
class SeguridadReservasWebMvcTest {

    @Autowired MockMvc mvc;
    @MockitoBean ReservaService service;
    @MockitoBean JwtDecoder jwtDecoder; // el filtro lo necesita; los tests entregan el JWT ya armado

    private RequestPostProcessor como(String sub, String rol) {
        return jwt().jwt(j -> j.subject(sub)).authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    private ReservaDTO dto(EstadoReserva estado, String motivo) {
        return new ReservaDTO(10L, 1L, "PRUEBA_APTO", null, estado, null, null, motivo, 70, 30, null);
    }

    @Test
    void sinTokenEs401ConJson() throws Exception {
        mvc.perform(get("/api/v1/vuelos/reservas/mias"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void clienteNoPuedeListarTodasEs403() throws Exception {
        mvc.perform(get("/api/v1/vuelos/reservas").with(como("c1", "CLIENTE")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void adminListaTodasEs200() throws Exception {
        when(service.listar(any())).thenReturn(new PageImpl<>(List.of(dto(EstadoReserva.CONFIRMADA, null))));
        mvc.perform(get("/api/v1/vuelos/reservas").with(como("a1", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(10));
    }

    @Test
    void clienteNoPuedeRevalidarNiFinalizarEs403() throws Exception {
        mvc.perform(post("/api/v1/vuelos/reservas/revalidacion").with(como("c1", "CLIENTE")))
                .andExpect(status().isForbidden());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .patch("/api/v1/vuelos/reservas/10/finalizacion").with(como("c1", "CLIENTE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void pilotoNoPuedeReservarEs403() throws Exception {
        mvc.perform(post("/api/v1/vuelos/reservas").with(como("p1", "PILOTO")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"turnoId\":1,\"pesoKg\":70,\"edad\":30}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void clienteCreaReservaUsandoSuSubEs201() throws Exception {
        when(service.crear(eq("11111111-1111-1111-1111-111111111111"), any()))
                .thenReturn(dto(EstadoReserva.CONFIRMADA, null));
        mvc.perform(post("/api/v1/vuelos/reservas").with(como("11111111-1111-1111-1111-111111111111", "CLIENTE"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"turnoId\":1,\"pesoKg\":70,\"edad\":30}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"));
    }

    @Test
    void reservaRechazadaDa422ConIdEstadoYMotivo() throws Exception {
        when(service.crear(any(), any())).thenThrow(
                new ReservaRechazadaException(dto(EstadoReserva.RECHAZADA, "turno completo")));
        mvc.perform(post("/api/v1/vuelos/reservas").with(como("c1", "CLIENTE"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"turnoId\":1,\"pesoKg\":70,\"edad\":30}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.reservaId").value(10))
                .andExpect(jsonPath("$.estado").value("RECHAZADA"))
                .andExpect(jsonPath("$.motivo").value("turno completo"));
    }

    @Test
    void cuerpoInvalidoEs400ConDetalles() throws Exception {
        mvc.perform(post("/api/v1/vuelos/reservas").with(como("c1", "CLIENTE"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"turnoId\":1,\"pesoKg\":-5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.pesoKg").exists())
                .andExpect(jsonPath("$.detalles.edad").exists());
    }

    @Test
    void reprogramarEsSoloClienteYCancelarAdminTambien() throws Exception {
        var rep = org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .patch("/api/v1/vuelos/reservas/10/reprogramacion").contentType(MediaType.APPLICATION_JSON)
                .content("{\"turnoId\":1}");
        mvc.perform(rep.with(como("a1", "ADMIN"))).andExpect(status().isForbidden());
        mvc.perform(rep.with(como("c1", "CLIENTE"))).andExpect(status().is(org.hamcrest.Matchers.not(403)));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .patch("/api/v1/vuelos/reservas/10/cancelacion").with(como("a1", "ADMIN")))
                .andExpect(status().is(org.hamcrest.Matchers.not(403)));
    }

    @Test
    void ordenInvalidoEs400ConJson() throws Exception {
        when(service.listar(any())).thenThrow(new IllegalArgumentException("sort inválido"));
        mvc.perform(get("/api/v1/vuelos/reservas").with(como("a1", "ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void errorInesperadoEs500SinDetalles() throws Exception {
        when(service.listar(any())).thenThrow(new RuntimeException("secreto interno"));
        mvc.perform(get("/api/v1/vuelos/reservas").with(como("a1", "ADMIN")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Error interno del servidor"));
    }

    @Test
    void adminVeCualquierReservaYClienteSoloLaSuya() throws Exception {
        when(service.obtener(10L, "a1", true)).thenReturn(dto(EstadoReserva.CONFIRMADA, null));
        mvc.perform(get("/api/v1/vuelos/reservas/10").with(como("a1", "ADMIN"))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/vuelos/reservas/10").with(como("c1", "CLIENTE")));
        verify(service).obtener(10L, "c1", false);
    }
}
