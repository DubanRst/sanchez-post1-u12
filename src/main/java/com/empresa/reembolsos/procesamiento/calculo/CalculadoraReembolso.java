package com.empresa.reembolsos.procesamiento.calculo;

import com.empresa.reembolsos.dominio.CategoriaGasto;
import com.empresa.reembolsos.dominio.SolicitudReembolso;

import java.math.BigDecimal;

/**
 * Estrategia de calculo del monto aprobado (Strategy).
 * Cada implementacion encapsula la formula de una unica categoria de gasto.
 */
public interface CalculadoraReembolso {

    CategoriaGasto getCategoria();

    BigDecimal calcular(SolicitudReembolso solicitud);
}