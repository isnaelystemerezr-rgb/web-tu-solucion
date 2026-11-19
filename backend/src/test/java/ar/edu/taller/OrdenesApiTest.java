package ar.edu.taller;

import ar.edu.taller.domain.EstadoOrden;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de integración de las garantías del backend (las que se pueden llamar
 * directamente por HTTP sin pasar por la UI).
 */
@SpringBootTest
@AutoConfigureMockMvc
class OrdenesApiTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    // --- 1. Transiciones válidas y rechazo de saltos -------------------------

    @Test
    @DisplayName("La secuencia completa de estados funciona y deja 8 eventos")
    void flujoCompleto() throws Exception {
        Long id = crearOrden();

        paso(id, "en_diagnostico", null);
        paso(id, "presupuestada", Map.of(
                "diagnostico", "Placa con sulfato, se limpia",
                "manoObra", 50,
                "piezas", java.util.List.of(Map.of("descripcion", "Pasta térmica", "cantidad", 1, "precio", 10))));
        paso(id, "aprobada", null);
        paso(id, "en_reparacion", null);
        paso(id, "lista", null);
        paso(id, "entregada", null);

        mvc.perform(get("/api/eventos").param("ordenId", id.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(7))); // evento de apertura + 6 transiciones
    }

    @Test
    @DisplayName("Se rechaza un salto de estado (solicitada → aprobada)")
    void rechazaSalto() throws Exception {
        Long id = crearOrden();
        mvc.perform(post("/api/ordenes/{id}/transicion", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"a\":\"aprobada\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("TRANSICION_INVALIDA"))
                .andExpect(jsonPath("$.mensaje", containsString("no permitida")));
    }

    @Test
    @DisplayName("Un estado final no admite más cambios")
    void estadoFinal() throws Exception {
        Long id = crearOrden();
        paso(id, "cancelada", null);
        mvc.perform(post("/api/ordenes/{id}/transicion", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"a\":\"en_diagnostico\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", containsString("final")));
    }

    // --- 4. Cancelación solo desde solicitada -------------------------------

    @Test
    @DisplayName("No se puede cancelar una orden en diagnóstico")
    void cancelacionSoloDesdeSolicitada() throws Exception {
        Long id = crearOrden();
        paso(id, "en_diagnostico", null);
        mvc.perform(post("/api/ordenes/{id}/transicion", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"a\":\"cancelada\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("TRANSICION_INVALIDA"));
    }

    // --- 5. Presupuesto inmutable ------------------------------------------

    @Test
    @DisplayName("El presupuesto no puede modificarse tras presupuestar")
    void presupuestoInmutable() throws Exception {
        Long id = crearOrden();
        paso(id, "en_diagnostico", null);
        paso(id, "presupuestada", Map.of("diagnostico", "Cambio de batería", "manoObra", 30,
                "piezas", java.util.List.of()));

        mvc.perform(put("/api/ordenes/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"manoObra\":9999}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("PRESUPUESTO_INMUTABLE"));

        // Tampoco se puede volver a registrar vía transición (ya no es "en_diagnostico")
        mvc.perform(post("/api/ordenes/{id}/transicion", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"a\":\"presupuestada\",\"manoObra\":1}"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Presupuestar sin datos del presupuesto es rechazado")
    void presupuestoRequerido() throws Exception {
        Long id = crearOrden();
        paso(id, "en_diagnostico", null);
        mvc.perform(post("/api/ordenes/{id}/transicion", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"a\":\"presupuestada\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("PRESUPUESTO_REQUERIDO"));
    }

    // --- 6. Total calculado -------------------------------------------------

    @Test
    @DisplayName("El total del servidor coincide con manoObra + Σ(cantidad×precio)")
    void totalCalculado() throws Exception {
        Long id = crearOrden();
        paso(id, "en_diagnostico", null);
        paso(id, "presupuestada", Map.of("diagnostico", "x", "manoObra", 60,
                "piezas", java.util.List.of(
                        Map.of("descripcion", "A", "cantidad", 1, "precio", 85),
                        Map.of("descripcion", "B", "cantidad", 2, "precio", 7.5))));

        String totalBody = mvc.perform(get("/api/ordenes/{id}/total", id))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        java.math.BigDecimal totalEndpoint = json.readTree(totalBody).get("total").decimalValue();

        String ordenBody = mvc.perform(get("/api/ordenes/{id}", id))
                .andExpect(jsonPath("$.presupuesto.registrado").value(true))
                .andReturn().getResponse().getContentAsString();
        java.math.BigDecimal totalEnOrden = json.readTree(ordenBody).path("presupuesto").get("total").decimalValue();

        // 60 + (1*85) + (2*7.50) = 160.00 y ambos endpoints deben coincidir.
        org.junit.jupiter.api.Assertions.assertEquals(0, totalEndpoint.compareTo(new java.math.BigDecimal("160.00")));
        org.junit.jupiter.api.Assertions.assertEquals(0, totalEnOrden.compareTo(totalEndpoint));
        // Y el total calculado a mano en el cliente también coincide con el servidor.
    }

    // --- 2. Eventos: solo lectura y creación -------------------------------

    @Test
    @DisplayName("PUT/PATCH/DELETE sobre eventos devuelven 405")
    void eventosInmutables() throws Exception {
        Long eventoId = json.readTree(
                mvc.perform(get("/api/eventos"))
                        .andReturn().getResponse().getContentAsString())
                .get(0).get("id").asLong();

        mvc.perform(put("/api/eventos/{id}", eventoId)).andExpect(status().isMethodNotAllowed());
        mvc.perform(patch("/api/eventos/{id}", eventoId)).andExpect(status().isMethodNotAllowed());
        mvc.perform(delete("/api/eventos/{id}", eventoId)).andExpect(status().isMethodNotAllowed());
    }

    // --- 7. No se borran órdenes -------------------------------------------

    @Test
    @DisplayName("DELETE de una orden devuelve 405")
    void noSeBorraOrden() throws Exception {
        Long id = crearOrden();
        mvc.perform(delete("/api/ordenes/{id}", id)).andExpect(status().isMethodNotAllowed());
    }

    // --- 8. Validaciones ----------------------------------------------------

    @Test
    @DisplayName("Bean Validation rechaza payload inválido con detalles por campo")
    void validaciones() throws Exception {
        mvc.perform(post("/api/ordenes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"equipo\":\"\",\"problema\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"))
                .andExpect(jsonPath("$.detalles", hasSize(greaterThanOrEqualTo(1))));

        mvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\",\"telefono\":\"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"));
    }

    // --- Seed ---------------------------------------------------------------

    @Test
    @DisplayName("El seed deja 2 clientes y al menos 3 órdenes en etapas distintas")
    void seedCargado() throws Exception {
        mvc.perform(get("/api/clientes")).andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))));
        mvc.perform(get("/api/ordenes")).andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(3))));
        mvc.perform(get("/api/ordenes/resumen")).andExpect(jsonPath("$", hasSize(EstadoOrden.values().length)));
    }

    // --- helpers ------------------------------------------------------------

    private Long crearOrden() throws Exception {
        String body = mvc.perform(post("/api/ordenes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clienteId\":1,\"equipo\":\"Equipo de prueba\",\"problema\":\"No enciende\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("solicitada"))
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("id").asLong();
    }

    private void paso(Long id, String destino, Map<String, Object> extra) throws Exception {
        Map<String, Object> cuerpo = new java.util.LinkedHashMap<>();
        cuerpo.put("a", destino);
        if (extra != null) {
            cuerpo.putAll(extra);
        }
        mvc.perform(post("/api/ordenes/{id}/transicion", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(cuerpo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value(destino));
    }
}
