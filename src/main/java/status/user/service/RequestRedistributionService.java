package status.user.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import status.user.dto.OpenRequestCountDto;
import status.user.entity.Request;
import status.user.enums.RequestStatus;
import status.user.entity.User;
import status.user.enums.UserStatus;
import status.user.repository.RequestRepository;
import status.user.repository.UserRepository;

import java.util.*;
import java.util.stream.Collectors;


@Service
public class RequestRedistributionService {

    private static final Logger logger = LoggerFactory.getLogger(RequestRedistributionService.class);

    private final UserRepository userRepository;
    private final RequestRepository requestRepository;

    public RequestRedistributionService(UserRepository userRepository, RequestRepository requestRepository) {
        this.userRepository = userRepository;
        this.requestRepository = requestRepository;
    }

    @Transactional
    public void redistributeRequests(Long userIdGoingOffline) {
        logger.info("Начало перераспределения запросов для пользователя с ID: {}", userIdGoingOffline);
        User userGoingOffline = findUserById(userIdGoingOffline);
        List<Request> requestsToRedistribute = findRequestsToRedistribute(userGoingOffline);
        List<User> onlineUsers = findOnlineUsers(userIdGoingOffline);
        Map<Long, Long> userRequestCounts = countOpenRequestsByUser();

        if (onlineUsers.isEmpty()) {
            logger.warn("Нет доступных пользователей в статусе ONLINE для перераспределения запросов.");
            throw new IllegalStateException("Нет доступных пользователей в статусе ONLINE для перераспределения запросов.");
        }

        redistributeRequestsAmongUsers(requestsToRedistribute, onlineUsers, userRequestCounts);
        logger.info("Перераспределение запросов завершено.");
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> {
            logger.error("Пользователь с ID {} не найден.", userId);
            return new IllegalArgumentException("Пользователь не найден");
        });
    }

    private List<Request> findRequestsToRedistribute(User user) {
        return requestRepository.findByResponsibleAndStatus(user, RequestStatus.OPEN);
    }

    private List<User> findOnlineUsers(Long userIdGoingOffline) {
        List<User> onlineUsers = userRepository.findByStatus(UserStatus.ONLINE);
        onlineUsers.removeIf(user -> user.getId().equals(userIdGoingOffline));
        return onlineUsers;
    }

    private Map<Long, Long> countOpenRequestsByUser() {
        List<OpenRequestCountDto> openRequestsCount = requestRepository.countOpenRequestsByUser();
        return openRequestsCount.stream()
                .collect(Collectors.toMap(
                        OpenRequestCountDto::getUserId,
                        OpenRequestCountDto::getRequestCount,
                        (existing, replacement) -> existing));
    }
    private void redistributeRequestsAmongUsers(List<Request> requestsToRedistribute, List<User> onlineUsers, Map<Long, Long> userRequestCounts) {
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
    }
}