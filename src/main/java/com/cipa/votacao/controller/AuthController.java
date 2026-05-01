package com.cipa.votacao.controller;

import com.cipa.votacao.entity.Usuario;
import com.cipa.votacao.service.ConfiguracaoService;
import com.cipa.votacao.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UsuarioService usuarioService;
    private final ConfiguracaoService configuracaoService;

    @GetMapping("/login")
    public String loginPage(Model model) {
        if (!configuracaoService.isEleicaoAberta()) {
            model.addAttribute("mensagem", "Eleição ainda não foi aberta.");
            return "urna/indisponivel";
        }
        if (!configuracaoService.isPeriodoVotacao()) {
            model.addAttribute("mensagem", "Votação não disponível no momento. Aguarde a abertura da eleição.");
            return "urna/indisponivel";
        }
        return "urna/login";
    }

    @PostMapping("/verificar")
    public String verificarMatricula(@RequestParam String matricula, Model model, HttpSession session) {
        // Validate matricula is numeric
        if (!matricula.matches("^[0-9]+$")) {
            model.addAttribute("erro", "Matrícula deve conter apenas números.");
            return "urna/login";
        }

        var usuarioOpt = usuarioService.buscarPorMatricula(matricula);

        if (usuarioOpt.isEmpty()) {
            model.addAttribute("erro", "Matrícula não encontrada.");
            return "urna/login";
        }

        Usuario usuario = usuarioOpt.get();

        if (!usuario.isAtivo()) {
            model.addAttribute("erro", "Usuário inativo.");
            return "urna/login";
        }

        if (usuario.isVotou()) {
            model.addAttribute("erro", "Você já votou nesta eleição.");
            return "urna/login";
        }

        session.setAttribute("matricula", usuario.getMatricula());
        session.setAttribute("nome", usuario.getNome());

        model.addAttribute("nome", usuario.getNome());
        return "urna/confirmar";
    }

    @PostMapping("/confirmar")
    public String confirmar(HttpSession session, Model model) {
        String matricula = (String) session.getAttribute("matricula");
        if (matricula == null) {
            return "redirect:/auth/login";
        }
        return "redirect:/votacao/tela";
    }

    @PostMapping("/corrigir")
    public String corrigir(HttpSession session) {
        session.removeAttribute("matricula");
        session.removeAttribute("nome");
        return "redirect:/auth/login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/auth/login";
    }
}