package status.user.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import status.user.entity.Request;
import status.user.enums.RequestStatus;
import status.user.entity.User;
import status.user.enums.UserStatus;
import status.user.repository.RequestRepository;
import status.user.repository.UserRepository;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Сервис для перераспределения запросов между пользователями.
 */
@Service
public class RequestRedistributionService {

    private static final Logger logger = LoggerFactory.getLogger(RequestRedistributionService.class);

    private final UserRepository userRepository;
    private final RequestRepository requestRepository;

    public RequestRedistributionService(UserRepository userRepository, RequestRepository requestRepository) {
        this.userRepository = userRepository;
        this.requestRepository = requestRepository;
    }

    /**
     * Перераспределение открытых запросов с пользователя, переходящего в статус "Оффлайн",
     * на пользователей, находящихся в статусе "Онлайн".
     * userIdGoingOffline Идентификатор пользователя, переходящего в статус "Оффлайн".
     */
    @Transactional
    public void redistributeRequests(Long userIdGoingOffline) {
        logger.info("Начало перераспределения запросов для пользователя с ID: {}", userIdGoingOffline);
        User userGoingOffline = userRepository.findById(userIdGoingOffline).orElseThrow(() -> {
            logger.error("Пользователь с ID {} не найден.", userIdGoingOffline);
            return new IllegalArgumentException("Пользователь не найден");
        });
        List<Request> requestsToRedistribute = requestRepository.findByResponsibleAndStatus(userGoingOffline, RequestStatus.OPEN);
        logger.debug("Найдено {} запросов для перераспределения.", requestsToRedistribute.size());
        List<User> onlineUsers = userRepository.findByStatus(UserStatus.ONLINE);

        onlineUsers.removeIf(user -> user.getId().equals(userIdGoingOffline));

        // Получаем количество открытых запросов для каждого пользователя
        List<Map<String, Object>> openRequestsCount = requestRepository.countOpenRequestsByUser();
        Map<Long, Long> userRequestCounts = openRequestsCount.stream()
                .collect(Collectors.toMap(
                        entry -> (Long) entry.get("userId"),
                        entry -> (Long) entry.get("requestCount"),
                        (existing, replacement) -> existing)); // In case of a key collision, keep the existing value

        if (onlineUsers.isEmpty()) {
            logger.warn("Нет доступных пользователей в статусе ONLINE для перераспределения запросов.");
            throw new IllegalStateException("Нет доступных пользователей в статусе ONLINE для перераспределения запросов.");
        }

        // Перераспределение в соответствии с нагруженностью
        double totalWeight = onlineUsers.stream()
                .mapToDouble(user -> 1.0 / (userRequestCounts.getOrDefault(user.getId(), 0L) + 1))
                .sum();

        Random random = new Random();
        for (Request request : requestsToRedistribute) {
            double value = random.nextDouble() * totalWeight;
            double currentSum = 0;
            User selectedUser = null;
            for (User user : onlineUsers) {
                currentSum += 1.0 / (userRequestCounts.getOrDefault(user.getId(), 0L) + 1);
                if (currentSum >= value) {
                    selectedUser = user;
                    break;
                }
            }

            if (selectedUser != null) {
                request.setResponsible(selectedUser);
                requestRepository.save(request);
                logger.debug("Запрос с ID {} переназначен пользователю с ID {}.", request.getId(), selectedUser.getId());
            }
        }
        logger.info("Перераспределение запросов завершено.");
    }
}