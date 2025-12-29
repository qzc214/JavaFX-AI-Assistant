package com.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class QwenClient {
    // DashScope HTTP 端点 - 使用正确的格式
    private static final String API_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";

    private final String apiKey;
    private final HttpClient httpClient;
    private Consumer<String> responseHandler;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private boolean isInitialized = false;

    public QwenClient(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    /**
     * 初始化 Qwen 客户端
     */
    public CompletableFuture<Boolean> connect() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        CompletableFuture.runAsync(() -> {
            try {
                System.out.println("✅ Qwen HTTP 客户端初始化成功");
                isInitialized = true;
                future.complete(true);
            } catch (Exception e) {
                System.err.println("❌ Qwen 客户端初始化失败: " + e.getMessage());
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    /**
     * 设置响应处理器
     */
    public void setResponseHandler(Consumer<String> handler) {
        this.responseHandler = handler;
    }

    /**
     * 发送自然语言指令到 Qwen
     */
    public void sendInstruction(String naturalLanguageCommand) {
        if (!isInitialized) {
            System.err.println("❌ Qwen 客户端未初始化");
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                // 构建正确的请求 JSON
                JSONObject request = new JSONObject();
                request.put("model", "qwen-max");  // 使用 qwen-max 模型

                // 构建消息数组 - 使用正确的格式
                JSONArray messages = new JSONArray();

                // 系统消息
                JSONObject systemMessage = new JSONObject();
                systemMessage.put("role", "system");
                systemMessage.put("content", buildSystemPrompt());
                messages.put(systemMessage);

                // 用户消息
                JSONObject userMessage = new JSONObject();
                userMessage.put("role", "user");
                userMessage.put("content", naturalLanguageCommand);
                messages.put(userMessage);

                request.put("messages", messages);
                request.put("stream", false);  // 非流式响应

                String requestJson = request.toString();
                System.out.println("📤 发送请求到 Qwen...");
                System.out.println("请求内容: " + requestJson);

                // 发送 HTTP 请求
                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(API_URL))
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                        .timeout(Duration.ofSeconds(60))
                        .build();

                httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
                        .thenAccept(response -> {
                            System.out.println("📥 收到 Qwen 响应，状态码: " + response.statusCode());

                            if (response.statusCode() == 200) {
                                String responseBody = response.body();
                                System.out.println("完整响应: " + responseBody);

                                if (responseHandler != null) {
                                    responseHandler.accept(responseBody);
                                }
                            } else {
                                System.err.println("❌ HTTP 请求失败: " + response.statusCode() + " - " + response.body());
                                if (responseHandler != null) {
                                    JSONObject error = new JSONObject();
                                    error.put("error", "HTTP " + response.statusCode());
                                    error.put("message", response.body());
                                    responseHandler.accept(error.toString());
                                }
                            }
                        })
                        .exceptionally(ex -> {
                            System.err.println("❌ 发送指令失败: " + ex.getMessage());
                            if (responseHandler != null) {
                                JSONObject error = new JSONObject();
                                error.put("error", "请求异常");
                                error.put("message", ex.getMessage());
                                responseHandler.accept(error.toString());
                            }
                            return null;
                        });

            } catch (Exception e) {
                System.err.println("❌ 构建请求失败: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * 构建系统提示词
     */
    private String buildSystemPrompt() {
        return """
        你是一个JavaFX应用程序的AI控制助手。请将用户的自然语言指令解析为可执行的JSON命令。
        
        可用命令类型：
        1. showComponent - 显示UI组件
        2. hideComponent - 隐藏UI组件
        3. changeText - 修改文本内容
        4. changeColor - 修改颜色
        5. setColorPicker - 设置颜色选择器的值（新增）
        6. setStyle - 设置CSS样式
        7. executeAction - 执行特定动作
        8. showColorHistory - 显示颜色历史记录
        9. clearColorHistory - 清空颜色历史记录
        8. applyHistoryColor - 应用历史颜色到指定组件
        
        可用组件ID（target字段）：
        - btn1, btn2: 按钮
        - sampleText: 文本框
        - titleLabel: 标题标签
        - chatArea: 聊天区域
        - controlPanel: 控制面板
        - statusLabel: 状态标签
        - colorPicker: 颜色选择器（新增说明）
        
        颜色相关功能：
        1. 颜色历史记录：系统会记录最近使用的颜色
        2. 颜色预设：可以使用预设颜色按钮
        3. 颜色联动：设置颜色选择器会自动应用到其他组件
        
        新增颜色相关指令：
        - "显示颜色历史记录"
        - "清空颜色历史"
        
        历史颜色使用说明：
        1. 使用前请确保颜色历史记录不为空
        2. 索引从1开始：历史颜色1是最新颜色
        3. 支持中文："历史颜色一"、"历史颜色1"
        
        历史颜色应用JSON格式：
        {
           "command": "applyHistoryColor",
           "target": "组件ID",
           "params": {
              "index": 1,
              "target": "btn1"
           },
           "description": "将历史颜色1应用到按钮1"
        }
        
        如果用户要求应用历史颜色但未指定索引，可以询问或使用最新颜色（索引1）。
        
        示例对话：
        用户："将历史颜色1应用到按钮1"
        返回：{
           "command": "applyHistoryColor",
           "target": "btn1",
           "params": {
               "index": 1,
               "target": "btn1"
           },
           "description": "已将最新使用的颜色应用到按钮1"
        }
        
        可用颜色格式：
        - 颜色名称：红色、蓝色、绿色、黄色、橙色、紫色、粉色、黑色、白色、灰色
        - 十六进制：#FF0000、#00FF00、#0000FF
        - RGB格式：rgb(255,0,0)、rgba(255,0,0,1.0)
        
        颜色命令参数格式：
        对于 changeColor 命令：params.color 可以是颜色名称（red, blue, green）或十六进制值（#FF0000）
        对于 setColorPicker 命令：params.color 可以是颜色名称或十六进制值
        
        请严格按照以下JSON格式返回，只返回JSON，不要有其他文字：
        {
            "command": "命令名称",
            "target": "组件ID",
            "params": {
                // 根据命令不同有不同参数
            },
            "description": "对操作的人类可读描述"
        }
        
        如果无法解析为有效命令，请返回包含解释的JSON格式。
        
        颜色选择器示例：
        用户说："将颜色选择器设置为蓝色"
        返回：{
            "command": "setColorPicker",
            "target": "colorPicker",
            "params": {
                "color": "blue"
            },
            "description": "已将颜色选择器设置为蓝色"
        }
        """;
    }

    /**
     * 关闭客户端
     */
    public void close() {
        System.out.println("已关闭 Qwen HTTP 客户端");
    }

    /**
     * 检查连接状态
     */
    public boolean isConnected() {
        return isInitialized;
    }

    /**
     * 解析 AI 响应为 JSON 命令
     */
    public JsonNode parseAIResponse(String response) {
        try {
            System.out.println("开始解析响应: " + response.substring(0, Math.min(300, response.length())) + "...");

            JsonNode rootNode = objectMapper.readTree(response);

            // 解析 OpenAI 兼容格式
            if (rootNode.has("choices")) {
                JsonNode choices = rootNode.get("choices");
                if (choices.isArray() && choices.size() > 0) {
                    JsonNode choice = choices.get(0);
                    if (choice.has("message")) {
                        JsonNode message = choice.get("message");
                        if (message.has("content")) {
                            String content = message.get("content").asText().trim();
                            System.out.println("AI 回复内容: " + content);

                            // 尝试解析内容中的 JSON
                            try {
                                // 清理可能的多余字符
                                String cleanedContent = content.replace("```json", "")
                                        .replace("```", "")
                                        .trim();

                                // 如果内容以 { 开头，尝试解析为 JSON
                                if (cleanedContent.startsWith("{")) {
                                    return objectMapper.readTree(cleanedContent);
                                } else {
                                    // 如果不是 JSON，创建文本响应
                                    JSONObject wrapper = new JSONObject();
                                    wrapper.put("text", content);
                                    wrapper.put("is_json", false);
                                    return objectMapper.readTree(wrapper.toString());
                                }
                            } catch (Exception e) {
                                System.out.println("内容解析失败，返回文本响应: " + e.getMessage());
                                // 创建文本响应
                                JSONObject wrapper = new JSONObject();
                                wrapper.put("text", content);
                                wrapper.put("is_json", false);
                                wrapper.put("parse_error", e.getMessage());
                                return objectMapper.readTree(wrapper.toString());
                            }
                        }
                    }
                }
            }

            // 检查是否有错误
            if (rootNode.has("error")) {
                System.err.println("API 返回错误: " + rootNode.toString());
                JSONObject errorWrapper = new JSONObject();
                if (rootNode.get("error").isObject()) {
                    JsonNode errorNode = rootNode.get("error");
                    if (errorNode.has("message")) {
                        errorWrapper.put("message", errorNode.get("message").asText());
                    }
                }
                errorWrapper.put("error", "API Error");
                return objectMapper.readTree(errorWrapper.toString());
            }

            // 如果没有标准格式，检查是否为直接错误响应
            if (response.contains("\"error\"")) {
                return objectMapper.readTree(response);
            }

            // 返回原始响应包装
            JSONObject wrapper = new JSONObject();
            wrapper.put("raw_response", response);
            wrapper.put("is_json", false);
            return objectMapper.readTree(wrapper.toString());

        } catch (Exception e) {
            System.err.println("❌ 解析 AI 响应失败: " + e.getMessage());
            e.printStackTrace();

            // 返回错误响应
            try {
                JSONObject error = new JSONObject();
                error.put("error", "解析失败");
                error.put("message", e.getMessage());
                error.put("raw_response", response.substring(0, Math.min(500, response.length())));
                return objectMapper.readTree(error.toString());
            } catch (Exception ex) {
                return null;
            }
        }
    }

    /**
     * 测试方法：发送一个简单的请求验证连接
     */
    public CompletableFuture<Boolean> testConnection() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        CompletableFuture.runAsync(() -> {
            try {
                JSONObject request = new JSONObject();
                request.put("model", "qwen-max");

                JSONArray messages = new JSONArray();

                JSONObject systemMessage = new JSONObject();
                systemMessage.put("role", "system");
                systemMessage.put("content", "你是一个测试助手，请回复 '连接成功'");
                messages.put(systemMessage);

                JSONObject userMessage = new JSONObject();
                userMessage.put("role", "user");
                userMessage.put("content", "测试连接");
                messages.put(userMessage);

                request.put("messages", messages);
                request.put("stream", false);

                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(API_URL))
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(request.toString()))
                        .timeout(Duration.ofSeconds(10))
                        .build();

                httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

                System.out.println("✅ API 连接测试成功");
                future.complete(true);

            } catch (Exception e) {
                System.err.println("❌ API 连接测试失败: " + e.getMessage());
                future.complete(false);
            }
        });

        return future;
    }
}