package com.parapente.clima.veredicto;

import com.parapente.clima.errores.ReglaNegocioException;
import com.parapente.clima.pronostico.OpenMeteoClient;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

/** Orquesta: valida la ventana de fechas, pide el pronóstico y aplica ReglaClima. */
@Service
public class VeredictoService {

    /** Open-Meteo sólo pronostica 16 días. */
    static final int DIAS_MAX = 16;

    private final OpenMeteoClient cliente;

    public VeredictoService(OpenMeteoClient cliente) {
        this.cliente = cliente;
    }

    public VeredictoDTO evaluar(double latitud, double longitud, LocalDateTime fechaHora,
                                double vientoMaxKmh, double margenKmh) {
        LocalDateTime ahora = LocalDateTime.now();
        if (fechaHora.isBefore(ahora)) {
            throw new ReglaNegocioException("La fecha ya pasó");
        }
        if (fechaHora.isAfter(ahora.plusDays(DIAS_MAX))) {
            throw new ReglaNegocioException("La fecha excede los " + DIAS_MAX + " días de pronóstico");
        }
        return ReglaClima.evaluar(cliente.obtener(latitud, longitud, fechaHora), vientoMaxKmh, margenKmh);
    }
}
