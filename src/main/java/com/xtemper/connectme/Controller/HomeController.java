package com.xtemper.connectme.Controller;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    @GetMapping("/")
    public String home() {
        return "index"; // Refers to src/main/resources/templates/index.html
    }
}
