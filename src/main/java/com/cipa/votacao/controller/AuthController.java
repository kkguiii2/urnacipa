package com.cipa.votacao.controller;

import com.cipa.votacao.entity.SessaoCabine;
import com.cipa.votacao.entity.Usuario;
import com.cipa.votacao.exception.CabineVotacaoException;
import com.cipa.votacao.service.CabineVotacaoService;
import com.cipa.votacao.service.ConfiguracaoService;
import com.cipa.votacao.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UsuarioService usuarioService;
    private final ConfiguracaoService configuracaoService;
    private final CabineVotacaoService cabineService;

    @GetMapping("/login")
    public String loginPage(Model model, HttpSession session) {
        if (session.getAttribute("matricula") != null
                && session.getAttribute("sessaoCabineId") != null) {
            return "redirect:/votacao/tela";
        }
        if (!configuracaoService.isEleicaoAberta()) {
            model.addAttribute("mensagem", "Eleição ainda não foi aberta.");
            return "urna/indisponivel";
        }
        if (!configuracaoService.isPeriodoVotacao()) {
            model.addAttribute("mensagem", "Votação não disponível no momento.");
            return "urna/indisponivel";
        }
        if (!cabineService.disponivelParaIdentificacao()) {
            return "urna/aguardando";
        }
        return "urna/login";
    }

    @PostMapping("/verificar")
    public String verificarMatricula(
            @RequestParam String matricula,
            Model model,
            HttpServletRequest request) {
        String normalizada = matricula == null ? "" : matricula.trim();
        if (!normalizada.matches("^[0-9]{1,20}$")) {
            model.addAttribute("erro", "Matrícula não corresponde à liberação atual.");
            return "urna/login";
        }

        try {
            SessaoCabine sessaoCabine = cabineService.identificar(normalizada);
            Usuario usuario = usuarioService.buscarPorId(sessaoCabine.getUsuarioId())
                    .orElseThrow(() -> new CabineVotacaoException("Liberação inválida."));

            request.changeSessionId();
            HttpSession session = request.getSession();
            session.setAttribute("matricula", usuario.getMatricula());
            session.setAttribute("usuarioId", usuario.getId());
            session.setAttribute("nome", usuario.getNome());
            session.setAttribute("sessaoCabineId", sessaoCabine.getId());
            return "redirect:/votacao/tela";
        } catch (CabineVotacaoException e) {
            model.addAttribute("erro", "Matrícula não corresponde à liberação atual.");
            return "urna/login";
        }
    }
}
