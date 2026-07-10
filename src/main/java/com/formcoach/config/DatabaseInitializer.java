package com.formcoach.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Auto-initializes DB tables and seed data. Idempotent — safe to run on every startup.
 * Recreates movement seed data from db/init.sql to keep standardAngles current.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseInitializer implements CommandLineRunner {

    private final DataSource dataSource;

    @Override
    public void run(String... args) {
        try (Connection conn = dataSource.getConnection()) {
            log.info("Initializing database schema and seed data...");

            // Clean old movement data and re-seed (ensures jointChain is up to date)
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("DELETE FROM coach_movement");
                stmt.execute("ALTER TABLE coach_movement AUTO_INCREMENT = 1");
            } catch (Exception e) {
                log.debug("Movement cleanup skipped: {}", e.getMessage());
            }

            ScriptUtils.executeSqlScript(conn, new ClassPathResource("db/init.sql"));

            // Set tutorial video URLs for all movements (Bilibili search)
            java.util.Map<String, String> videos = java.util.Map.<String, String>ofEntries(
                java.util.Map.entry("深蹲", "https://search.bilibili.com/all?keyword=深蹲+标准动作+教学"),
                java.util.Map.entry("俯卧撑", "https://search.bilibili.com/all?keyword=俯卧撑+标准动作+教学"),
                java.util.Map.entry("平板支撑", "https://search.bilibili.com/all?keyword=平板支撑+标准动作+教学"),
                java.util.Map.entry("弓步蹲", "https://search.bilibili.com/all?keyword=弓步蹲+标准动作+教学"),
                java.util.Map.entry("臀桥", "https://search.bilibili.com/all?keyword=臀桥+标准动作+教学"),
                java.util.Map.entry("肩部推举", "https://search.bilibili.com/all?keyword=肩部推举+标准动作+教学"),
                java.util.Map.entry("硬拉", "https://search.bilibili.com/all?keyword=硬拉+标准动作+教学"),
                java.util.Map.entry("仰卧起坐", "https://search.bilibili.com/all?keyword=仰卧起坐+标准动作+教学"),
                java.util.Map.entry("二头弯举", "https://search.bilibili.com/all?keyword=二头弯举+标准动作+教学"),
                java.util.Map.entry("高抬腿", "https://search.bilibili.com/all?keyword=高抬腿+标准动作+教学"),
                java.util.Map.entry("靠墙静蹲", "https://search.bilibili.com/all?keyword=靠墙静蹲+标准动作+教学"),
                java.util.Map.entry("相扑深蹲", "https://search.bilibili.com/all?keyword=相扑深蹲+标准动作+教学"),
                java.util.Map.entry("保加利亚分腿蹲", "https://search.bilibili.com/all?keyword=保加利亚分腿蹲+标准动作+教学"),
                java.util.Map.entry("侧弓步", "https://search.bilibili.com/all?keyword=侧弓步+标准动作+教学"),
                java.util.Map.entry("小腿提踵", "https://search.bilibili.com/all?keyword=小腿提踵+标准动作+教学"),
                java.util.Map.entry("早安式体前屈", "https://search.bilibili.com/all?keyword=早安式体前屈+标准动作+教学"),
                java.util.Map.entry("跳跃深蹲", "https://search.bilibili.com/all?keyword=跳跃深蹲+标准动作+教学"),
                java.util.Map.entry("钻石俯卧撑", "https://search.bilibili.com/all?keyword=钻石俯卧撑+标准动作+教学"),
                java.util.Map.entry("宽距俯卧撑", "https://search.bilibili.com/all?keyword=宽距俯卧撑+标准动作+教学"),
                java.util.Map.entry("三头臂屈伸", "https://search.bilibili.com/all?keyword=三头臂屈伸+标准动作+教学"),
                java.util.Map.entry("侧平举", "https://search.bilibili.com/all?keyword=侧平举+标准动作+教学"),
                java.util.Map.entry("前平举", "https://search.bilibili.com/all?keyword=前平举+标准动作+教学"),
                java.util.Map.entry("跪姿俯卧撑", "https://search.bilibili.com/all?keyword=跪姿俯卧撑+标准动作+教学"),
                java.util.Map.entry("上斜俯卧撑", "https://search.bilibili.com/all?keyword=上斜俯卧撑+标准动作+教学"),
                java.util.Map.entry("俄罗斯转体", "https://search.bilibili.com/all?keyword=俄罗斯转体+标准动作+教学"),
                java.util.Map.entry("死虫式", "https://search.bilibili.com/all?keyword=死虫式+标准动作+教学"),
                java.util.Map.entry("鸟狗式", "https://search.bilibili.com/all?keyword=鸟狗式+标准动作+教学"),
                java.util.Map.entry("侧平板", "https://search.bilibili.com/all?keyword=侧平板+标准动作+教学"),
                java.util.Map.entry("登山者", "https://search.bilibili.com/all?keyword=登山者+标准动作+教学"),
                java.util.Map.entry("V字卷腹", "https://search.bilibili.com/all?keyword=V字卷腹+标准动作+教学"),
                java.util.Map.entry("波比跳", "https://search.bilibili.com/all?keyword=波比跳+标准动作+教学"),
                java.util.Map.entry("开合跳", "https://search.bilibili.com/all?keyword=开合跳+标准动作+教学"),
                java.util.Map.entry("单腿深蹲", "https://search.bilibili.com/all?keyword=单腿深蹲+标准动作+教学"),
                java.util.Map.entry("后撤步弓步", "https://search.bilibili.com/all?keyword=后撤步弓步+标准动作+教学"),
                java.util.Map.entry("交替弓步跳", "https://search.bilibili.com/all?keyword=交替弓步跳+标准动作+教学"),
                java.util.Map.entry("蛙跳", "https://search.bilibili.com/all?keyword=蛙跳+标准动作+教学"),
                java.util.Map.entry("蟹步走", "https://search.bilibili.com/all?keyword=蟹步走+标准动作+教学"),
                java.util.Map.entry("引体向上", "https://search.bilibili.com/all?keyword=引体向上+标准动作+教学"),
                java.util.Map.entry("俯身划船", "https://search.bilibili.com/all?keyword=俯身划船+标准动作+教学"),
                java.util.Map.entry("蜘蛛人俯卧撑", "https://search.bilibili.com/all?keyword=蜘蛛人俯卧撑+标准动作+教学"),
                java.util.Map.entry("仰卧抬腿", "https://search.bilibili.com/all?keyword=仰卧抬腿+标准动作+教学"),
                java.util.Map.entry("剪刀腿", "https://search.bilibili.com/all?keyword=剪刀腿+标准动作+教学"),
                java.util.Map.entry("交替触踝", "https://search.bilibili.com/all?keyword=交替触踝+标准动作+教学"),
                java.util.Map.entry("平板交替抬手", "https://search.bilibili.com/all?keyword=平板支撑交替抬手+标准动作+教学"),
                java.util.Map.entry("猫牛式", "https://search.bilibili.com/all?keyword=猫牛式+标准动作+教学"),
                java.util.Map.entry("下犬式", "https://search.bilibili.com/all?keyword=下犬式+标准动作+教学"),
                java.util.Map.entry("婴儿式", "https://search.bilibili.com/all?keyword=婴儿式+标准动作+教学"),
                java.util.Map.entry("肩部拉伸", "https://search.bilibili.com/all?keyword=肩部拉伸+标准动作+教学"),
                java.util.Map.entry("站立体前屈", "https://search.bilibili.com/all?keyword=站立体前屈+标准动作+教学"),
                java.util.Map.entry("上犬式", "https://search.bilibili.com/all?keyword=上犬式+标准动作+教学")
            );
            try (Statement stmt = conn.createStatement()) {
                for (var e : videos.entrySet()) {
                    stmt.executeUpdate("UPDATE coach_movement SET video_url = '" + e.getValue() + "' WHERE name = '" + e.getKey() + "'");
                }
            } catch (Exception ex) { log.debug("Video URL update skipped: {}", ex.getMessage()); }

            log.info("Database initialization completed with {} movements seeded.", countMovements(conn));
        } catch (Exception e) {
            log.warn("Database init skipped: {}", e.getMessage());
        }
    }

    private int countMovements(Connection conn) {
        try (var rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM coach_movement")) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (Exception e) {
            return -1;
        }
    }
}
