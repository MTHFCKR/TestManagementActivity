package status.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class OpenRequestCountDto {
    private Long userId;
    private Long requestCount;

}
