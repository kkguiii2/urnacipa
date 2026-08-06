package com.cipa.votacao.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/cabine")
public class CabineController {

    @GetMapping("/login")
    public String login() {
        return "cabine/login";
    }
}
