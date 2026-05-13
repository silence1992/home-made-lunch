package com.homemadelunch.util;

import java.util.Random;

/**
 * 随机头像生成器 - 使用DiceBear API生成可爱头像
 */
public class AvatarGenerator {

    private static final String[] STYLES = {
        "adventurer", "adventurer-neutral", "avataaars",
        "big-ears", "big-ears-neutral", "big-smile",
        "bottts", "croodles", "croodles-neutral",
        "fun-emoji", "icons", "identicon",
        "lorelei", "lorelei-neutral", "micah",
        "miniavs", "notionists", "notionists-neutral",
        "open-peeps", "personas", "pixel-art",
        "pixel-art-neutral", "thumbs"
    };

    private static final Random RANDOM = new Random();

    /**
     * 生成随机头像URL
     */
    public static String generate() {
        String style = "fun-emoji";
        String seed = "user_" + System.currentTimeMillis() + "_" + RANDOM.nextInt(10000);
        return "https://api.dicebear.com/7.x/" + style + "/svg?seed=" + seed;
    }
}
