package com.chen.core;

import cn.hutool.json.JSONUtil;
import com.chen.ai.AiCodeGeneratorService;
import com.chen.ai.AiCodeGeneratorServiceFactory;
import com.chen.ai.model.HtmlCodeResult;
import com.chen.ai.model.MultiFileCodeResult;
import com.chen.ai.model.message.AiResponseMessage;
import com.chen.ai.model.message.ToolExecutedMessage;
import com.chen.ai.model.message.ToolRequestMessage;
import com.chen.constant.AppConstant;
import com.chen.core.builder.VueProjectBuilder;
import com.chen.core.parser.CodeParserExecutor;
import com.chen.core.saver.CodeFileSaverExecutor;
import com.chen.exception.BusinessException;
import com.chen.exception.ErrorCode;
import com.chen.model.enums.CodeGenTypeEnum;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;

/**
 * 编辑模式生成入口。
 * 这里故意只保留“生成并保存”相关能力，不再承载 discussion 逻辑。
 */
@Service
@Slf4j
public class AiGenerateFacade {

    @Resource
    private AiCodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;

    @Resource
    private VueProjectBuilder vueProjectBuilder;

    public File generateWithSave(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "生成类型不能为空");
        }
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
            default -> throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, codeGenTypeEnum.getValue());
        };
    }

    public Flux<String> generateWithSaveStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "生成类型不能为空");
        }

        AiCodeGeneratorService aiCodeGeneratorService =
                aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId, codeGenTypeEnum);

        return switch (codeGenTypeEnum) {
            case HTML -> processCodeStream(aiCodeGeneratorService.generateHtmlCodeStream(userMessage), codeGenTypeEnum, appId);
            case MULTI_FILE ->
                    processCodeStream(aiCodeGeneratorService.generateMultiFileCodeStream(userMessage), codeGenTypeEnum, appId);
            case VUE_PROJECT -> processTokenStream(aiCodeGeneratorService.generateVueProjectCodeStream(appId, userMessage), appId);
            default -> throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, codeGenTypeEnum.getValue());
        };
    }

    private Flux<String> processTokenStream(TokenStream tokenStream, Long appId) {
        return Flux.create(sink -> tokenStream.onPartialResponse(partialResponse -> {
                    AiResponseMessage aiResponseMessage = new AiResponseMessage(partialResponse);
                    sink.next(JSONUtil.toJsonStr(aiResponseMessage));
                })
                .onPartialToolExecutionRequest((index, toolExecutionRequest) -> {
                    ToolRequestMessage toolRequestMessage = new ToolRequestMessage(toolExecutionRequest);
                    sink.next(JSONUtil.toJsonStr(toolRequestMessage));
                })
                .onToolExecuted((ToolExecution toolExecution) -> {
                    ToolExecutedMessage toolExecutedMessage = new ToolExecutedMessage(toolExecution);
                    sink.next(JSONUtil.toJsonStr(toolExecutedMessage));
                })
                .onCompleteResponse((ChatResponse response) -> {
                    // Vue 项目要在完整响应结束后同步构建，保证预览目录可用。
                    String projectPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + "vue_project_" + appId;
                    vueProjectBuilder.buildProject(projectPath);
                    sink.complete();
                })
                .onError(sink::error)
                .start());
    }

    private Flux<String> processCodeStream(Flux<String> result, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        StringBuilder aiContentBuilder = new StringBuilder();
        return result.doOnNext(aiContentBuilder::append)
                .doOnComplete(() -> {
                    // 纯文本代码类型先聚合完整响应，再统一解析并落盘。
                    String aiContent = aiContentBuilder.toString();
                    Object codeContent = CodeParserExecutor.executeParser(aiContent, codeGenTypeEnum);
                    File file = CodeFileSaverExecutor.executeSaver(codeContent, codeGenTypeEnum, appId);
                    log.info("生成文件成功，文件目录：{}", file.getAbsolutePath());
                });
    }
}
