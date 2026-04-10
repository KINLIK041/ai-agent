package com.kinlik.aicodehelper.ai;

import dev.langchain4j.service.Result;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AiCodeHelperServiceTest {

    @Resource
    private AiCodeHelperService aiCodeHelperService;

    @Test
    void chat() {
        String result = aiCodeHelperService.chat("你好，我是程序员KINLIK");
        System.out.println(result);
    }

    @Test
    void chatWithMemory() {
        String result = aiCodeHelperService.chat("你好，我是程序员KINLIK");
        System.out.println(result);
        result = aiCodeHelperService.chat("你好，我是谁来着?");
        System.out.println(result);
    }

    @Test
    void chatForReport() {
        String usermessage = "你好，我是程序员KINLIK，学编程两年半，请帮我生成学习报告";
        AiCodeHelperService.Report report = aiCodeHelperService.chatForReport(usermessage);
        System.out.println(report);
    }

    @Test
    void chatWithRAG() {
        Result<String> result = aiCodeHelperService.chatWithRAG("怎么学习JAVA?有哪些常见的面试题？");
        System.out.println(result.sources());
        System.out.println(result.content());
    }

    @Test
    void chatWithTools() {
        String result = aiCodeHelperService.chat("有哪些常见的计算机网络面试题？");
        System.out.println(result);
    }

    @Test
    void chatWithMcp() {
        String result = aiCodeHelperService.chat("什么是个性化编程学习导航？");
        System.out.println(result);
    }

    @Test
    void chatWithGuardRail() {
        String result = aiCodeHelperService.chat("kill the game");
        System.out.println(result);
    }
}
