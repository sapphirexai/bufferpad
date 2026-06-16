package cn.tpl.opc.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.tpl.opc.commons.constant.Constants;
import cn.tpl.opc.commons.dto.ResultDTO;
import cn.tpl.opc.commons.dto.result.CushionDetailDTO;
import cn.tpl.opc.commons.dto.result.CushionInfoDTO;
import cn.tpl.opc.commons.dto.result.ExportCushionDetailDTO;
import cn.tpl.opc.commons.dto.result.ExportCushionInfoDTO;
import cn.tpl.opc.commons.dto.result.PageData;
import cn.tpl.opc.commons.scheme.request.ManualCushionInfoScheme;
import cn.tpl.opc.commons.scheme.request.ModifyCushionInfoScheme;
import cn.tpl.opc.commons.scheme.request.QueryCushionDetailPageScheme;
import cn.tpl.opc.commons.scheme.request.QueryCushionInfoPageScheme;
import cn.tpl.opc.entity.CushionDetailEntity;
import cn.tpl.opc.mapper.CushionDetailEntityMapper;
import cn.tpl.opc.service.ICushionInfoService;
import cn.tpl.opc.service.IDeviceInstallPositionService;
import cn.tpl.opc.util.EasyExcelUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 缓冲垫相关接口.
 */
@Tag(name = "缓冲垫", description = "缓冲垫相关接口")
@Slf4j
@RestController
@RequestMapping("/cushion")
public class CushionController {
    @Resource
    private ICushionInfoService cushionInfoService;
    @Resource
    private CushionDetailEntityMapper cushionDetailEntityMapper;
    @Resource
    private IDeviceInstallPositionService installPositionService;

    @Operation(summary = "分页查询缓冲垫列表")
    @GetMapping("/cushionsPage")
    public ResultDTO<PageData<CushionInfoDTO>> cushionsPage(
            @Parameter(description = "查询条件")
            QueryCushionInfoPageScheme scheme) {
        try {
            log.debug("cushionsPage, scheme => {}", scheme);
            return ResultDTO.success(cushionInfoService.listByPage(scheme));
        } catch (Exception e) {
            return ResultDTO.exception(e);
        }
    }

    @Operation(summary = "通过二维码模糊查询缓冲垫列表")
    @GetMapping("/cushions")
    public ResultDTO<List<CushionInfoDTO>> listByQrCodeQuery(@Parameter(description = "二维码")
                                                             @RequestParam String qrCode) {
        return doListByQrCode(qrCode);
    }

    @Operation(summary = "通过二维码模糊查询缓冲垫列表，兼容旧路径")
    @GetMapping("/cushions/{qrCode}")
    public ResultDTO<List<CushionInfoDTO>> listByQrCode(@Parameter(description = "二维码")
                                                        @PathVariable String qrCode) {
        return doListByQrCode(qrCode);
    }

    private ResultDTO<List<CushionInfoDTO>> doListByQrCode(String qrCode) {
        try {
            log.info("listByQrCode, qrCode => {}", qrCode);
            if (StringUtils.isEmpty(qrCode)) {
                return ResultDTO.failure("缓冲垫编码不能为空！");
            }
            return ResultDTO.success(cushionInfoService.listByQrCode(qrCode));
        } catch (Exception e) {
            return ResultDTO.exception(e);
        }
    }

    @Operation(summary = "通过二维码分页查询对应缓冲垫明细")
    @GetMapping("/detailsPage")
    public ResultDTO<PageData<CushionDetailDTO>> listDetailsPageByQrCode(
            @Parameter(description = "查询条件")
            QueryCushionDetailPageScheme scheme) {
        try {
            log.debug("detailsPage, scheme => {}", scheme);
            return ResultDTO.success(cushionInfoService.listDetailsByPage(scheme));
        } catch (Exception e) {
            return ResultDTO.exception(e);
        }
    }

    @Operation(summary = "手动输入缓冲垫二维码")
    @PostMapping("/manualCushionInfo")
    public ResultDTO<CushionInfoDTO> manualCushionInfo(
            @Valid
            @Parameter(description = "手动扫码请求")
            @RequestBody ManualCushionInfoScheme scheme) {
        return doManualCushionInfo(scheme.getWorkLine(), scheme.getQrCode());
    }

    @Operation(summary = "手动输入缓冲垫二维码，兼容旧路径")
    @PostMapping("/manualCushionInfo/{workLine}/{qrCode}")
    public ResultDTO<CushionInfoDTO> manualCushionInfo(
            @Parameter(description = "产线")
            @PathVariable("workLine") Integer workLine,
            @Parameter(description = "二维码")
            @PathVariable("qrCode") String qrCode) {
        return doManualCushionInfo(workLine, qrCode);
    }

    private ResultDTO<CushionInfoDTO> doManualCushionInfo(Integer workLine, String qrCode) {
        try {
            log.info("manualCushionInfo, workLine => {}, qrCode => {}", workLine, qrCode);
            if (workLine == null) {
                return ResultDTO.failure("产线不能为空！");
            }
            if (StringUtils.isEmpty(qrCode)) {
                return ResultDTO.failure("缓冲垫编码不能为空！");
            }
            return cushionInfoService.onQrCodeReceived(null, workLine, null, null, Constants.SCANNER_POSITION_MANUAL, null, qrCode);
        } catch (Exception e) {
            return ResultDTO.exception(e);
        }
    }

