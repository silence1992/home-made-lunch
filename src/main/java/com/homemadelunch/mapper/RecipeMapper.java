package com.homemadelunch.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homemadelunch.entity.Recipe;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RecipeMapper extends BaseMapper<Recipe> {
}
