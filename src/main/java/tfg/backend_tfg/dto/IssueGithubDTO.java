package tfg.backend_tfg.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IssueGithubDTO {
    private Integer number;
    private String title;
    private String state;
    private LocalDateTime createdAt;
    private LocalDateTime closedAt;
    private String type; // "USER_STORY" o "TASK"
    private List<String> assignees; // Los usernames de GitHub
}