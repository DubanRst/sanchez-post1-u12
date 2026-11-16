package com.empresa.reembolsos;

import com.empresa.reembolsos.aplicacion.SolicitudInvalidaException;
import com.empresa.reembolsos.aplicacion.SolicitudReembolsoService;
import com.empresa.reembolsos.dominio.CategoriaGasto;
import com.empresa.reembolsos.dominio.EstadoSolicitud;
import com.empresa.reembolsos.dominio.SolicitudReembolso;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ExtendWith(OutputCaptureExtension.class)
class FlujoCompletoIntegracionTest {

    @Autowired
    private SolicitudReembolsoService servicio;

    private SolicitudReembolso nueva(String empleadoId, CategoriaGasto categoria,
                                     String monto, int dias, boolean documentos) {
        SolicitudReembolso s = new SolicitudReembolso();
        s.setEmpleadoId(empleadoId);
        s.setCategoria(categoria);
        s.setMontoSolicitado(new BigDecimal(monto));
        s.setFechaGasto(LocalDate.of(2026, 8, 6));
        s.setDiasViaje(dias);
        s.setDocumentosSoporte(documentos);
        return s;
    }

    private void assertMonto(String esperado, BigDecimal real) {
        assertEquals(0, new BigDecimal(esperado).compareTo(real),
            "esperado " + esperado + " pero fue " + real);
    }

    @Test
    void flujoCompleto_creaCalculaYNotificaLaAprobacion(CapturedOutput output) {
        var creada = servicio.crear(nueva("EMP-INT-1", CategoriaGasto.VIAJE, "100000", 2, true));

        assertNotNull(creada.getId());
        assertEquals(EstadoSolicitud.PENDIENTE, creada.getEstado());
        assertMonto("100000", creada.getMontoAprobado());

        var aprobada = servicio.cambiarEstado(creada.getId(), EstadoSolicitud.APROBADA);

        assertEquals(EstadoSolicitud.APROBADA, aprobada.getEstado());
        assertTrue(output.getOut().contains("Notificando a EMP-INT-1"));
        assertTrue(output.getOut().contains("Contabilidad: programar pago"));
    }

    @Test
    void crear_representacionYCapacitacion_calculanSegunSuCategoria() {
        var representacion = servicio.crear(
            nueva("EMP-INT-2", CategoriaGasto.REPRESENTACION, "1500000", 0, true));
        var capacitacion = servicio.crear(
            nueva("EMP-INT-3", CategoriaGasto.CAPACITACION, "1000000", 0, true));

        assertMonto("1500000", representacion.getMontoAprobado());
        assertMonto("800000", capacitacion.getMontoAprobado());
    }

    @Test
    void crear_sinDocumentos_rechazaConMensajeClaro() {
        var sinDocumentos = nueva("EMP-INT-4", CategoriaGasto.VIAJE, "100000", 2, false);

        var error = assertThrows(SolicitudInvalidaException.class,
            () -> servicio.crear(sinDocumentos));

        assertEquals("Faltan documentos soporte", error.getMessage());
    }

    @Test
    void crear_duplicadaEnElMismoPeriodo_seRechaza() {
        servicio.crear(nueva("EMP-INT-5", CategoriaGasto.VIAJE, "100000", 2, true));
        var duplicada = nueva("EMP-INT-5", CategoriaGasto.VIAJE, "100000", 2, true);
        duplicada.setFechaGasto(LocalDate.of(2026, 8, 20));

        var error = assertThrows(SolicitudInvalidaException.class,
            () -> servicio.crear(duplicada));

        assertTrue(error.getMessage().startsWith("Ya existe una solicitud pendiente"));
    }

    @Test
    void cambiarEstado_aRechazada_noProgramaPagoEnContabilidad(CapturedOutput output) {
        var creada = servicio.crear(nueva("EMP-INT-6", CategoriaGasto.VIAJE, "100000", 2, true));

        servicio.cambiarEstado(creada.getId(), EstadoSolicitud.RECHAZADA);

        assertTrue(output.getOut().contains("Notificando a EMP-INT-6"));
        assertFalse(output.getOut().contains("Contabilidad: programar pago"));
    }
}