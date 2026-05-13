package com.homemadelunch.util;

import java.util.Random;

/**
 * 随机中文用户名生成器
 */
public class NicknameGenerator {

    private static final String[] ADJECTIVES = {
        "快乐的", "温暖的", "可爱的", "甜蜜的", "阳光的",
        "幸福的", "开心的", "美味的", "香甜的", "元气的",
        "活力的", "暖心的", "萌萌的", "乖巧的", "勤劳的",
        "贪吃的", "爱笑的", "软糯的", "清新的", "灵动的"
    };

    private static final String[] NOUNS = {
        "小厨师", "美食家", "饭团", "小笼包", "豆包",
        "汤圆", "饺子", "小馒头", "布丁", "麻薯",
        "年糕", "糯米团", "小蛋糕", "奶茶", "棉花糖",
        "小面包", "蜜桃", "草莓", "芒果", "西瓜"
    };

    private static final Random RANDOM = new Random();

    /**
     * 生成随机中文昵称
     */
    public static String generate() {
        String adj = ADJECTIVES[RANDOM.nextInt(ADJECTIVES.length)];
        String noun = NOUNS[RANDOM.nextInt(NOUNS.length)];
        int num = RANDOM.nextInt(100);
        return adj + noun + num;
    }
}
