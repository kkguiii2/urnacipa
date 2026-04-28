package com.cipa.votacao.controller;

import com.cipa.votacao.entity.Candidato;
import com.cipa.votacao.service.CandidatoService;
import com.cipa.votacao.service.VotacaoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.*;

@Controller
@RequestMapping("/votacao")
@RequiredArgsConstructor
@Slf4j
public class VotacaoController {

    private final VotacaoService votacaoService;
    private final CandidatoService candidatoService;

    @GetMapping("/tela")
    public String telaVotacao(HttpSession session, Model model) {
        String matricula = (String) session.getAttribute("matricula");
        if (matricula == null) {
            return "redirect:/auth/login";
        }

        if (!votacaoService.isVotacaoLiberada()) {
            model.addAttribute("mensagem", "Votação não disponível no momento.");
            return "urna/indisponivel";
        }

        List<Candidato> candidatos = candidatoService.listarAtivos();
        
        // Build safe candidato list for JavaScript (avoid circular references)
        List<Map<String, Object>> candidatosJson = new ArrayList<>();
        for (Candidato c : candidatos) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", c.getId());
            map.put("numero", c.getNumero());
            map.put("nome", c.getNome());
            map.put("foto", c.getFoto());
            candidatosJson.add(map);
        }
        
        model.addAttribute("candidatos", candidatos);
        model.addAttribute("candidatosJson", candidatosJson);
        model.addAttribute("nome", session.getAttribute("nome"));
        
        return "urna/votacao";
    }

    @GetMapping("/candidato/{numero}")
    @ResponseBody
    public Map<String, Object> buscarCandidato(@PathVariable Integer numero) {
        return candidatoService.buscarPorNumeroAtivo(numero)
                .map(c -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", c.getId());
                    map.put("numero", c.getNumero());
                    map.put("nome", c.getNome());
                    map.put("foto", c.getFoto());
                    return map;
                })
                .orElse(null);
    }

    @PostMapping("/votar")
    public String registrarVoto(@RequestParam Long candidatoId, HttpSession session, Model model) {
        String matricula = (String) session.getAttribute("matricula");
        if (matricula == null) {
            return "redirect:/auth/login";
        }

        if (!votacaoService.isVotacaoLiberada()) {
            model.addAttribute("mensagem", "Votação não disponível no momento.");
            return "urna/indisponivel";
        }

        Optional<Candidato> candidatoOpt = candidatoService.buscarPorId(candidatoId);
        if (candidatoOpt.isEmpty()) {
            model.addAttribute("erro", "Candidato não encontrado.");
            return "redirect:/votacao/tela";
        }

        var voto = votacaoService.registrarVoto(candidatoId, matricula);
        
        if (voto.isPresent()) {
            Candidato candidato = candidatoOpt.get();
            // Store candidato info in model before invalidating session
            model.addAttribute("candidatoNome", candidato.getNome());
            model.addAttribute("candidatoNumero", candidato.getNumero());
            session.invalidate();
            return "urna/sucesso";
        } else {
            model.addAttribute("erro", "Erro ao registrar voto. Tente novamente.");
            return "redirect:/votacao/tela";
        }
    }

    @GetMapping("/sucesso")
    public String sucesso() {
        return "urna/sucesso";
    }

    @GetMapping("/reset")
    public String reset(HttpSession session) {
        session.invalidate();
        return "redirect:/auth/login";
    }
}