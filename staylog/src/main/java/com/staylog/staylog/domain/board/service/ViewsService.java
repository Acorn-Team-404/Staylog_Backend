package com.staylog.staylog.domain.board.service;

import com.staylog.staylog.domain.board.dto.ViewDto;

public interface ViewsService {

    public int CountViewsByBoardId(Long boardId);
    public void insertView(ViewDto viewDto);

}
