# 我的课表 (ClassScheduleApp)

一款 Android 课表应用，支持从正方教务系统导入课表，并在上课前 10 分钟推送通知提醒。

## 功能特性

- **正方教务系统导入**：通过 WebView 登录教务系统，自动解析课表 HTML
- **周视图课表**：以 7x12 网格展示每周课程，彩色卡片区分不同课程
- **课前提醒**：上课前 10 分钟推送系统通知
- **周次切换**：左右切换查看不同周次的课表，自动计算当前周次
- **开机自启**：设备重启后自动恢复课程提醒

## 开发环境

- Android Studio Hedgehog (2023.1) 或更高版本
- Java 17
- Gradle 8.4
- Android Gradle Plugin 8.2.2
- Min SDK: 26 (Android 8.0)
- Target SDK: 34 (Android 14)

## 使用方法

1. 用 Android Studio 打开 `ClassScheduleApp` 文件夹
2. 等待 Gradle 同步完成
3. 连接 Android 设备或启动模拟器
4. 运行项目

### 首次使用

1. 进入 **设置** > 设置 **学期开始日期**
2. 点击工具栏的 **导入课表** 按钮
3. 输入你学校的正方教务系统网址
4. 在 WebView 中登录并导航到课表页面
5. 点击右下角浮动按钮导入课表

## 技术架构

- **MVVM** 架构模式
- **Room** 本地数据库
- **LiveData** 数据观察
- **WebView + Jsoup** HTML 解析
- **AlarmManager** 精确定时通知
- **Material Design 3** UI 组件

## 依赖库

| 库 | 版本 | 用途 |
|---|---|---|
| Room | 2.6.1 | 本地数据库 |
| Jsoup | 1.17.2 | HTML 解析 |
| Material Components | 1.11.0 | UI 组件 |
| Lifecycle | 2.7.0 | ViewModel / LiveData |
| Preference | 1.2.1 | 设置页面 |
