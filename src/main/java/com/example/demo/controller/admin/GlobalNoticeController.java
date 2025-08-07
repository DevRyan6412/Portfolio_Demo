package com.example.demo.controller.admin;

import com.example.demo.domain.dto.GlobalNoticeDTO;
import com.example.demo.service.admin.GlobalNoticeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequestMapping("/admin/global-notices")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class GlobalNoticeController {
    private final GlobalNoticeService globalNoticeService;

    @GetMapping
    public String listGlobalNotices(Model model) {
        model.addAttribute("globalNotices", globalNoticeService.getAllGlobalNotices());
        return "admin/manage/globalNoticeList"; // 별도의 템플릿
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("globalNoticeDto", new GlobalNoticeDTO());
        return "admin/manage/globalNoticeCreate";
    }

    @PostMapping("/create")
    public String createGlobalNotice(@Valid @ModelAttribute("globalNoticeDto") GlobalNoticeDTO dto,
                                     BindingResult bindingResult,
                                     RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "admin/manage/globalNoticeCreate";
        }
        globalNoticeService.createGlobalNotice(dto);
        redirectAttributes.addFlashAttribute("message", "글로벌 공지가 생성되었습니다.");
        return "redirect:/admin/global-notices";
    }

    // 수정 페이지 표시 (GET)
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        GlobalNoticeDTO dto = globalNoticeService.getGlobalNoticeById(id);
        model.addAttribute("globalNoticeDto", dto);
        return "admin/manage/globalNoticeEdit";
    }

    // 수정 처리 (POST)
    @PostMapping("/edit/{id}")
    public String updateGlobalNotice(@PathVariable Long id,
                                     @Valid @ModelAttribute("globalNoticeDto") GlobalNoticeDTO dto,
                                     BindingResult bindingResult,
                                     RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "admin/manage/globalNoticeEdit";
        }
        globalNoticeService.updateGlobalNotice(id, dto);
        redirectAttributes.addFlashAttribute("message", "글로벌 공지가 수정되었습니다.");
        return "redirect:/admin/global-notices";
    }

    // 삭제 처리 (POST)
    @PostMapping("/delete/{id}")
    public String deleteGlobalNotice(@PathVariable Long id,
                                     RedirectAttributes redirectAttributes) {
        globalNoticeService.deleteGlobalNotice(id);
        redirectAttributes.addFlashAttribute("message", "글로벌 공지가 삭제되었습니다.");
        return "redirect:/admin/global-notices";
    }
}
