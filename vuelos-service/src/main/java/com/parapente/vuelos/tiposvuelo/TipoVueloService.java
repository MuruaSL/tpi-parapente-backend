package com.parapente.vuelos.tiposvuelo;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.parapente.vuelos.errores.ReglaNegocioException;
import com.parapente.vuelos.errores.RecursoNoEncontradoException;

/** Alta, modificación y consulta de tipos de vuelo. */
@Service
public class TipoVueloService {

    private final TipoVueloRepository repo;

    public TipoVueloService(TipoVueloRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<TipoVueloDTO> listar() {
        return repo.findAll().stream().map(TipoVueloDTO::desde).toList();
    }

    @Transactional
    public TipoVueloDTO crear(TipoVueloDTO dto) {
        validarRangos(dto);
        if (repo.existsByCodigo(dto.codigo())) {
            throw new ReglaNegocioException("ya existe un tipo de vuelo con código " + dto.codigo());
        }
        TipoVuelo t = new TipoVuelo(null, dto.codigo(), dto.nombre(), dto.pesoMin(), dto.pesoMax(),
                dto.edadMin(), dto.vientoMaxKmh(), dto.margenVientoKmh());
        return TipoVueloDTO.desde(repo.save(t));
    }

    @Transactional
    public TipoVueloDTO modificar(Long id, TipoVueloDTO dto) {
        validarRangos(dto);
        TipoVuelo t = repo.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("tipo de vuelo " + id + " no existe"));
        t.actualizar(dto.nombre(), dto.pesoMin(), dto.pesoMax(), dto.edadMin(), dto.vientoMaxKmh(),
                dto.margenVientoKmh());
        return TipoVueloDTO.desde(t);
    }

    private void validarRangos(TipoVueloDTO dto) {
        if (dto.pesoMin() > dto.pesoMax()) {
            throw new ReglaNegocioException("pesoMin no puede superar a pesoMax");
        }
    }
}
