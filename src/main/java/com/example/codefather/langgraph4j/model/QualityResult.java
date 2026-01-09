package com.example.codefather.langgraph4j.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QualityResult implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 是否通过质检
     */
    private Boolean isValid;

    /**
     * 错误信息列表
     */
    private List<String> errors;

    /**
     * 改进建议
     */
    private List<String> suggestions;
}
