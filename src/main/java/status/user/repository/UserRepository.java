package status.user.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import status.user.entity.User;
import status.user.enums.UserStatus;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findByStatus(UserStatus status);
}