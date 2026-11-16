package com.empresa.reembolsos.procesamiento.validacion;

import com.empresa.reembolsos.dominio.CategoriaGasto;
import com.empresa.reembolsos.dominio.EstadoSolicitud;
import com.empresa.reembolsos.dominio.SolicitudReembolso;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidadoresTest {

    private static final LocalDate AGOSTO = LocalDate.of(2026, 8, 6);

    private SolicitudReembolso solicitud(CategoriaGasto categoria, String monto,
                                         LocalDate fecha, boolean documentos) {
        SolicitudReembolso s = new SolicitudReembolso();
        s.setEmpleadoId("EMP-001");
        s.setCategoria(categoria);
        s.setMontoSolicitado(new BigDecimal(monto));
        s.setFechaGasto(fecha);
        s.setDiasViaje(2);
        s.setDocumentosSoporte(documentos);
        s.setEstado(EstadoSolicitud.PENDIENTE);
        return s;
    }

    private CadenaValidadores cadena() {
        return new CadenaValidadores(new ValidadorDocumentosSoporte(),
            new ValidadorMontoMaximo(), new ValidadorDuplicados());
    }

    @Test
    void documentos_conDocumentos_esValida() {
        var resultado = new ValidadorDocumentosSoporte()
            .validar(solicitud(CategoriaGasto.VIAJE, "100000", AGOSTO, true), List.of());
        assertTrue(resultado.valida());
    }

    @Test
    void documentos_sinDocumentos_rechazaConMotivo() {
        var resultado = new ValidadorDocumentosSoporte()
            .validar(solicitud(CategoriaGasto.VIAJE, "100000", AGOSTO, false), List.of());
        assertFalse(resultado.valida());
        assertEquals("Faltan documentos soporte", resultado.motivoRechazo());
    }

    @Test
    void monto_dentroDelTope_esValida() {
        var resultado = new ValidadorMontoMaximo()
            .validar(solicitud(CategoriaGasto.VIAJE, "100000", AGOSTO, true), List.of());
        assertTrue(resultado.valida());
    }

    @Test
    void monto_enElTopeExacto_esValida() {
        var resultado = new ValidadorMontoMaximo()
            .validar(solicitud(CategoriaGasto.VIAJE, "150000", AGOSTO, true), List.of());
        assertTrue(resultado.valida());
    }

    @Test
    void monto_excedeElTopeDeViaje_rechaza() {
        var resultado = new ValidadorMontoMaximo()
            .validar(solicitud(CategoriaGasto.VIAJE, "150001", AGOSTO, true), List.of());
        assertFalse(resultado.valida());
        assertTrue(resultado.motivoRechazo().startsWith("Monto excede el tope"));
    }

    @Test
    void monto_excedeElTopeDeRepresentacion_rechaza() {
        var resultado = new ValidadorMontoMaximo()
            .validar(solicitud(CategoriaGasto.REPRESENTACION, "2000001", AGOSTO, true), List.of());
        assertFalse(resultado.valida());
    }

    @Test
    void duplicados_mismaCategoriaMismoPeriodo_rechaza() {
        var pendiente = solicitud(CategoriaGasto.VIAJE, "100000", LocalDate.of(2026, 8, 1), true);
        var nueva = solicitud(CategoriaGasto.VIAJE, "100000", LocalDate.of(2026, 8, 20), true);
        var resultado = new ValidadorDuplicados().validar(nueva, List.of(pendiente));
        assertFalse(resultado.valida());
        assertTrue(resultado.motivoRechazo().startsWith("Ya existe una solicitud pendiente"));
    }

    @Test
    void duplicados_otraCategoria_esValida() {
        var pendiente = solicitud(CategoriaGasto.VIAJE, "100000", AGOSTO, true);
        var nueva = solicitud(CategoriaGasto.REPRESENTACION, "100000", AGOSTO, true);
        assertTrue(new ValidadorDuplicados().validar(nueva, List.of(pendiente)).valida());
    }

    @Test
    void duplicados_mismoMesDeOtroAnio_esValida() {
        var pendiente = solicitud(CategoriaGasto.VIAJE, "100000", AGOSTO, true);
        var nueva = solicitud(CategoriaGasto.VIAJE, "100000", LocalDate.of(2027, 8, 6), true);
        assertTrue(new ValidadorDuplicados().validar(nueva, List.of(pendiente)).valida());
    }

    @Test
    void duplicados_sinPendientes_esValida() {
        var nueva = solicitud(CategoriaGasto.VIAJE, "100000", AGOSTO, true);
        assertTrue(new ValidadorDuplicados().validar(nueva, List.of()).valida());
    }

    @Test
    void cadena_sinDocumentosYExcedeMonto_reportaPrimeroLosDocumentos() {
        var resultado = cadena().validar(
            solicitud(CategoriaGasto.VIAJE, "999999", AGOSTO, false), List.of());
        assertEquals("Faltan documentos soporte", resultado.motivoRechazo());
    }

    @Test
    void cadena_excedeMontoYDuplicada_reportaElMonto() {
        var pendiente = solicitud(CategoriaGasto.VIAJE, "100000", LocalDate.of(2026, 8, 1), true);
        var nueva = solicitud(CategoriaGasto.VIAJE, "200000", LocalDate.of(2026, 8, 20), true);
        var resultado = cadena().validar(nueva, List.of(pendiente));
        assertTrue(resultado.motivoRechazo().startsWith("Monto excede el tope"));
    }

    @Test
    void cadena_todoCorrecto_esValida() {
        var resultado = cadena().validar(
            solicitud(CategoriaGasto.VIAJE, "100000", AGOSTO, true), List.of());
        assertTrue(resultado.valida());
    }
}