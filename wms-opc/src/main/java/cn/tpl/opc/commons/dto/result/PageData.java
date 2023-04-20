package cn.tpl.opc.commons.dto.result;

import cn.tpl.opc.commons.dto.base.AbsBasePageDTO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Author: Luo GuoWen
 * Email: luoguowen123@qq.com
 * Time: 2023/4/19
 * 分页数据
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class PageData<R> extends AbsBasePageDTO {
    private List<R> data;

    public static <T, R> PageData<R> of(IPage<T> iPage, Function<? super T, ? extends R> data2DTO) {
        PageData<R> pageData = new PageData<>();
        pageData.setCurrentPage(iPage.getCurrent());
        pageData.setTotalPage(iPage.getTotal());
        pageData.setData(iPage.getRecords().stream().map(data2DTO).collect(Collectors.toList()));
        return pageData;
    }

}
