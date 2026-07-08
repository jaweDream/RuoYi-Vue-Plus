package org.dromara.chain.api.model;

import lombok.Value;
import org.dromara.chain.api.enums.FinalityType;

import java.io.Serial;
import java.io.Serializable;

/**
 * 链终局性描述(终局模型 + 建议确认深度)
 *
 * @author jarvey
 */
@Value(staticConstructor = "of")
public class ChainFinality implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 终局性模型
     */
    FinalityType type;

    /**
     * 建议入账确认深度(确定终局链为 0)
     */
    int recommendedConfirmations;

}
