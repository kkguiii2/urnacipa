package com.cipa.votacao.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@Slf4j
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object path = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);

        String pathStr = path != null ? path.toString() : "";
        model.addAttribute("path", pathStr);

        if (status != null) {
            int statusCode = Integer.parseInt(status.toString());
            model.addAttribute("status", statusCode);

            if (statusCode == HttpStatus.FORBIDDEN.value()) {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                String principal = (auth != null) ? auth.getName() : "ANÔNIMO";
                Object authorities = (auth != null) ? auth.getAuthorities() : "NENHUMA";
                log.warn("Acesso 403 Proibido no recurso '{}'. Usuário: '{}', Permissões: '{}', Exceção: '{}'",
                        pathStr, principal, authorities, exception);

                model.addAttribute("message", "Acesso proibido. Permissões insuficientes.");
                return "error/403";
            }
            if (statusCode == HttpStatus.NOT_FOUND.value()) {
                model.addAttribute("message", "A página ou recurso solicitado não existe.");
                return "error/404";
            }
            if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                log.error("Erro 500 no recurso '{}':", pathStr, (Throwable) exception);
                model.addAttribute("message", "Ocorreu um erro interno no servidor.");
                return "error/500";
            }
        }

        if (exception instanceof Throwable throwable) {
            model.addAttribute("message", throwable.getMessage());
        }
        return "error/error";
    }
}
