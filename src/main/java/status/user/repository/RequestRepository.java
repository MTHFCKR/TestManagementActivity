package status.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import status.user.entity.Request;
import status.user.enums.RequestStatus;
import status.user.entity.User;

import java.util.List;
import java.util.Map;

public interface RequestRepository extends JpaRepository<Request, Long> {
    List<Request> findByResponsible(User responsible);
    List<Request> findByStatus(RequestStatus status);
    List<Request> findByResponsibleAndStatus(User responsible, RequestStatus status);


     //Пользовательский запрос JPQL для подсчета открытых запросов, сгруппированных по ID пользователя.

    @Query("SELECT r.responsible.id as userId, COUNT(r) as requestCount FROM Request r WHERE r.status = 'OPEN' GROUP BY r.responsible.id")
    List<Map<String, Object>> countOpenRequestsByUser();
}