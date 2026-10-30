package com.gaotai.studio.asset;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AssetMapper {

    int insert(Asset asset);

    Asset findById(@Param("id") String id);
}
