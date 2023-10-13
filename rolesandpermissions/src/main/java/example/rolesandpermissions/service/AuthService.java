package example.rolesandpermissions.service;

import java.io.IOException;

import example.rolesandpermissions.dto.request.AuthenticationRequest;
import example.rolesandpermissions.dto.request.RegisterRequest;
import example.rolesandpermissions.dto.response.AuthenticationResponse;
import example.rolesandpermissions.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {
    AuthenticationResponse register(RegisterRequest request);
    AuthenticationResponse authenticate(AuthenticationRequest request);
	void saveUserToken(User user, String jwtToken);
	void revokeAllUserTokens(User user);
	void refreshToken(HttpServletRequest request,HttpServletResponse response) throws IOException;
}