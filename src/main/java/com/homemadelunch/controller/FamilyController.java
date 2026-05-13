package com.homemadelunch.controller;

import com.homemadelunch.common.Result;
import com.homemadelunch.entity.*;
import com.homemadelunch.service.FamilyService;
import com.homemadelunch.util.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 家庭控制器
 */
@Slf4j
@RestController
@RequestMapping("/family")
public class FamilyController {

    @Autowired
    private FamilyService familyService;

    /**
     * 创建家庭
     */
    @PostMapping("/create")
    public Result<Family> createFamily(HttpServletRequest request, @RequestBody Map<String, String> params) {
        User user = UserContext.getCurrentUser(request);
        String name = params.get("name");
        if (name == null || name.isEmpty()) {
            return Result.fail("家庭名称不能为空哦~");
        }
        Family family = familyService.createFamily(user, name);
        return Result.ok("家庭创建成功~", family);
    }

    /**
     * 获取家庭信息
     */
    @GetMapping("/info")
    public Result<Map<String, Object>> getFamilyInfo(HttpServletRequest request) {
        User user = UserContext.getCurrentUser(request);
        if (user.getFamilyId() == null) {
            return Result.ok(null);
        }
        Family family = familyService.getFamilyById(user.getFamilyId());
        List<User> members = familyService.getFamilyMembers(user.getFamilyId());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("family", family);
        result.put("members", members);
        result.put("isAdmin", "ADMIN".equals(user.getRole()));
        return Result.ok(result);
    }

    /**
     * 获取家庭成员
     */
    @GetMapping("/members")
    public Result<List<User>> getMembers(HttpServletRequest request) {
        User user = UserContext.getCurrentUser(request);
        if (user.getFamilyId() == null) {
            return Result.fail("你还没有加入家庭哦~");
        }
        List<User> members = familyService.getFamilyMembers(user.getFamilyId());
        return Result.ok(members);
    }

    /**
     * 生成邀请链接
     */
    @PostMapping("/invite")
    public Result<InviteLink> createInvite(HttpServletRequest request, @RequestBody Map<String, String> params) {
        User user = UserContext.getCurrentUser(request);
        String role = params.get("role");
        InviteLink link = familyService.createInviteLink(user, role);
        return Result.ok("邀请链接已生成~", link);
    }

    /**
     * 获取邀请链接信息（无需登录）
     */
    @GetMapping("/invite/info/{code}")
    public Result<Map<String, Object>> getInviteInfo(@PathVariable String code) {
        InviteLink link = familyService.getInviteLinkInfo(code);
        Family family = familyService.getFamilyById(link.getFamilyId());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("familyName", family.getName());
        result.put("role", link.getRole());
        result.put("roleName", "COOK".equals(link.getRole()) ? "厨师" : "就餐者");
        result.put("expired", link.getExpireAt().isBefore(java.time.LocalDateTime.now()));
        result.put("used", link.getUsed() == 1);
        return Result.ok(result);
    }

    /**
     * 接受邀请
     */
    @PostMapping("/invite/accept")
    public Result<?> acceptInvite(HttpServletRequest request, @RequestBody Map<String, String> params) {
        User user = UserContext.getCurrentUser(request);
        String code = params.get("code");
        familyService.acceptInvite(user, code);
        return Result.ok("欢迎加入新家庭~ 🎉");
    }

    /**
     * 踢出成员
     */
    @PostMapping("/kick")
    public Result<?> kickMember(HttpServletRequest request, @RequestBody Map<String, Object> params) {
        User user = UserContext.getCurrentUser(request);
        Long targetUserId = Long.parseLong(String.valueOf(params.get("userId")));
        familyService.kickMember(user, targetUserId);
        return Result.ok("已将该成员移出家庭~");
    }
}
