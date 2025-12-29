package com.example;

import com.fasterxml.jackson.databind.JsonNode;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.application.Platform;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class AIController implements Initializable {

    // FXML 注入的组件
    @FXML private TextArea chatArea;
    @FXML private TextField commandInput;
    @FXML private Button executeButton;
    @FXML private VBox controlPanel;
    @FXML private Label statusLabel;
    @FXML private Label titleLabel;

    // 颜色历史记录相关组件
    @FXML private HBox colorHistoryBox;

    // 控制面板中的组件
    @FXML private Button btn1;
    @FXML private Button btn2;
    @FXML private TextField sampleText;
    @FXML private ColorPicker colorPicker;

    // AI 客户端和工具
    private QwenClient qwenClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, Node> registeredComponents = new HashMap<>();

    // 颜色历史记录
    private final List<Color> colorHistory = new ArrayList<>();
    private static final int MAX_HISTORY_SIZE = 8; // 最多保存8个历史颜色

    // 颜色预设映射
    private final Map<String, Color> colorPresets = new HashMap<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        System.out.println("初始化 AI 控制器...");

        try {
            setupUIComponents();
            initializeQwenConnection();
            registerDefaultComponents();
            setupColorFeatures(); // 初始化颜色相关功能

            // 显示欢迎消息
            appendToChat("系统", "🤖 JavaFX AI 助手已启动");
            appendToChat("系统", "输入自然语言指令控制界面，例如：");
            appendToChat("系统", "  • '隐藏按钮1'");
            appendToChat("系统", "  • '将标题改为红色'");
            appendToChat("系统", "  • '显示所有组件'");
            appendToChat("系统", "  • '设置颜色选择器为蓝色'");

        } catch (Exception e) {
            System.err.println("控制器初始化失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 设置 UI 组件属性和事件
     */
    private void setupUIComponents() {
        // 设置聊天区域
        chatArea.setEditable(false);
        chatArea.setWrapText(true);

        // 命令输入框 - 回车执行
        commandInput.setOnAction(event -> executeNaturalLanguageCommand());
        commandInput.setPromptText("输入指令，如：隐藏按钮1、改变颜色、设置颜色选择器...");

        // 执行按钮
        executeButton.setOnAction(event -> executeNaturalLanguageCommand());
        executeButton.setText("🚀 执行");

        // 状态标签
        updateStatus("初始化中...", "orange");
    }

    /**
     * 初始化颜色相关功能
     */
    private void setupColorFeatures() {
        // 初始化预设颜色
        initializeColorPresets();

        // 设置颜色选择器事件监听器
        if (colorPicker != null) {
            colorPicker.setOnAction(event -> {
                Color selectedColor = colorPicker.getValue();
                handleColorSelection(selectedColor, "手动选择");
            });
        }
    }

    /**
     * 初始化颜色预设
     */
    private void initializeColorPresets() {
        colorPresets.put("#FF0000", Color.RED);        // 红色
        colorPresets.put("#00FF00", Color.GREEN);      // 绿色
        colorPresets.put("#0000FF", Color.BLUE);       // 蓝色
        colorPresets.put("#FFFF00", Color.YELLOW);     // 黄色
        colorPresets.put("#FFA500", Color.ORANGE);     // 橙色
        colorPresets.put("#800080", Color.PURPLE);     // 紫色
        colorPresets.put("#FFC0CB", Color.PINK);       // 粉色
        colorPresets.put("#000000", Color.BLACK);      // 黑色
        colorPresets.put("#FFFFFF", Color.WHITE);      // 白色
        colorPresets.put("#808080", Color.GRAY);       // 灰色
    }

    /**
     * 初始化 Qwen 连接
     */
    private void initializeQwenConnection() {
        // 从环境变量获取 API 密钥
        String apiKey = System.getProperty("qwen.api.key",
                System.getenv("QWEN_API_KEY"));

        if (apiKey == null || apiKey.trim().isEmpty()) {
            String errorMsg = "❌ 未找到 QWEN_API_KEY 环境变量";
            System.err.println(errorMsg);
            appendToChat("系统", errorMsg);
            appendToChat("系统", "请设置环境变量: export QWEN_API_KEY=your_key_here");
            updateStatus("需要 API 密钥", "red");
            return;
        }

        appendToChat("系统", "正在连接 Qwen AI 服务...");

        // 创建 Qwen 客户端
        qwenClient = new QwenClient(apiKey);

        // 设置响应处理器
        qwenClient.setResponseHandler(this::handleQwenResponse);

        // 异步连接
        CompletableFuture.runAsync(() -> {
            qwenClient.connect().thenAccept(success -> {
                Platform.runLater(() -> {
                    if (success) {
                        appendToChat("系统", "✅ 成功连接到 Qwen AI 助手");
                        updateStatus("已连接", "green");
                    } else {
                        appendToChat("系统", "❌ 连接 Qwen 服务失败");
                        updateStatus("连接失败", "red");
                    }
                });
            }).exceptionally(ex -> {
                Platform.runLater(() -> {
                    String errorMsg = "连接错误: " + ex.getMessage();
                    System.err.println(errorMsg);
                    appendToChat("系统", errorMsg);
                    updateStatus("连接错误", "red");
                });
                return null;
            });
        });
    }

    /**
     * 注册默认的 UI 组件
     */
    private void registerDefaultComponents() {
        // 注册所有可控制的组件
        registerComponent("btn1", btn1);
        registerComponent("btn2", btn2);
        registerComponent("sampleText", sampleText);
        registerComponent("colorPicker", colorPicker);
        registerComponent("titleLabel", titleLabel);
        registerComponent("chatArea", chatArea);
        registerComponent("controlPanel", controlPanel);
        registerComponent("statusLabel", statusLabel);
        registerComponent("commandInput", commandInput);
        registerComponent("executeButton", executeButton);

        System.out.println("已注册 " + registeredComponents.size() + " 个组件");
        appendToChat("系统", "已注册 " + registeredComponents.size() + " 个可控制组件");
    }

    /**
     * 执行自然语言指令
     */
    @FXML
    private void executeNaturalLanguageCommand() {
        String command = commandInput.getText().trim();
        if (command.isEmpty()) {
            return;
        }

        // 检查连接状态
        if (qwenClient == null || !qwenClient.isConnected()) {
            appendToChat("系统", "❌ AI 服务未连接，请检查连接状态");
            updateStatus("未连接", "red");
            return;
        }

        // 显示用户输入
        appendToChat("您", command);

        // 更新状态
        updateStatus("AI 思考中...", "orange");

        // 异步发送指令
        CompletableFuture.runAsync(() -> {
            qwenClient.sendInstruction(command);
        });

        // 清空输入框
        commandInput.clear();
    }

    /**
     * 处理 Qwen 的响应
     */
    private void handleQwenResponse(String responseJson) {
        try {
            System.out.println("处理 Qwen 响应...");

            // 使用 QwenClient 解析响应
            JsonNode parsedResponse = qwenClient.parseAIResponse(responseJson);

            if (parsedResponse == null) {
                appendToChat("AI", "❌ 无法解析响应");
                return;
            }

            // 检查是否为有效的 JSON 命令
            if (parsedResponse.has("command") && parsedResponse.has("description")) {
                executeJsonCommand(parsedResponse);
            } else if (parsedResponse.has("text")) {
                // 文本回复
                String text = parsedResponse.get("text").asText();
                appendToChat("AI", text);
                updateStatus("就绪", "green");
            } else if (parsedResponse.has("raw_response")) {
                // 原始响应
                appendToChat("AI", parsedResponse.get("raw_response").asText());
                updateStatus("就绪", "green");
            } else {
                appendToChat("AI", "收到响应: " + parsedResponse.toString());
                updateStatus("就绪", "green");
            }

        } catch (Exception e) {
            System.err.println("处理响应失败: " + e.getMessage());
            appendToChat("系统", "处理 AI 响应时出错: " + e.getMessage());
            updateStatus("处理错误", "red");
        }
    }

    /**
     * 执行 JSON 格式的命令
     */
    private void executeJsonCommand(JsonNode commandNode) {
        Platform.runLater(() -> {
            try {
                String commandType = commandNode.path("command").asText();
                String target = commandNode.path("target").asText();
                JsonNode params = commandNode.path("params");
                String description = commandNode.path("description").asText();

                // 显示 AI 的描述
                appendToChat("AI", description);

                // 根据命令类型执行操作
                boolean success = false;

                switch (commandType.toLowerCase()) {
                    case "showcomponent":
                        success = showComponent(target);
                        break;
                    case "hidecomponent":
                        success = hideComponent(target);
                        break;
                    case "changetext":
                        if (params.has("text")) {
                            success = changeText(target, params.path("text").asText());
                        }
                        break;
                    case "changecolor":
                        if (params.has("color")) {
                            success = changeColor(target, params.path("color").asText());
                        }
                        break;
                    case "setcolorpicker":  // 新增的命令类型
                        if (params.has("color")) {
                            success = setColorPickerValue(target, params.path("color").asText());
                        }
                        break;
                    case "setstyle":
                        if (params.has("style")) {
                            success = setStyle(target, params.path("style").asText());
                        }
                        break;
                    case "showcolorhistory":  // 显示颜色历史
                        success = showColorHistory();
                        break;
                    case "clearcolorhistory":  // 清空颜色历史
                        success = clearColorHistory();
                        break;
                    case "applyhistorycolor":
                        if (params.has("index") && params.has("target")) {
                            int index = params.path("index").asInt();
                            String targetComponent = params.path("target").asText();
                            success = applyHistoryColorByIndex(index, targetComponent);
                        }
                        break;
                    default:
                        appendToChat("系统", "❌ 未识别的命令类型: " + commandType);
                }

                if (success) {
                    updateStatus("命令执行成功", "green");
                } else {
                    updateStatus("执行失败", "orange");
                }

            } catch (Exception e) {
                System.err.println("执行命令失败: " + e.getMessage());
                appendToChat("系统", "❌ 执行命令失败: " + e.getMessage());
                updateStatus("执行错误", "red");
            }
        });
    }

    /**
     * UI 控制方法
     */
    private boolean showComponent(String componentId) {
        Node node = registeredComponents.get(componentId);
        if (node != null) {
            node.setVisible(true);
            node.setManaged(true);
            appendToChat("系统", "✅ 已显示: " + componentId);
            return true;
        } else {
            appendToChat("系统", "❌ 未找到组件: " + componentId);
            return false;
        }
    }

    // 添加对应的方法
    private boolean applyHistoryColorByIndex(int index, String componentId) {
        // 调整索引（用户使用1-based，内部使用0-based）
        int internalIndex = index - 1;

        if (internalIndex < 0 || internalIndex >= colorHistory.size()) {
            appendToChat("系统", String.format("❌ 历史颜色%d不存在，当前只有%d个历史颜色",
                    index, colorHistory.size()));
            return false;
        }

        Color color = colorHistory.get(internalIndex);
        Node component = registeredComponents.get(componentId);

        if (component == null) {
            appendToChat("系统", "❌ 未找到组件: " + componentId);
            return false;
        }

        // 应用颜色
        return applyColorToComponent(component, color,
                String.format("历史颜色%d", index));
    }

    /**
     * 通用方法：应用颜色到指定组件
     */
    private boolean applyColorToComponent(Node component, Color color, String sourceDesc) {
        try {
            String hexColor = colorToHex(color);
            String colorName = getColorName(color);

            if (component instanceof Region) {
                // 对于区域类组件（按钮、面板等），设置背景色
                String style = String.format("-fx-background-color: %s;", hexColor);

                // 保留原有样式（防止按钮变小）
                String originalStyle = component.getStyle();
                String cleanedStyle = originalStyle
                        .replaceAll("-fx-background-color:[^;]*;?", "")
                        .replaceAll(";;", ";")
                        .trim();

                component.setStyle(style + " " + cleanedStyle);
            }

            if (component instanceof Labeled) {
                // 对于标签类组件，设置文字颜色
                Labeled labeled = (Labeled) component;
                if (color.getBrightness() > 0.5) {
                    labeled.setStyle(labeled.getStyle() + " -fx-text-fill: black;");
                } else {
                    labeled.setStyle(labeled.getStyle() + " -fx-text-fill: white;");
                }
            }

            appendToChat("系统", String.format("✅ 已将%s应用到%s (%s)",
                    sourceDesc, getComponentName(component), colorName));

            return true;

        } catch (Exception e) {
            appendToChat("系统", "❌ 应用颜色失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 获取组件名称
     */
    private String getComponentName(Node node) {
        if (node instanceof Button && ((Button) node).getText() != null) {
            return "按钮: " + ((Button) node).getText();
        } else if (node instanceof Label && ((Label) node).getText() != null) {
            return "标签: " + ((Label) node).getText();
        } else {
            return node.getId() != null ? node.getId() : "未命名组件";
        }
    }

    private boolean hideComponent(String componentId) {
        Node node = registeredComponents.get(componentId);
        if (node != null) {
            node.setVisible(false);
            node.setManaged(false);
            appendToChat("系统", "✅ 已隐藏: " + componentId);
            return true;
        } else {
            appendToChat("系统", "❌ 未找到组件: " + componentId);
            return false;
        }
    }

    private boolean changeText(String componentId, String text) {
        Node node = registeredComponents.get(componentId);
        if (node == null) {
            appendToChat("系统", "❌ 未找到组件: " + componentId);
            return false;
        }

        try {
            if (node instanceof Label) {
                ((Label) node).setText(text);
            } else if (node instanceof Button) {
                ((Button) node).setText(text);
            } else if (node instanceof TextField) {
                ((TextField) node).setText(text);
            } else if (node instanceof TextArea) {
                ((TextArea) node).setText(text);
            } else {
                appendToChat("系统", "❌ 组件 " + componentId + " 不支持文本修改");
                return false;
            }

            appendToChat("系统", "✅ 已修改文本: " + componentId + " → " + text);
            return true;

        } catch (Exception e) {
            appendToChat("系统", "❌ 修改文本失败: " + e.getMessage());
            return false;
        }
    }

    private boolean changeColor(String componentId, String colorStr) {
        Node node = registeredComponents.get(componentId);
        if (node == null) {
            appendToChat("系统", "❌ 未找到组件: " + componentId);
            return false;
        }

        try {
            Color color = parseColorString(colorStr);

            // 转换为十六进制
            String hex = String.format("#%02X%02X%02X",
                    (int)(color.getRed() * 255),
                    (int)(color.getGreen() * 255),
                    (int)(color.getBlue() * 255));

            // 设置样式
            String style = String.format("-fx-background-color: %s; -fx-text-fill: %s;",
                    hex, color.getBrightness() > 0.5 ? "black" : "white");

            node.setStyle(node.getStyle() + style);

            appendToChat("系统", "✅ 已修改颜色: " + componentId + " → " + colorStr);

            // 添加到颜色历史
            addToColorHistory(color);

            return true;

        } catch (Exception e) {
            appendToChat("系统", "❌ 颜色格式错误: " + colorStr);
            return false;
        }
    }

    private boolean setStyle(String componentId, String style) {
        Node node = registeredComponents.get(componentId);
        if (node == null) {
            appendToChat("系统", "❌ 未找到组件: " + componentId);
            return false;
        }

        try {
            node.setStyle(style);
            appendToChat("系统", "✅ 已设置样式: " + componentId);
            return true;
        } catch (Exception e) {
            appendToChat("系统", "❌ 设置样式失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 设置颜色选择器的值
     */
    private boolean setColorPickerValue(String componentId, String colorStr) {
        Node node = registeredComponents.get(componentId);
        if (node == null) {
            appendToChat("系统", "❌ 未找到颜色选择器: " + componentId);
            return false;
        }

        if (!(node instanceof ColorPicker)) {
            appendToChat("系统", "❌ 组件 " + componentId + " 不是颜色选择器");
            return false;
        }

        ColorPicker colorPicker = (ColorPicker) node;

        try {
            Color color = parseColorString(colorStr);

            if (color == null) {
                appendToChat("系统", "❌ 无法识别的颜色: " + colorStr);
                return false;
            }

            // 设置颜色选择器的值
            colorPicker.setValue(color);

            // 处理颜色选择（这会自动添加到历史记录并显示消息）
            handleColorSelection(color, "通过AI指令设置");

            return true;

        } catch (Exception e) {
            appendToChat("系统", "❌ 设置颜色选择器失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 处理颜色选择（手动或AI设置）
     */
    private void handleColorSelection(Color color, String source) {
        if (color == null) return;

        String hexColor = colorToHex(color);
        String colorName = getColorName(color);

        // 添加到历史记录
        addToColorHistory(color);

        // 显示通知
        appendToChat("系统", source + "了颜色: " + colorName + " (" + hexColor + ")");

        // 自动应用到其他组件
        boolean autoApply = true;
        if (autoApply) {
            applyColorToSampleComponents(color);
        }
    }

    /**
     * 解析颜色字符串为 Color 对象
     */
    private Color parseColorString(String colorStr) {
        if (colorStr == null || colorStr.trim().isEmpty()) {
            return null;
        }

        String lowerColor = colorStr.trim().toLowerCase();

        // 新增：处理历史颜色索引
        if (lowerColor.startsWith("历史颜色") ||
                lowerColor.startsWith("historycolor") ||
                lowerColor.startsWith("colorhistory")) {

            return parseHistoryColorIndex(lowerColor);
        }

        try {
            // 处理十六进制颜色
            if (lowerColor.startsWith("#")) {
                return Color.web(colorStr);
            }

            // 处理 RGB/RGBA 格式
            if (lowerColor.startsWith("rgb") || lowerColor.startsWith("rgba")) {
                // 移除 "rgb(" 或 "rgba(" 和 ")"
                String rgbStr = lowerColor
                        .replace("rgba(", "")
                        .replace("rgb(", "")
                        .replace(")", "");

                String[] parts = rgbStr.split(",");
                if (parts.length >= 3) {
                    double r = Double.parseDouble(parts[0].trim()) / 255.0;
                    double g = Double.parseDouble(parts[1].trim()) / 255.0;
                    double b = Double.parseDouble(parts[2].trim()) / 255.0;

                    if (parts.length == 4) {
                        // RGBA 格式
                        double a = Double.parseDouble(parts[3].trim());
                        return new Color(r, g, b, a);
                    } else {
                        // RGB 格式
                        return new Color(r, g, b, 1.0);
                    }
                }
            }

            // 处理常见颜色名称
            Map<String, Color> colorMap = new HashMap<>();
            colorMap.put("红色", Color.RED);
            colorMap.put("蓝色", Color.BLUE);
            colorMap.put("绿色", Color.GREEN);
            colorMap.put("黄色", Color.YELLOW);
            colorMap.put("紫色", Color.PURPLE);
            colorMap.put("橙色", Color.ORANGE);
            colorMap.put("粉色", Color.PINK);
            colorMap.put("黑色", Color.BLACK);
            colorMap.put("白色", Color.WHITE);
            colorMap.put("灰色", Color.GRAY);
            colorMap.put("深蓝", Color.DARKBLUE);
            colorMap.put("浅蓝", Color.LIGHTBLUE);

            // 检查中英文颜色名称
            if (colorMap.containsKey(lowerColor)) {
                return colorMap.get(lowerColor);
            }

            // 英文颜色名称
            Map<String, Color> englishColorMap = new HashMap<>();
            englishColorMap.put("red", Color.RED);
            englishColorMap.put("blue", Color.BLUE);
            englishColorMap.put("green", Color.GREEN);
            englishColorMap.put("yellow", Color.YELLOW);
            englishColorMap.put("purple", Color.PURPLE);
            englishColorMap.put("orange", Color.ORANGE);
            englishColorMap.put("pink", Color.PINK);
            englishColorMap.put("black", Color.BLACK);
            englishColorMap.put("white", Color.WHITE);
            englishColorMap.put("gray", Color.GRAY);
            englishColorMap.put("darkblue", Color.DARKBLUE);
            englishColorMap.put("lightblue", Color.LIGHTBLUE);

            if (englishColorMap.containsKey(lowerColor)) {
                return englishColorMap.get(lowerColor);
            }

            // 最后尝试使用 Color.web（支持更多颜色名称）
            return Color.web(colorStr);

        } catch (Exception e) {
            System.err.println("解析颜色失败: " + colorStr + " - " + e.getMessage());
            return null;
        }
    }

    /**
     * 解析历史颜色索引
     */
    private Color parseHistoryColorIndex(String colorIndexStr) {
        try {
            // 提取数字部分
            String numberStr = colorIndexStr
                    .replace("历史颜色", "")
                    .replace("historycolor", "")
                    .replace("colorhistory", "")
                    .replace("历史", "")
                    .replace("颜色", "")
                    .trim();

            // 中文数字转换
            Map<String, Integer> chineseNumbers = new HashMap<>();
            chineseNumbers.put("一", 1);
            chineseNumbers.put("二", 2);
            chineseNumbers.put("三", 3);
            chineseNumbers.put("四", 4);
            chineseNumbers.put("五", 5);
            chineseNumbers.put("六", 6);
            chineseNumbers.put("七", 7);
            chineseNumbers.put("八", 8);

            int index;
            if (chineseNumbers.containsKey(numberStr)) {
                index = chineseNumbers.get(numberStr);
            } else {
                index = Integer.parseInt(numberStr);
            }

            // 索引转换为0-based，且不超过历史记录大小
            index = Math.max(1, Math.min(index, colorHistory.size())) - 1;

            if (index >= 0 && index < colorHistory.size()) {
                return colorHistory.get(index);
            } else {
                return null;
            }

        } catch (Exception e) {
            System.err.println("解析历史颜色索引失败: " + colorIndexStr);
            return null;
        }
    }


    /**
     * 将 Color 转换为十六进制字符串
     */
    private String colorToHex(Color color) {
        return String.format("#%02X%02X%02X",
                (int)(color.getRed() * 255),
                (int)(color.getGreen() * 255),
                (int)(color.getBlue() * 255));
    }

    /**
     * 添加到颜色历史记录
     */
    private void addToColorHistory(Color color) {
        // 移除重复的颜色
        colorHistory.removeIf(c -> colorToHex(c).equals(colorToHex(color)));

        // 添加到开头
        colorHistory.add(0, color);

        // 保持最大数量
        if (colorHistory.size() > MAX_HISTORY_SIZE) {
            colorHistory.remove(colorHistory.size() - 1);
        }

        // 更新历史记录显示
        updateColorHistoryDisplay();
    }

    /**
     * 更新颜色历史记录显示
     */
    private void updateColorHistoryDisplay() {
        Platform.runLater(() -> {
            colorHistoryBox.getChildren().clear();

            for (Color color : colorHistory) {
                Rectangle colorRect = createColorRectangle(color);
                colorHistoryBox.getChildren().add(colorRect);
            }
        });
    }

    /**
     * 创建颜色矩形显示
     */
    private Rectangle createColorRectangle(Color color) {
        Rectangle rect = new Rectangle(25, 25, color);
        rect.setStroke(Color.LIGHTGRAY);
        rect.setStrokeWidth(1);
        rect.setArcWidth(5);
        rect.setArcHeight(5);

        // 点击颜色矩形可以重新选择该颜色
        rect.setOnMouseClicked(event -> {
            colorPicker.setValue(color);
            handleColorSelection(color, "从历史记录选择");
        });

        // 添加悬停效果
        rect.setOnMouseEntered(event -> {
            rect.setStroke(Color.BLACK);
            rect.setStrokeWidth(2);
        });

        rect.setOnMouseExited(event -> {
            rect.setStroke(Color.LIGHTGRAY);
            rect.setStrokeWidth(1);
        });

        // 添加工具提示
        Tooltip tooltip = new Tooltip(
                "颜色: " + getColorName(color) + "\n" +
                        "十六进制: " + colorToHex(color) + "\n" +
                        "点击应用此颜色"
        );
        Tooltip.install(rect, tooltip);

        return rect;
    }

    /**
     * 应用颜色到示例组件（联动效果）
     */
    private void applyColorToSampleComponents(Color color) {
        String hexColor = colorToHex(color);
        String textColor = color.getBrightness() > 0.5 ? "black" : "white";

        // 应用到按钮1 - 使用追加模式
        if (btn1 != null) {
            // 获取原有样式
            String originalStyle = btn1.getStyle();

            // 移除可能存在的背景颜色和文字颜色设置
            String cleanedStyle = removeColorProperties(originalStyle);

            // 添加新的颜色设置
            String newColorStyle = String.format(
                    "-fx-background-color: %s; -fx-text-fill: %s;",
                    hexColor, textColor
            );

            // 合并样式（新颜色 + 清理后的原有样式）
            btn1.setStyle(newColorStyle + cleanedStyle);

            // 确保最小尺寸
            btn1.setMinSize(Button.USE_PREF_SIZE, Button.USE_PREF_SIZE);
        }

        // 应用到标题标签
        if (titleLabel != null) {
            String labelStyle = String.format(
                    "-fx-text-fill: %s;",
                    hexColor
            );
            titleLabel.setStyle(titleLabel.getStyle() + labelStyle);
        }
    }

    /**
     * 从样式字符串中移除颜色相关属性
     */
    private String removeColorProperties(String style) {
        if (style == null || style.isEmpty()) {
            return "";
        }

        // 移除背景颜色、文字颜色、边框颜色相关属性
        String[] lines = style.split(";");
        StringBuilder cleaned = new StringBuilder();

        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() &&
                    !trimmed.startsWith("-fx-background-color") &&
                    !trimmed.startsWith("-fx-text-fill") &&
                    !trimmed.startsWith("-fx-border-color")) {
                cleaned.append(trimmed).append("; ");
            }
        }

        return cleaned.toString();
    }

    /**
     * 应用颜色预设
     */
    @FXML
    private void applyColorPreset() {
        // 获取事件源
        Object eventSource = colorPicker.getScene().getFocusOwner();
        if (eventSource instanceof Button) {
            Button sourceButton = (Button) eventSource;
            if (sourceButton.getUserData() != null) {
                String colorHex = sourceButton.getUserData().toString();
                Color color = Color.web(colorHex);

                // 设置颜色选择器的值
                colorPicker.setValue(color);

                // 处理颜色选择
                handleColorSelection(color, "通过预设按钮");
            }
        }
    }

    /**
     * 获取颜色名称
     */
    private String getColorName(Color color) {
        String hex = colorToHex(color);

        // 检查预设颜色
        for (Map.Entry<String, Color> entry : colorPresets.entrySet()) {
            if (colorToHex(entry.getValue()).equals(hex)) {
                return getChineseColorName(entry.getKey());
            }
        }

        // 返回十六进制值
        return hex;
    }

    /**
     * 获取中文颜色名称
     */
    private String getChineseColorName(String hex) {
        Map<String, String> colorNames = new HashMap<>();
        colorNames.put("#FF0000", "红色");
        colorNames.put("#00FF00", "绿色");
        colorNames.put("#0000FF", "蓝色");
        colorNames.put("#FFFF00", "黄色");
        colorNames.put("#FFA500", "橙色");
        colorNames.put("#800080", "紫色");
        colorNames.put("#FFC0CB", "粉色");
        colorNames.put("#000000", "黑色");
        colorNames.put("#FFFFFF", "白色");
        colorNames.put("#808080", "灰色");

        return colorNames.getOrDefault(hex, hex);
    }

    /**
     * 显示颜色历史记录
     */
    private boolean showColorHistory() {
        if (colorHistory.isEmpty()) {
            appendToChat("系统", "📭 颜色历史记录为空");
            return true;
        }

        appendToChat("系统", "🎨 最近使用的颜色：");
        for (int i = 0; i < colorHistory.size(); i++) {
            Color color = colorHistory.get(i);
            String hex = colorToHex(color);
            String name = getColorName(color);
            appendToChat("系统", String.format("  %d. %s (%s)", i + 1, name, hex));
        }
        return true;
    }

    /**
     * 清空颜色历史记录
     */
    private boolean clearColorHistory() {
        colorHistory.clear();
        updateColorHistoryDisplay();
        appendToChat("系统", "✅ 已清空颜色历史记录");
        return true;
    }

    /**
     * 注册新组件
     */
    public void registerComponent(String id, Node node) {
        registeredComponents.put(id, node);
        System.out.println("注册组件: " + id);
    }

    /**
     * 注册新组件按钮事件（FXML 调用）
     */
    @FXML
    private void registerNewComponent() {
        appendToChat("系统", "📝 注册新组件功能开发中...");
        appendToChat("系统", "当前已注册 " + registeredComponents.size() + " 个组件");
    }

    /**
     * 更新状态标签
     */
    private void updateStatus(String text, String color) {
        Platform.runLater(() -> {
            statusLabel.setText(text);
            switch (color.toLowerCase()) {
                case "green":
                    statusLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");
                    break;
                case "red":
                    statusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    break;
                case "orange":
                    statusLabel.setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                    break;
                default:
                    statusLabel.setStyle("-fx-text-fill: #34495e; -fx-font-weight: bold;");
            }
        });
    }

    /**
     * 添加消息到聊天区域
     */
    private void appendToChat(String sender, String message) {
        Platform.runLater(() -> {
            String formattedMessage;

            if ("系统".equals(sender)) {
                formattedMessage = String.format("[系统] %s\n", message);
            } else if ("AI".equals(sender)) {
                formattedMessage = String.format("🤖 AI: %s\n", message);
            } else if ("您".equals(sender)) {
                formattedMessage = String.format("👤 您: %s\n", message);
            } else {
                formattedMessage = String.format("[%s] %s\n", sender, message);
            }

            chatArea.appendText(formattedMessage);

            // 滚动到底部
            chatArea.setScrollTop(Double.MAX_VALUE);
        });
    }

//    /**
//     * 获取颜色历史记录
//     */
//    public List<String> getColorHistory() {
//        List<String> history = new ArrayList<>();
//        for (Color color : colorHistory) {
//            history.add(colorToHex(color));
//        }
//        return history;
//    }
//
//    /**
//     * 清空颜色历史记录
//     */
//    public void clearColorHistoryPublic() {
//        clearColorHistory();
//    }
//
//    /**
//     * 应用历史颜色到指定组件
//     */
//    public void applyHistoryColor(int index, String componentId) {
//        if (index >= 0 && index < colorHistory.size()) {
//            Color color = colorHistory.get(index);
//            Node component = registeredComponents.get(componentId);
//
//            if (component != null) {
//                String hexColor = colorToHex(color);
//                String style = String.format("-fx-background-color: %s;", hexColor);
//                component.setStyle(style);
//
//                appendToChat("系统",
//                        "已应用历史颜色" + (index + 1) + "到" + componentId);
//            }
//        }
//    }

    /**
     * 清理资源
     */
    public void cleanup() {
        System.out.println("清理 AI 控制器资源...");
        if (qwenClient != null) {
            qwenClient.close();
            appendToChat("系统", "已断开 AI 连接");
        }
    }
}