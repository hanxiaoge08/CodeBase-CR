package com.hxg.service;

import com.hxg.context.ExecutionContext;
import com.hxg.model.dto.CatalogueStruct;
import com.hxg.model.dto.GenCatalogueDTO;
import com.hxg.model.entity.Catalogue;
import com.hxg.model.vo.CatalogueListVo;
import org.springframework.scheduling.annotation.Async;

import java.util.List;

/**
 * @author hxg
 * @description: 文档目录生成服务接口
 * @date 2025/7/20 20:48
 */
public interface ICatalogueService {
    public GenCatalogueDTO generateCatalogue(String fileTree, ExecutionContext context);

    public CatalogueStruct processCatalogueStruct(String result);

    public List<Catalogue> saveCatalogueStruct(ExecutionContext context, CatalogueStruct catalogueStruct);

    public void parallelGenerateCatalogueDetail(String fileTree, GenCatalogueDTO genCatalogueDTO, String localPath);

    @Async("GenCatalogueDetailExcutor")
    public void generateCatalogueDetail(Catalogue catalogue, String fileTree, CatalogueStruct catalogueStruct, String localPath);

    public void deleteCatalogueByTaskId(String taskId);

    public List<Catalogue> getCatalogueByTaskId(String taskId);

    /**
     * 根据taskId获取目录树形结构
     */
    public List<CatalogueListVo> getCatalogueTreeByTaskId(String taskId);
}
