package com.staylog.staylog.domain.board.mapper;

import com.staylog.staylog.domain.board.dto.ViewDto;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ViewsMapper {

    public void insertView(ViewDto viewDto);
    public boolean hasRecentView(ViewDto viewDto);
    public int countByBoardId(Long boardId);
}
