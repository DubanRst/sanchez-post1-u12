package com.empresa.reembolsos.procesamiento.notificacion;

import com.empresa.reembolsos.dominio.EstadoSolicitud;
import com.empresa.reembolsos.dominio.SolicitudReembolso;

/**
 * Suscriptor de los cambios de estado de una solicitud (Observer).
 * Cada implementacion reacciona a su manera sin que el servicio la conozca.
 */
public interface ObservadorCambioEstado {

    void onCambioEstado(SolicitudReembolso solicitud, EstadoSolicitud estadoAnterior);
}