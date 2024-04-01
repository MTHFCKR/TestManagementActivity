package status.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import status.user.dto.OpenRequestCountDto;
import status.user.entity.Request;
import status.user.enums.RequestStatus;
import status.user.entity.User;
import java.util.List;


public interface RequestRepository extends JpaRepository<Request, Long> {
    List<Request> findByResponsible(User responsible);
    List<Request> findByStatus(RequestStatus status);
    List<Request> findByResponsibleAndStatus(User responsible, RequestStatus status);

    @Query("SELECT new status.user.dto.OpenRequestCountDto(r.responsible.id, COUNT(r)) FROM Request r WHERE r.status = 'OPEN' GROUP BY r.responsible.id")
    List<OpenRequestCountDto> countOpenRequestsByUser();
}