package com.universidad.reservaslabs.service;

import com.universidad.reservaslabs.exception.ReservaConflictException;
import com.universidad.reservaslabs.model.Laboratorio;
import com.universidad.reservaslabs.repository.LaboratorioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class LaboratorioService {

    private final LaboratorioRepository laboratorioRepo;

    public LaboratorioService(LaboratorioRepository laboratorioRepo) {
        this.laboratorioRepo = laboratorioRepo;
    }

    public List<Laboratorio> findAll() {
        return laboratorioRepo.findAll();
    }

    public Optional<Laboratorio> findById(Long id) {
        return laboratorioRepo.findById(id);
    }

    public Laboratorio crear(Laboratorio laboratorio) {
        if (laboratorioRepo.existsByNombreIgnoreCase(laboratorio.getNombre())) {
            throw new ReservaConflictException(
                    "Ya existe un laboratorio con el nombre: " + laboratorio.getNombre());
        }
        return laboratorioRepo.save(laboratorio);
    }
}