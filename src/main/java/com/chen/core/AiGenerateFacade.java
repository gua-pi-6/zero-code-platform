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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;

/**
 * AI 代码生成器
 */
@Service
@Slf4j
public class AiGenerateFacade {


    @Resource
    private AiCodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;
    @Resource
    private VueProjectBuilder vueProjectBuilder;



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

        // 根据appId获取对应的AI代码生成器服务
        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId);

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
     * 生成交流内容, 不修改应用 (仅交流类型下)
     *
     * @param userMessage 用户消息
     * @param codeGenTypeEnum 生成类型
     * @return 生成的文件
     */
    public Flux<String> generateDiscussion(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId){
        if (codeGenTypeEnum == null){
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "生成类型不能为空");
        }

        // 根据appId获取对应的AI代码生成器服务
        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiDiscussionGeneratorService(appId, codeGenTypeEnum);

        return aiCodeGeneratorService.generateDiscussionStream(userMessage);
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

        // 根据appId获取对应的AI代码生成器服务
        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId, codeGenTypeEnum);

        return switch (codeGenTypeEnum){
            case HTML -> {
                Flux<String> result = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
                yield this.processCodeStream(result, codeGenTypeEnum, appId);
            }
            case MULTI_FILE -> {
                Flux<String> result = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
                yield this.processCodeStream(result, codeGenTypeEnum, appId);
            }
            case VUE_PROJECT -> {
                TokenStream result = aiCodeGeneratorService.generateVueProjectCodeStream(appId, userMessage);
                yield this.processTokenStream(result, appId);
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
     * 将 TokenStream 转换为 Flux<String>，并传递工具调用信息
     *
     * @param tokenStream TokenStream 对象
     * @return Flux<String> 流式响应
     */
    private Flux<String> processTokenStream(TokenStream tokenStream, Long appId) {

        return Flux.create(sink -> {
            tokenStream.onPartialResponse((String partialResponse) -> {
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
                        // 执行 Vue 项目构建（同步执行，确保预览时项目已就绪）
                        String projectPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + "vue_project_" + appId;
                        vueProjectBuilder.buildProject(projectPath);
                        sink.complete();
                    })

                    .onError((Throwable error) -> {
                        error.printStackTrace();
                        sink.error(error);
                    })
                    .start();
        });
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
