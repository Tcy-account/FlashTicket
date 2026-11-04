package com.flashticket.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashticket.inventory.entity.InventoryLedger;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 库存流水数据访问（表 ft_inventory_ledger），不可变账本。
 *
 * <p>仅插入与查询，无更新/删除；operation_id 唯一兜底防重复写流水。
 */
@Mapper
public interface InventoryLedgerMapper extends BaseMapper<InventoryLedger> {

    /** 某票档的流水（后台查询，按 id 倒序）。 */
    @Select("SELECT * FROM ft_inventory_ledger WHERE tier_id = #{tierId} ORDER BY id DESC")
    List<InventoryLedger> findByTierId(@Param("tierId") Long tierId);
}
