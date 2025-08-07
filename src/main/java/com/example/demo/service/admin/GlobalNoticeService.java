package com.example.demo.service.admin;

import com.example.demo.domain.dto.GlobalNoticeDTO;
import com.example.demo.domain.entity.GlobalNotice;
import com.example.demo.repository.GlobalNoticeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class GlobalNoticeService {
    private final GlobalNoticeRepository globalNoticeRepository;

    public GlobalNoticeDTO createGlobalNotice(GlobalNoticeDTO dto) {
        GlobalNotice globalNotice = GlobalNotice.builder()
                .gTitle(dto.getGTitle())
                .gContents(dto.getGContents())
                .build();
        globalNotice = globalNoticeRepository.save(globalNotice);
        return GlobalNoticeDTO.from(globalNotice);
    }

    public GlobalNoticeDTO updateGlobalNotice(Long id, GlobalNoticeDTO dto) {
        GlobalNotice globalNotice = globalNoticeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("글로벌 공지를 찾을 수 없습니다."));
        globalNotice.setGTitle(dto.getGTitle());
        globalNotice.setGContents(dto.getGContents());
        globalNotice = globalNoticeRepository.save(globalNotice);
        return GlobalNoticeDTO.from(globalNotice);
    }

    public void deleteGlobalNotice(Long id) {
        globalNoticeRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<GlobalNoticeDTO> getAllGlobalNotices() {
        return globalNoticeRepository.findAll().stream()
                .map(GlobalNoticeDTO::from)
                .collect(Collectors.toList());
    }

    //전체공지 상세보기
    @Transactional(readOnly = true)
    public GlobalNoticeDTO getGlobalNoticeById(Long id) {
        GlobalNotice notice = globalNoticeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Global Notice not found with id: " + id));
        return GlobalNoticeDTO.from(notice);
    }
}
