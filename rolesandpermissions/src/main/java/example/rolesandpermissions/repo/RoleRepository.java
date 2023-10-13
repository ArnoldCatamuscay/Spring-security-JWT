package example.rolesandpermissions.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import example.rolesandpermissions.model.Role;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);
}
