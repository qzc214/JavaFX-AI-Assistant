package com.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import java.util.Objects;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) {
        try {
            // 1. 加载 FXML 界面
            FXMLLoader loader = new FXMLLoader(
                    Objects.requireNonNull(getClass().getResource("/com/example/view/AIAssistantView.fxml"))
            );
            Parent root = loader.load();
            // 2. 获取控制器实例（用于后续清理）
            AIController controller = loader.getController();
            // 3. 创建场景
            Scene scene = new Scene(root, 1000, 750);
            // 4. 可选：添加 CSS 样式
            String cssPath = Objects.requireNonNull(
                    getClass().getResource("/com/example/css/style.css")
            ).toExternalForm();
            scene.getStylesheets().add(cssPath);
            // 5. 配置主窗口
            primaryStage.setTitle("🤖 JavaFX AI 智能控制台 - 基于 Qwen 大模型");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(900);
            primaryStage.setMinHeight(700);
            // 6. 设置应用图标
            try {
                Image icon = new Image(Objects.requireNonNull(
                        getClass().getResourceAsStream("/com/example/images/icon.png")
                ));
                primaryStage.getIcons().add(icon);
            } catch (Exception e) {
                System.out.println("图标加载失败，使用默认图标");
            }
            // 7. 显示窗口
            primaryStage.show();
            // 8. 窗口关闭时的清理操作
            primaryStage.setOnCloseRequest(event -> {
                System.out.println("应用程序正在关闭...");
                if (controller != null) {
                    controller.cleanup();
                }
                System.exit(0);
            });
        } catch (Exception e) {
            System.err.println("应用程序启动失败:");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        // 启动 JavaFX 应用
        System.out.println("启动 JavaFX AI 助手...");
        if (System.getenv("QWEN_API_KEY") == null) {
            System.setProperty("qwen.api.key", "your_key");//输入你自己的apikey
            System.out.println("⚠️ 使用应急系统属性设置");
        }
        launch(args);
    }
}