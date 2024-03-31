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
                        // В случае дублирования ключей сохраняем существующее значение
                        (existing, replacement) -> existing));

        if (onlineUsers.isEmpty()) {
            logger.warn("Нет доступных пользователей в статусе ONLINE для перераспределения запросов.");
            throw new IllegalStateException("Нет доступных пользователей в статусе ONLINE для перераспределения запросов.");
        }

        for (Request request : requestsToRedistribute) {
            onlineUsers.sort(Comparator.comparingLong(user -> userRequestCounts.getOrDefault(user.getId(), 0L)));
            User newResponsible = onlineUsers.get(0);
            request.setResponsible(newResponsible);
            requestRepository.save(request);
            logger.debug("Запрос с ID {} переназначен пользователю с ID {}.", request.getId(), newResponsible.getId());
        }
        logger.info("Перераспределение запросов завершено.");
    }
}