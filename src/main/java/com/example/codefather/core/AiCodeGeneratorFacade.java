package com.example.codefather.core;

import com.example.codefather.ai.AiCodeGeneratorService;
import com.example.codefather.ai.AiCodeGeneratorServiceFactory;
import com.example.codefather.ai.model.HtmlCodeResult;
import com.example.codefather.ai.model.MultiFileCodeResult;
import com.example.codefather.core.parser.CodeParserExecutor;
import com.example.codefather.core.saver.CodeFileSaverExecutor;
import com.example.codefather.exception.BusinessException;
import com.example.codefather.exception.ErrorCode;
import com.example.codefather.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;

/**
 * AI代码生成器门面类，组合生成和保存功能（门面模式）
 */
@Slf4j
@Service
public class AiCodeGeneratorFacade {

//    @Resource
//    private AiCodeGeneratorService aiCodeGeneratorService;

    @Resource
    private AiCodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;


    /**
     * 统一入口，根据类型生成代码并保存
     *
     * @param userMessage 用户消息
     * @param codeGenTypeEnum 生成类型
     * @param appId 应用ID
     * @return 保存的目录
     */
    public File generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }
        // 根据appId获取AI服务实例
        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId);
        return switch (codeGenTypeEnum) {
            case HTML -> {
                HtmlCodeResult htmlCodeResult = aiCodeGeneratorService.generateHtmlCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(htmlCodeResult, codeGenTypeEnum, appId);
            }
            case MULTI_FILE -> {
                MultiFileCodeResult multiFileCodeResult = aiCodeGeneratorService.generateMultiFileCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(multiFileCodeResult, codeGenTypeEnum, appId);
            }
            default -> {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的生成类型" + codeGenTypeEnum.getValue());
            }
        };
    }

    /**
     * 统一入口，根据类型生成代码并保存（流式）
     *
     * @param userMessage 用户消息
     * @param codeGenTypeEnum 生成类型
     * @param appId 应用ID
     * @return 流式结果
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }
        // 根据appId获取AI服务实例
        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId);
        return switch (codeGenTypeEnum) {
            case HTML -> {
                Flux<String> chatResult = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
                yield processCodeStream(chatResult, codeGenTypeEnum, appId);
            }
            case MULTI_FILE -> {
                Flux<String> chatResult = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
                yield processCodeStream(chatResult, codeGenTypeEnum, appId);
            }
            default -> {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的生成类型" + codeGenTypeEnum.getValue());
            }
        };
    }

    /**
     * 通用代码流处理方法
     *
     * @param codeStream 模型返回的流式结果
     * @param codeGenType 生成类型
     * @param appid 应用ID
     * @return 流式结果
     */
    private Flux<String> processCodeStream(Flux<String> codeStream, CodeGenTypeEnum codeGenType, Long appid) {
        StringBuilder resultBuilder = new StringBuilder();
        // 流式处理
        return codeStream
                .doOnNext(chunk -> {
                    // 实时收集模型返回的流式结果
                    resultBuilder.append(chunk);
                })
                .doOnComplete(() -> {
                    // 流式返回完成后提取并保存代码
                    try {
                        String completeResult = resultBuilder.toString();
                        // 使用解析执行器解析HTML代码
                        Object codeResult = CodeParserExecutor.executeParser(completeResult, codeGenType);
                        // 使用保存执行器保存代码
                        File saveDir = CodeFileSaverExecutor.executeSaver(codeResult, codeGenType, appid);
                        log.info("保存文件成功：{}", saveDir.getAbsolutePath());
                    } catch (Exception e) {
                        log.error("保存文件失败：{}", e.getMessage());
                    }
                });
    }





//----------------------------------------------------------------------------------------------------------------------
//    /**
//     * 生成HTML代码并保存（流式）
//     *
//     * @param userMessage 用户消息
//     * @return 流式结果
//     */
//    @Deprecated
//    private Flux<String> generateAndSaveHtmlCodeStream(String userMessage) {
//        Flux<String> chatResult = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
//        StringBuilder resultBuilder = new StringBuilder();
//        // 流式处理
//        return chatResult
//                .doOnNext(chunk -> {
//                    // 实时收集模型返回的流式结果
//                    resultBuilder.append(chunk);
//                })
//                .doOnComplete(() -> {
//                    // 流式返回完成后提取并保存代码
//                    try {
//                        // 解析HTML代码
//                        HtmlCodeResult htmlCodeResult = CodeParser.parseHtmlCode(resultBuilder.toString());
//                        // 保存代码
//                        File saveDir = CodeFileSaver.saveHtmlCodeResult(htmlCodeResult);
//                        log.info("保存文件成功：{}", saveDir.getAbsolutePath());
//                    } catch (Exception e) {
//                        log.error("保存文件失败：{}", e.getMessage());
//                    }
//                });
//    }
//
//    /**
//     * 生成多文件模式的代码并保存（流式）
//     *
//     * @param userMessage 用户消息
//     * @return 流式结果
//     */
//    @Deprecated
//    private Flux<String> generateAndSaveMultiFileCodeStream(String userMessage) {
//        Flux<String> chatResult = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
//        StringBuilder resultBuilder = new StringBuilder();
//        // 流式处理
//        return chatResult
//                .doOnNext(chunk -> {
//                    // 实时收集模型返回的流式结果
//                    resultBuilder.append(chunk);
//                })
//                .doOnComplete(() -> {
//                    try {
//                        // 解析多文件代码
//                        MultiFileCodeResult multiFileCodeResult = CodeParser.parseMultiFileCode(resultBuilder.toString());
//                        // 保存代码
//                        File saveDir = CodeFileSaver.saveMultiFileCodeResult(multiFileCodeResult);
//                        log.info("保存文件成功：{}", saveDir.getAbsolutePath());
//                    } catch (Exception e) {
//                        log.error("保存文件失败：{}", e.getMessage());
//                    }
//                });
//    }
//
//    /**
//     * 生成HTML代码并保存
//     *
//     * @param userMessage 用户消息
//     * @return 保存的目录
//     */
//    @Deprecated
//    private File generateAndSaveHtmlCode(String userMessage) {
//        HtmlCodeResult htmlCodeResult = aiCodeGeneratorService.generateHtmlCode(userMessage);
//        return CodeFileSaver.saveHtmlCodeResult(htmlCodeResult);
//    }
//
//    /**
//     * 生成多文件模式的代码并保存
//     *
//     * @param userMessage 用户消息
//     * @return 保存的目录
//     */
//    @Deprecated
//    private File generateAndSaveMultiFileCode(String userMessage) {
//        MultiFileCodeResult multiFileCodeResult = aiCodeGeneratorService.generateMultiFileCode(userMessage);
//        return CodeFileSaver.saveMultiFileCodeResult(multiFileCodeResult);
//    }
}
