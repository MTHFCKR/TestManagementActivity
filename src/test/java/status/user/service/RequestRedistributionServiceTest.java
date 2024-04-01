package status.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.MockitoAnnotations;
import status.user.entity.Request;
import status.user.enums.RequestStatus;
import status.user.entity.User;
import status.user.enums.UserStatus;
import status.user.repository.RequestRepository;
import status.user.repository.UserRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;


public class RequestRedistributionServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RequestRepository requestRepository;

    @InjectMocks
    private RequestRedistributionService redistributionService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testRedistributeRequests() {

        User userGoingOffline = new User(1L, "user1", "User One", UserStatus.OFFLINE);
        User onlineUser = new User(2L, "user2", "User Two", UserStatus.ONLINE);

        Request request1 = new Request(1L, "Request 1", "Description 1", userGoingOffline, RequestStatus.OPEN);
        Request request2 = new Request(2L, "Request 2", "Description 2", userGoingOffline, RequestStatus.OPEN);

        List<Request> requestsToRedistribute = Arrays.asList(request1, request2);
        List<User> onlineUsers = Arrays.asList(onlineUser);

        when(userRepository.findById(1L)).thenReturn(Optional.of(userGoingOffline));
        when(requestRepository.findByResponsibleAndStatus(userGoingOffline, RequestStatus.OPEN)).thenReturn(requestsToRedistribute);
        when(userRepository.findByStatus(UserStatus.ONLINE)).thenReturn(onlineUsers);

        redistributionService.redistributeRequests(1L);

        verify(requestRepository, times(requestsToRedistribute.size())).save(any(Request.class));
    }
}
