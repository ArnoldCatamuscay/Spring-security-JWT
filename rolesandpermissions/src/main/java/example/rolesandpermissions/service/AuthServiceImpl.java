package example.rolesandpermissions.service;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import example.rolesandpermissions.dto.request.RegisterRequest;
import example.rolesandpermissions.dto.response.AuthenticationResponse;
import example.rolesandpermissions.model.Role;
import example.rolesandpermissions.model.Token;
import example.rolesandpermissions.model.TokenType;
import example.rolesandpermissions.model.User;
import example.rolesandpermissions.dto.request.AuthenticationRequest;
import example.rolesandpermissions.repo.RoleRepository;
import example.rolesandpermissions.repo.TokenRepository;
import example.rolesandpermissions.repo.UserRepository;
import example.rolesandpermissions.utils.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService{
	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final TokenRepository tokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final AuthenticationManager authenticationManager;

    @Override
    public AuthenticationResponse register(RegisterRequest request) {
		Role role = roleRepository.findByName(request.getRole())
			.orElseThrow(/* TODO: Handle exception */);
        var user = User.builder()
            .firstName(request.getFirstname())
            .lastName(request.getLastname())
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .role(role)
            .build();
		var savedUser = userRepository.save(user); //* Guarda el usuario en la base de datos */
		var jwtToken = jwtService.generateToken(user); //* Genera el Access Token */
		var refreshToken = jwtService.generateRefreshToken(user); //* Genera el Refresh Token */
		saveUserToken(savedUser, jwtToken); //* guarda el Access Token en la base de datos */
		return AuthenticationResponse.builder() //* retorna un DTO con el Access y el Refresh Token */
			.accessToken(jwtToken)
			.refreshToken(refreshToken)
			.build();
  	}

	@Override
  	public AuthenticationResponse authenticate(AuthenticationRequest request) {
		authenticationManager.authenticate(
			new UsernamePasswordAuthenticationToken(
				request.getEmail(),
				request.getPassword()
			)
		);
		var user = userRepository.findByEmail(request.getEmail())
			.orElseThrow(/*TODO: Handle Exception */);
		var jwtToken = jwtService.generateToken(user);
		var refreshToken = jwtService.generateRefreshToken(user);
		revokeAllUserTokens(user); //* Pone los tokens del usuario en expirados o revocados */
		saveUserToken(user, jwtToken); //* Guarda el nuevo Token generado */
		return AuthenticationResponse.builder()
			.accessToken(jwtToken)
			.refreshToken(refreshToken)
			.build();
	}
	
	@Override
	public void saveUserToken(User user, String jwtToken) {
		var token = Token.builder()
			.user(user)
			.token(jwtToken)
			.tokenType(TokenType.BEARER)
			.expired(false)
			.revoked(false)
			.build();
		tokenRepository.save(token);
	}

	@Override
	public void revokeAllUserTokens(User user) {
		var validUserTokens = tokenRepository.findAllValidTokenByUser(user.getId()); //* Encuentra los tokens NO expirados o NO revocados */
		if (validUserTokens.isEmpty())
			return;
		validUserTokens.forEach(token -> { //* A esos tokens los pone en expirados o revocados */
			token.setExpired(true);
			token.setRevoked(true);
		});
		tokenRepository.saveAll(validUserTokens); //? Actualiza los tokens?
	}

	@Override
	public void refreshToken(HttpServletRequest request,HttpServletResponse response) throws IOException {
		final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
		final String refreshToken;
		final String userEmail;
		if (authHeader == null ||!authHeader.startsWith("Bearer ")) {
			return;
		}
		refreshToken = authHeader.substring(7);
		userEmail = jwtService.extractUsername(refreshToken); //* Extrae el correo del token */
		if (userEmail != null) { 
			var user = this.userRepository.findByEmail(userEmail) //* Comprueba que exista el correo en la base de datos */
					.orElseThrow();
			if (jwtService.isTokenValid(refreshToken, user)) { //* Comprueba que el token sea valido */
				var accessToken = jwtService.generateToken(user); //* Genera un nuevo Token */
				revokeAllUserTokens(user); //* Pone el resto de tokens del usuario en expirado o revocado */
				saveUserToken(user, accessToken); //* Guarda el nuevo token generado */
				var authResponse = AuthenticationResponse.builder() //* Retorna un DTO con el Access y el Refresh Token */
					.accessToken(accessToken)
					.refreshToken(refreshToken)
					.build();
				new ObjectMapper().writeValue(response.getOutputStream(), authResponse);
			}
		}
	}
}
