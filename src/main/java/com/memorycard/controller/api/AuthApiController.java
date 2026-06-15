package com.memorycard.controller.api;

import com.memorycard.dto.request.LoginRequest;
import com.memorycard.dto.request.RegisterRequest;
import com.memorycard.dto.response.AuthResponse;
import com.memorycard.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    private final AuthService authService;

    public AuthApiController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request,
                                                  HttpServletResponse response) {
        AuthResponse auth = authService.register(request);
        authService.setJwtCookie(response, auth.token());
        return ResponseEntity.ok(auth);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletResponse response) {
        AuthResponse auth = authService.login(request);
        authService.setJwtCookie(response, auth.token());
        return ResponseEntity.ok(auth);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        authService.clearJwtCookie(response);
        return ResponseEntity.noContent().build();
    }
}
