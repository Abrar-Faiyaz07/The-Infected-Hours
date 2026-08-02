package com.infectedhour.backend.service;

import com.infectedhour.backend.entity.Player;
import com.infectedhour.backend.entity.SaveState;
import com.infectedhour.backend.repository.PlayerRepository;
import com.infectedhour.backend.repository.SaveStateRepository;
import com.infectedhour.backend.security.JwtService;
import com.infectedhour.shared.dto.LoginRequest;
import com.infectedhour.shared.dto.LoginResponse;
import com.infectedhour.shared.dto.PlayerDto;
import com.infectedhour.shared.dto.RegisterRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.regex.Pattern;

/** POST /auth/register and /auth/login (Backend Schema §4). */
@Service
public class AuthService {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,24}$");

    private final PlayerRepository playerRepository;
    private final SaveStateRepository saveStateRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(PlayerRepository playerRepository, SaveStateRepository saveStateRepository,
                        PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.playerRepository = playerRepository;
        this.saveStateRepository = saveStateRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public UUID register(RegisterRequest request) {
        if (!USERNAME_PATTERN.matcher(request.username()).matches()) {
            throw new IllegalArgumentException("Invalid username format");
        }
        if (playerRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username already taken");
        }

        Player player = new Player(request.username(), request.displayName(),
                passwordEncoder.encode(request.password()));
        player = playerRepository.save(player);

        saveStateRepository.save(new SaveState(player)); // one SaveState per player, created at registration

        return player.getId();
    }

    public LoginResponse login(LoginRequest request) {
        Player player = playerRepository.findByUsername(request.username())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), player.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        player.recordLogin();
        playerRepository.save(player);

        String token = jwtService.generateToken(player.getId().toString(), player.getUsername());
        PlayerDto dto = new PlayerDto(player.getId(), player.getUsername(), player.getDisplayName());
        return new LoginResponse(token, jwtService.getExpirySeconds(), dto);
    }
}
