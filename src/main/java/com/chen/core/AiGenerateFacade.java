package com.chen.core;

import com.chen.ai.AiCodeGeneratorService;
import com.chen.ai.model.HtmlCodeResult;
import com.chen.ai.model.MultiFileCodeResult;
import com.chen.core.parser.CodeParserExecutor;
import com.chen.core.saver.CodeFileSaverExecutor;
import com.chen.exception.BusinessException;
import com.chen.exception.ErrorCode;
import com.chen.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;

/**
 * AI 代码生成器
 */
@Service
public class AiGenerateFacade {

    private static final Logger log = LoggerFactory.getLogger(AiGenerateFacade.class);
    @Resource
    private AiCodeGeneratorService aiCodeGeneratorService;


    /**
     * 生成代码并保存到文件
     *
     * @param userMessage 用户消息
     * @param codeGenTypeEnum 生成类型
     * @return 生成的文件
     */
    public File generateWithSave(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId){
        if (codeGenTypeEnum == null){
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "生成类型不能为空");
        }

        return switch (codeGenTypeEnum){
            case HTML -> {
                HtmlCodeResult htmlCodeResult = aiCodeGeneratorService.generateHtmlCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(htmlCodeResult, codeGenTypeEnum, appId);
            }
            case MULTI_FILE -> {
                MultiFileCodeResult multiFileCodeResult = aiCodeGeneratorService.generateMultiFileCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(multiFileCodeResult, codeGenTypeEnum, appId);
            }
            default ->
            {
                // 不支持的生成类型
                String illegalType = codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, illegalType);
            }
        };
    }

    /**
     * 生成代码并保存到文件（流式）
     *
     * @param userMessage 用户消息
     * @param codeGenTypeEnum 生成类型
     * @return 生成的文件
     */
    public Flux<String> generateWithSaveStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId){
        if (codeGenTypeEnum == null){
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "生成类型不能为空");
        }

        return switch (codeGenTypeEnum){
            case HTML -> {
                Flux<String> result = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
                yield this.processCodeStream(result, codeGenTypeEnum, appId);
            }
            case MULTI_FILE -> {
                Flux<String> result = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
                yield this.processCodeStream(result, codeGenTypeEnum, appId);
            }
            default ->
            {
                // 不支持的生成类型
                String illegalType = codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, illegalType);
            }
        };
    }

    /**
     * 处理代码流，将其转换为代码内容并保存到文件
     *
     * @param result 代码流
     * @param codeGenTypeEnum 生成类型
     * @return 生成的文件
     */
    private Flux<String> processCodeStream(Flux<String> result, CodeGenTypeEnum codeGenTypeEnum, Long appId) {

        StringBuilder aiContentBuilder = new StringBuilder();

        return result.doOnNext(
                aiContentBuilder::append
        ).doOnComplete(() -> {
            try {
                String aiContent = aiContentBuilder.toString();
                Object codeContent = CodeParserExecutor.executeParser(aiContent, codeGenTypeEnum);
                File file = CodeFileSaverExecutor.executeSaver(codeContent, codeGenTypeEnum, appId);
                log.info("生成文件成功，文件目录：{}", file.getAbsolutePath());
            } catch (Exception e) {
                log.info("生成文件失败，异常信息：{}", e.getMessage());
            }
        });

    }
}
