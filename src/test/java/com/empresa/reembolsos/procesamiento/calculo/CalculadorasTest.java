package com.empresa.reembolsos.procesamiento.calculo;

import com.empresa.reembolsos.dominio.CategoriaGasto;
import com.empresa.reembolsos.dominio.SolicitudReembolso;
import com.empresa.reembolsos.procesamiento.SelectorCalculadora;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CalculadorasTest {

    private SolicitudReembolso solicitud(CategoriaGasto categoria, String monto, int dias) {
        SolicitudReembolso s = new SolicitudReembolso();
        s.setCategoria(categoria);
        s.setMontoSolicitado(new BigDecimal(monto));
        s.setDiasViaje(dias);
        return s;
    }

    private void assertMonto(String esperado, BigDecimal real) {
        assertEquals(0, new BigDecimal(esperado).compareTo(real),
            "esperado " + esperado + " pero fue " + real);
    }

    @Test
    void viaje_bajoElTopeDiario_reembolsaElMontoCompleto() {
        assertMonto("100000",
            new CalculadoraViaje().calcular(solicitud(CategoriaGasto.VIAJE, "100000", 2)));
    }

    @Test
    void viaje_sobreElTopeDiario_limitaAlTopePorDias() {
        // 3 dias x 150000 = 450000
        assertMonto("450000",
            new CalculadoraViaje().calcular(solicitud(CategoriaGasto.VIAJE, "600000", 3)));
    }

    @Test
    void representacion_bajoElTopeMensual_reembolsaElMontoCompleto() {
        assertMonto("1500000", new CalculadoraRepresentacion()
            .calcular(solicitud(CategoriaGasto.REPRESENTACION, "1500000", 0)));
    }

    @Test
    void representacion_sobreElTopeMensual_limitaAlTope() {
        assertMonto("2000000", new CalculadoraRepresentacion()
            .calcular(solicitud(CategoriaGasto.REPRESENTACION, "2500000", 0)));
    }

    @Test
    void capacitacion_reembolsaElOchentaPorCiento() {
        assertMonto("800000", new CalculadoraCapacitacion()
            .calcular(solicitud(CategoriaGasto.CAPACITACION, "1000000", 0)));
    }

    @Test
    void capacitacion_sobreElTopeAnual_limitaAlTope() {
        // 80% de 5.000.000 = 4.000.000, limitado a 3.000.000
        assertMonto("3000000", new CalculadoraCapacitacion()
            .calcular(solicitud(CategoriaGasto.CAPACITACION, "5000000", 0)));
    }

    @Test
    void selector_devuelveLaCalculadoraDeCadaCategoria() {
        var selector = new SelectorCalculadora(List.of(
            new CalculadoraViaje(), new CalculadoraRepresentacion(), new CalculadoraCapacitacion()));
        assertInstanceOf(CalculadoraViaje.class, selector.obtener(CategoriaGasto.VIAJE));
        assertInstanceOf(CalculadoraRepresentacion.class, selector.obtener(CategoriaGasto.REPRESENTACION));
        assertInstanceOf(CalculadoraCapacitacion.class, selector.obtener(CategoriaGasto.CAPACITACION));
    }

    @Test
    void selector_categoriaSinCalculadora_lanzaExcepcion() {
        var selector = new SelectorCalculadora(List.of(new CalculadoraViaje()));
        assertThrows(IllegalArgumentException.class,
            () -> selector.obtener(CategoriaGasto.CAPACITACION));
    }
}