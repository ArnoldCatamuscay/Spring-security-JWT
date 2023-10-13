package example.rolesandpermissions.service;

import java.security.Principal;

import example.rolesandpermissions.dto.request.ChangePasswordRequest;

public interface UserService {
    void delete(Long id);
    void changePassword(ChangePasswordRequest request, Principal connectedUser);
}
