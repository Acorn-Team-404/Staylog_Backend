package com.staylog.staylog.domain.board.dto;

import lombok.*;
import org.apache.ibatis.type.Alias;

import java.time.LocalDateTime;

@Alias("ViewDto")
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViewDto {

    private Long viewId;
    private Long boardId;
    private Long userId;
    private String viewToken;
    private LocalDateTime viewedAt;

}
