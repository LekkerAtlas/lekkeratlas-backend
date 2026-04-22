package nl.lekkeratlas.lekkeratlasbackend.web;

import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoController {

    @GetMapping("/")
    public String home() {
        return "Public home";
    }

    @GetMapping("/api/me")
    public String me(Principal principal) {
        return "Hello " + principal;
    }
}