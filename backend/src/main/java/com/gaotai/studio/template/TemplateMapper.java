package com.gaotai.studio.template;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TemplateMapper {

    Template findById(@Param("id") String id);

    List<Template> findAll(@Param("category") String category);

    List<String> findCategories();
}
