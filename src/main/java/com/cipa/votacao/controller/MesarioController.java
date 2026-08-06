package com.cipa.votacao.controller;

import com.cipa.votacao.exception.CabineVotacaoException;
import com.cipa.votacao.service.CabineVotacaoService;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/mesario")
@RequiredArgsConstructor
public class MesarioController {

    private final CabineVotacaoService cabineService;

    @GetMapping("/login")
    public String login() {
        return "mesario/login";
    }

    @GetMapping("/cabine")
    public String cabine(Model model) {
        model.addAttribute("cabine", cabineService.obterEstado());
        return "mesario/cabine";
    }

    @PostMapping("/cabine/liberar")
    public String liberar(
            @RequestParam String matricula,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        String normalizada = matricula == null ? "" : matricula.trim();
        if (!normalizada.matches("^[0-9]{1,20}$")) {
            redirectAttributes.addFlashAttribute("erro", "Informe uma matrícula numérica válida.");
            return "redirect:/mesario/cabine";
        }
        try {
            cabineService.liberar(normalizada, principal.getName());
            redirectAttributes.addFlashAttribute("sucesso", "Cabine liberada para o eleitor conferido.");
        } catch (CabineVotacaoException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/mesario/cabine";
    }

    @PostMapping("/cabine/cancelar")
    public String cancelar(Principal principal, RedirectAttributes redirectAttributes) {
        try {
            cabineService.cancelar(principal.getName());
            redirectAttributes.addFlashAttribute("sucesso", "Liberação cancelada.");
        } catch (CabineVotacaoException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/mesario/cabine";
    }
}
