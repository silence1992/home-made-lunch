package com.homemadelunch.service;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.homemadelunch.common.BizException;
import com.homemadelunch.entity.*;
import com.homemadelunch.mapper.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 家庭服务
 */
@Slf4j
@Service
public class FamilyService {

    @Autowired
    private FamilyMapper familyMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private RecipeMapper recipeMapper;
    @Autowired
    private RecipeTagMapper recipeTagMapper;
    @Autowired
    private RecipeTagRelationMapper recipeTagRelationMapper;
    @Autowired
    private InviteLinkMapper inviteLinkMapper;
    @Autowired
    private OperationLogService logService;

    /**
     * 创建家庭
     */
    @Transactional
    public Family createFamily(User user, String familyName) {
        // 检查用户是否已加入家庭
        if (user.getFamilyId() != null) {
            throw new BizException("你已经有家庭啦，不能再创建新的哦~");
        }

        // 创建家庭
        Family family = new Family();
        family.setName(familyName);
        family.setAdminId(user.getId());
        family.setInviteCode(IdUtil.simpleUUID().substring(0, 8));
        familyMapper.insert(family);

        // 更新用户信息
        user.setFamilyId(family.getId());
        user.setRole("ADMIN");
        userMapper.updateById(user);

        // 创建默认标签和菜谱
        createDefaultTagsAndRecipes(family.getId());

        logService.log(user, "FAMILY", "CREATE", "创建了家庭「" + familyName + "」");
        return family;
    }

    /**
     * 创建默认标签和菜谱
     */
    private void createDefaultTagsAndRecipes(Long familyId) {
        // 创建默认标签：随意
        RecipeTag tagRandom = new RecipeTag();
        tagRandom.setFamilyId(familyId);
        tagRandom.setName("随意");
        tagRandom.setIsDefault(1);
        recipeTagMapper.insert(tagRandom);

        // 创建默认标签：停休
        RecipeTag tagOff = new RecipeTag();
        tagOff.setFamilyId(familyId);
        tagOff.setName("停休");
        tagOff.setIsDefault(1);
        recipeTagMapper.insert(tagOff);

        // 创建默认菜谱：随意
        Recipe recipeRandom = new Recipe();
        recipeRandom.setFamilyId(familyId);
        recipeRandom.setName("随意");
        recipeRandom.setDescription("今天吃什么由厨师决定，期待惊喜~");
        recipeRandom.setIsDefault(1);
        recipeMapper.insert(recipeRandom);

        // 绑定标签
        RecipeTagRelation rel1 = new RecipeTagRelation();
        rel1.setRecipeId(recipeRandom.getId());
        rel1.setTagId(tagRandom.getId());
        recipeTagRelationMapper.insert(rel1);

        // 创建默认菜谱：停休
        Recipe recipeOff = new Recipe();
        recipeOff.setFamilyId(familyId);
        recipeOff.setName("停休");
        recipeOff.setDescription("今天休息，不供餐哦~");
        recipeOff.setIsDefault(1);
        recipeMapper.insert(recipeOff);

        // 绑定标签
        RecipeTagRelation rel2 = new RecipeTagRelation();
        rel2.setRecipeId(recipeOff.getId());
        rel2.setTagId(tagOff.getId());
        recipeTagRelationMapper.insert(rel2);
    }

    /**
     * 获取家庭信息
     */
    public Family getFamilyById(Long familyId) {
        Family family = familyMapper.selectById(familyId);
        if (family == null) {
            throw new BizException("家庭不存在~");
        }
        return family;
    }

    /**
     * 获取家庭成员列表
     */
    public List<User> getFamilyMembers(Long familyId) {
        return userMapper.selectList(
            new LambdaQueryWrapper<User>()
                .eq(User::getFamilyId, familyId)
                .orderByAsc(User::getCreatedAt)
        );
    }

    /**
     * 生成邀请链接
     */
    public InviteLink createInviteLink(User user, String role) {
        if (user.getFamilyId() == null) {
            throw new BizException("你还没有家庭，请先创建一个吧~");
        }
        if (!"ADMIN".equals(user.getRole())) {
            throw new BizException("只有管理员才能邀请新成员哦~");
        }
        if (!"COOK".equals(role) && !"DINER".equals(role)) {
            throw new BizException("邀请角色只能是厨师或就餐者哦~");
        }

        InviteLink link = new InviteLink();
        link.setFamilyId(user.getFamilyId());
        link.setCode(IdUtil.simpleUUID().substring(0, 16));
        link.setRole(role);
        link.setCreatedBy(user.getId());
        link.setExpireAt(LocalDateTime.now().plusDays(7));
        link.setUsed(0);
        link.setCreatedAt(LocalDateTime.now());
        inviteLinkMapper.insert(link);

        logService.log(user, "FAMILY", "INVITE",
            "生成了" + ("COOK".equals(role) ? "厨师" : "就餐者") + "邀请链接");
        return link;
    }

    /**
     * 获取邀请链接信息
     */
    public InviteLink getInviteLinkInfo(String code) {
        InviteLink link = inviteLinkMapper.selectOne(
            new LambdaQueryWrapper<InviteLink>().eq(InviteLink::getCode, code)
        );
        if (link == null) {
            throw new BizException("邀请链接不存在~");
        }
        return link;
    }

    /**
     * 接受邀请加入家庭
     */
    @Transactional
    public void acceptInvite(User user, String code) {
        if (user.getFamilyId() != null) {
            throw new BizException("你已经有家庭了，不能再加入其它家庭哦~");
        }

        InviteLink link = inviteLinkMapper.selectOne(
            new LambdaQueryWrapper<InviteLink>().eq(InviteLink::getCode, code)
        );
        if (link == null) {
            throw new BizException("邀请链接不存在~");
        }
        if (link.getUsed() == 1) {
            throw new BizException("这个邀请链接已经被使用过啦~");
        }
        if (link.getExpireAt().isBefore(LocalDateTime.now())) {
            throw new BizException("邀请链接已过期，请让管理员重新生成~");
        }

        // 更新用户
        user.setFamilyId(link.getFamilyId());
        user.setRole(link.getRole());
        userMapper.updateById(user);

        // 标记链接已使用
        link.setUsed(1);
        link.setUsedBy(user.getId());
        inviteLinkMapper.updateById(link);

        Family family = familyMapper.selectById(link.getFamilyId());
        logService.log(user, "FAMILY", "JOIN",
            user.getNickname() + " 加入了家庭「" + family.getName() + "」，角色: " +
            ("COOK".equals(link.getRole()) ? "厨师" : "就餐者"));
    }

    /**
     * 踢出成员
     */
    @Transactional
    public void kickMember(User admin, Long targetUserId) {
        if (!"ADMIN".equals(admin.getRole())) {
            throw new BizException("只有管理员才能踢出成员哦~");
        }
        if (admin.getId().equals(targetUserId)) {
            throw new BizException("不能踢出自己哦~");
        }

        User target = userMapper.selectById(targetUserId);
        if (target == null || !admin.getFamilyId().equals(target.getFamilyId())) {
            throw new BizException("该用户不在你的家庭中~");
        }

        target.setFamilyId(null);
        target.setRole(null);
        userMapper.updateById(target);

        logService.log(admin, "FAMILY", "KICK",
            "将「" + target.getNickname() + "」移出了家庭");
    }
}
