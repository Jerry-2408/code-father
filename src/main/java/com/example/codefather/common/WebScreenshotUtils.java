package com.example.codefather.common;

import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.example.codefather.constant.AppConstant;
import com.example.codefather.exception.BusinessException;
import com.example.codefather.exception.ErrorCode;
import io.github.bonigarcia.wdm.WebDriverManager;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.time.Duration;
import java.util.UUID;

@Slf4j
public class WebScreenshotUtils {

    private static final ThreadLocal<WebDriver> driverThreadLocal = new ThreadLocal<>();

    private static final int DEFAULT_WIDTH = 1600;
    private static final int DEFAULT_HEIGHT = 900;

    /**
     * 获取webDriver
     * @return webDriver
     */
    public static WebDriver getWebDriver() {
        WebDriver driver = driverThreadLocal.get();
        if (driver == null) {
            driver = initChromeDriver(DEFAULT_WIDTH, DEFAULT_HEIGHT);
            driverThreadLocal.set(driver);
        }
        return driver;
    }

//    private static final WebDriver webDriver;

    // 线程A: driver.get("page1.html") -> 截图
    // 线程B: driver.get("page2.html") -> 截图
    // 结果：线程 A 可能截到 page2 的内容
//    static {
//        final int DEFAULT_WIDTH = 1600;
//        final int DEFAULT_HEIGHT = 900;
//        webDriver = initChromeDriver(DEFAULT_WIDTH, DEFAULT_HEIGHT);
//    }

//    @PreDestroy
//    public void destroy() {
//        webDriver.quit();
//    }

    /**
     * 生成网页截图
     *
     * @param webUrl 网页地址
     * @return 图片路径
     */
    public static String saveWebPageScreenshot(String webUrl) {
        if (StrUtil.isBlank(webUrl)) {
            log.error("webUrl不能为空");
            return null;
        }
        // 获取webDriver
        WebDriver webDriver = getWebDriver();
        try {
            // 创建临时目录
            String rootPath = AppConstant.COVER_IMAGE_ROOT_DIR + File.separator + UUID.randomUUID().toString().substring(0, 8);
            FileUtil.mkdir(rootPath);
            // 访问网页
            webDriver.get(webUrl);
            // 等待页面加载完成
            waitForPageLoad(webDriver);
            // 截图
            byte[] screenshotBytes = ((TakesScreenshot) webDriver).getScreenshotAs(OutputType.BYTES);
            // 原始图片路径
            final String IMAGE_SUFFIX = ".png";
            String imageSavePath = rootPath + File.separator + RandomUtil.randomNumbers(5) + IMAGE_SUFFIX;
            // 保存原始图片
            saveImage(screenshotBytes, imageSavePath);
            log.info("原始图片保存成功：{}", imageSavePath);
            // 压缩图片路径
            final String COMPRESS_IMAGE_SUFFIX = "_compressed.jpg";
            String compressedImageSavePath = rootPath + File.separator + RandomUtil.randomNumbers(5) + COMPRESS_IMAGE_SUFFIX;
            // 压缩图片
            compressImage(imageSavePath, compressedImageSavePath);
            log.info("压缩图片保存成功：{}", compressedImageSavePath);
            // 删除原始图片，保留压缩图片
            FileUtil.del(imageSavePath);
            return compressedImageSavePath;
        } catch (Exception e) {
            log.error("截图失败：{}", webUrl, e);
            return null;
        } finally {
            // 资源释放
            webDriver.quit();
            driverThreadLocal.remove();
        }
    }

    /**
     * 初始化 Chrome 浏览器驱动
     */
    private static WebDriver initChromeDriver(int width, int height) {
        try {
            // 自动管理 ChromeDriver
            WebDriverManager.chromedriver().setup();
            // 配置 Chrome 选项
            ChromeOptions options = new ChromeOptions();
            // 无头模式
            options.addArguments("--headless");
            // 禁用GPU（在某些环境下避免问题）
            options.addArguments("--disable-gpu");
            // 禁用沙盒模式（Docker环境需要）
            options.addArguments("--no-sandbox");
            // 禁用开发者shm使用
            options.addArguments("--disable-dev-shm-usage");
            // 设置窗口大小
            options.addArguments(String.format("--window-size=%d,%d", width, height));
            // 禁用扩展
            options.addArguments("--disable-extensions");
            // 设置用户代理
            options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            // 创建驱动
            WebDriver driver = new ChromeDriver(options);
            // 设置页面加载超时
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
            // 设置隐式等待
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
            return driver;
        } catch (Exception e) {
            log.error("初始化 Chrome 浏览器失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "初始化 Chrome 浏览器失败");
        }
    }

    /**
     * 保存图片
     */
    private static void saveImage(byte[] imageBytes, String imagePath) {
        try {
            FileUtil.writeBytes(imageBytes, imagePath);
        } catch (Exception e) {
            log.error("保存图片失败：{}", imagePath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存图片失败");
        }
    }

    /**
     * 压缩图片
     */
    private static void compressImage(String originalImagePath, String compressedImagePath) {
        final float COMPRESSION_RATIO = 0.3f;
        try {
            ImgUtil.compress(FileUtil.file(originalImagePath), FileUtil.file(compressedImagePath), COMPRESSION_RATIO);
        } catch (Exception e) {
            log.error("图片压缩失败：{} -> {}", originalImagePath, compressedImagePath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片压缩失败");
        }
    }

    private static void waitForPageLoad(WebDriver driver) {
        try {
            // 创建等待页面加载对象
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            // 等待 document.readyState === 'complete'
            wait.until(webDriver -> ((JavascriptExecutor) webDriver).executeScript("return document.readyState").equals("complete"));
            // 额外等待一段时间，确保动态内容加载完成
            Thread.sleep(2000);
        } catch (Exception e) {
            log.error("等待页面加载时出现异常，继续执行截图", e);
        }
    }
}