    @Operation(summary = "批量修改缓冲垫信息")
    @PostMapping("/cushions")
    public ResultDTO<?> modifyCushionInfo(
            @Parameter(description = "修改缓冲垫信息协议")
            @RequestBody ModifyCushionInfoScheme scheme) {
        try {
            log.info("modifyCushionInfo, scheme => {}", scheme);
            Integer maxUseCount = scheme.getMaxUseCount();
            if (maxUseCount == null) {
                return ResultDTO.failure("最大使用次数不能为空！");
            }
            if (maxUseCount <= 0) {
                return ResultDTO.failure("最大使用次数必须大于0！");
            }

            List<Long> ids = scheme.getIds();
            if (CollectionUtils.isEmpty(ids)) {
                return ResultDTO.failure("需修改的缓冲垫ID不能为空!");
            }

            boolean result = cushionInfoService.modifyMaxUseCountByIds(scheme);
            return result ? ResultDTO.success() : ResultDTO.failure(null);
        } catch (Exception e) {
            return ResultDTO.exception(e);
        }
    }

    @Operation(summary = "批量导出缓冲垫信息")
    @PostMapping("/cushions/excel")
    public ResultDTO<Boolean> exportCushions(
            @Parameter(hidden = true) HttpServletResponse response,
            @Parameter(description = "需要导出的缓冲垫ID列表")
            @RequestBody List<Long> ids) {
        try {
            log.info("exportCushions, ids => {}", ids);
            if (CollectionUtils.isEmpty(ids)) {
                return ResultDTO.failure("需导出的缓冲垫ID不能为空!");
            }

            List<CushionInfoDTO> cushionInfos = cushionInfoService.listByIds(ids);
            if (CollectionUtils.isEmpty(cushionInfos)) {
                return ResultDTO.failure("无对应数据");
            }

            List<ExportCushionInfoDTO> exportDatas = new ArrayList<>();
            for (CushionInfoDTO data : cushionInfos) {
                ExportCushionInfoDTO exportData = new ExportCushionInfoDTO();
                BeanUtil.copyProperties(data, exportData);
                Integer scannerSeq = data.getScannerSeq();
                exportData.setCreatedDate(DateUtil.formatDateTime(data.getCreatedDate()));
                exportData.setLastScanDate(DateUtil.formatDateTime(data.getLastScanDate()));
                if (CharSequenceUtil.isEmpty(exportData.getScannerPosition())) {
                    exportData.setScannerPosition(convertSeq2Pos(scannerSeq));
                }
                exportDatas.add(exportData);
            }
            doExportExcel("Cushion_", exportDatas, ExportCushionInfoDTO.class, response);
            return ResultDTO.success();
        } catch (Exception e) {
            return ResultDTO.exception(e);
        }
    }

    @Operation(summary = "批量导出指定缓冲垫明细")
    @GetMapping("/details/excel")
    public ResultDTO<Boolean> exportCushionDetails(
            @Parameter(hidden = true) HttpServletResponse response,
            @DateTimeFormat(pattern = "yyyyMMdd") Date startTime,
            @DateTimeFormat(pattern = "yyyyMMdd") Date endTime) {
        try {
            List<CushionDetailEntity> entityList = cushionDetailEntityMapper.selectList(new LambdaQueryWrapper<CushionDetailEntity>()
                    .ge(startTime != null, CushionDetailEntity::getCreatedDate, startTime)
                    .le(endTime != null, CushionDetailEntity::getCreatedDate, endTime));
            if (CollectionUtils.isEmpty(entityList)) {
                return ResultDTO.failure("无对应数据");
            }

            List<ExportCushionDetailDTO> exportData = BeanUtil.copyToList(entityList, ExportCushionDetailDTO.class);
            for (ExportCushionDetailDTO item : exportData) {
                item.setScannerPosition(ObjectUtil.isEmpty(item.getScannerPosition()) ? convertSeq2Pos(item.getScannerSeq()) : item.getScannerPosition());
            }
            doExportExcel("Cushion_Details_", exportData, ExportCushionDetailDTO.class, response);
            return ResultDTO.success();
        } catch (Exception e) {
            return ResultDTO.exception(e);
        }
    }

    private String convertSeq2Pos(Integer scannerSeq) {
        String position = installPositionService.getNameById(scannerSeq);
        return position == null ? "" : position;
    }

    private <T> void doExportExcel(String fileName, List<T> exportDatas, Class<?> template, HttpServletResponse response) throws IOException {
        fileName = fileName + DateUtil.format(new Date(), "yyyyMMddHHmmss") + ".xls";
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
        response.setHeader("Content-Disposition", "attachment;filename=" + new String(fileName.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1));
        EasyExcelUtils.export(response.getOutputStream(), exportDatas, template);
    }
}
