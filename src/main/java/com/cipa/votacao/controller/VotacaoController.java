package com.cipa.votacao.controller;

import com.cipa.votacao.entity.Candidato;
import com.cipa.votacao.exception.CabineVotacaoException;
import com.cipa.votacao.service.CandidatoService;
import com.cipa.votacao.service.VotacaoService;
import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/votacao")
@RequiredArgsConstructor
public class VotacaoController {

    private final VotacaoService votacaoService;
    private final CandidatoService candidatoService;

    @GetMapping("/tela")
    public String telaVotacao(HttpSession session, Model model) {
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        Long sessaoCabineId = (Long) session.getAttribute("sessaoCabineId");
        if (usuarioId == null || sessaoCabineId == null) {
            return "redirect:/auth/login";
        }

        try {
            votacaoService.validarSessaoCabine(sessaoCabineId, usuarioId);
        } catch (CabineVotacaoException e) {
            limparEleitor(session);
            return "redirect:/auth/login";
        }

        if (!votacaoService.isVotacaoLiberada()) {
            model.addAttribute("mensagem", "Votação não disponível no momento.");
            return "urna/indisponivel";
        }

        List<Candidato> candidatos = candidatoService.listarAtivos();
        List<Map<String, Object>> candidatosJson = new ArrayList<>();
        for (Candidato candidato : candidatos) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", candidato.getId());
            item.put("numero", candidato.getNumero());
            item.put("nome", candidato.getNome());
            item.put("foto", fotoSegura(candidato.getFoto()));
            candidatosJson.add(item);
        }

        model.addAttribute("candidatos", candidatos);
        model.addAttribute("candidatosJson", candidatosJson);
        model.addAttribute("nome", session.getAttribute("nome"));
        return "urna/votacao";
    }

    @GetMapping("/candidato/{numero}")
    @ResponseBody
    public Map<String, Object> buscarCandidato(@PathVariable Integer numero, HttpSession session) {
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        Long sessaoCabineId = (Long) session.getAttribute("sessaoCabineId");
        if (usuarioId == null || sessaoCabineId == null) {
            return null;
        }
        try {
            votacaoService.validarSessaoCabine(sessaoCabineId, usuarioId);
        } catch (CabineVotacaoException e) {
            limparEleitor(session);
            return null;
        }
        return candidatoService.buscarPorNumeroAtivo(numero)
                .map(candidato -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", candidato.getId());
                    item.put("numero", candidato.getNumero());
                    item.put("nome", candidato.getNome());
                    item.put("foto", fotoSegura(candidato.getFoto()));
                    return item;
                })
                .orElse(null);
    }

    @PostMapping("/votar")
    public String registrarVoto(
            @RequestParam Long candidatoId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        Long sessaoCabineId = (Long) session.getAttribute("sessaoCabineId");
        if (usuarioId == null || sessaoCabineId == null) {
            return "redirect:/auth/login";
        }

        if (votacaoService.registrarVoto(candidatoId, usuarioId, sessaoCabineId).isPresent()) {
            limparEleitor(session);
            return "urna/sucesso";
        }

        limparEleitor(session);
        redirectAttributes.addFlashAttribute("erro", "Não foi possível registrar o voto.");
        return "redirect:/auth/login";
    }

    @GetMapping("/sucesso")
    public String sucesso() {
        return "urna/sucesso";
    }

    private void limparEleitor(HttpSession session) {
        session.removeAttribute("matricula");
        session.removeAttribute("usuarioId");
        session.removeAttribute("nome");
        session.removeAttribute("sessaoCabineId");
    }

    private String fotoSegura(String foto) {
        if (foto == null || !foto.matches("^[a-f0-9-]{36}\\.(jpg|jpeg|png)$")) {
            return null;
        }
        return foto;
    }
}
