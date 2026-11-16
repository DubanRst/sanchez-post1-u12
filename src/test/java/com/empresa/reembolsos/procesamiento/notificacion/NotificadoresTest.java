package com.empresa.reembolsos.procesamiento.notificacion;

import com.empresa.reembolsos.dominio.EstadoSolicitud;
import com.empresa.reembolsos.dominio.SolicitudReembolso;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(OutputCaptureExtension.class)
class NotificadoresTest {

    private SolicitudReembolso solicitud(EstadoSolicitud estado) {
        SolicitudReembolso s = new SolicitudReembolso();
        s.setId(7L);
        s.setEmpleadoId("EMP-001");
        s.setMontoAprobado(new BigDecimal("500000"));
        s.setEstado(estado);
        return s;
    }

    @Test
    void empleado_siempreRecibeElMensajeConElCambio(CapturedOutput output) {
        new NotificadorEmpleado().onCambioEstado(
            solicitud(EstadoSolicitud.RECHAZADA), EstadoSolicitud.PENDIENTE);
        assertTrue(output.getOut().contains("Notificando a EMP-001"));
    }

    @Test
    void contabilidad_alAprobar_programaElPago(CapturedOutput output) {
        new NotificadorContabilidad().onCambioEstado(
            solicitud(EstadoSolicitud.APROBADA), EstadoSolicitud.PENDIENTE);
        assertTrue(output.getOut().contains("Contabilidad: programar pago"));
    }

    @Test
    void contabilidad_alRechazar_noHaceNada(CapturedOutput output) {
        new NotificadorContabilidad().onCambioEstado(
            solicitud(EstadoSolicitud.RECHAZADA), EstadoSolicitud.PENDIENTE);
        assertFalse(output.getOut().contains("Contabilidad"));
    }

    @Test
    void auditoria_registraCualquierCambioSinLanzarExcepcion() {
        assertDoesNotThrow(() -> new NotificadorAuditoria().onCambioEstado(
            solicitud(EstadoSolicitud.REQUIERE_CORRECCION), EstadoSolicitud.PENDIENTE));
    }
}