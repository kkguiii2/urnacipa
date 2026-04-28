package com.cipa.votacao.service;

import com.cipa.votacao.entity.Candidato;
import com.cipa.votacao.repository.CandidatoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CandidatoService {

    private final CandidatoRepository candidatoRepository;

    public Candidato salvar(Candidato candidato) {
        return candidatoRepository.save(candidato);
    }

    public Optional<Candidato> buscarPorId(Long id) {
        return candidatoRepository.findById(id);
    }

    public Optional<Candidato> buscarPorNumero(Integer numero) {
        return candidatoRepository.findByNumero(numero);
    }

    public Optional<Candidato> buscarPorNumeroAtivo(Integer numero) {
        return candidatoRepository.findByNumeroAndAtivoTrue(numero);
    }

    public List<Candidato> listarTodos() {
        return candidatoRepository.findAll();
    }

    public List<Candidato> listarAtivos() {
        return candidatoRepository.findAllAtivosOrderByNumero();
    }

    @Transactional
    public void ativarDesativar(Long id, boolean ativo) {
        Optional<Candidato> candidatoOpt = candidatoRepository.findById(id);
        if (candidatoOpt.isPresent()) {
            Candidato candidato = candidatoOpt.get();
            candidato.setAtivo(ativo);
            candidatoRepository.save(candidato);
        }
    }

    public boolean existeNumero(Integer numero) {
        return candidatoRepository.existsByNumero(numero);
    }

    public void excluir(Long id) {
        candidatoRepository.deleteById(id);
    }

    public void atualizarFoto(Long id, String caminho) {
        Optional<Candidato> candidatoOpt = candidatoRepository.findById(id);
        if (candidatoOpt.isPresent()) {
            Candidato candidato = candidatoOpt.get();
            candidato.setFoto(caminho);
            candidatoRepository.save(candidato);
        }
    }
}