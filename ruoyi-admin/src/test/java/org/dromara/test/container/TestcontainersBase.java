package org.dromara.test.container;

import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;

/**
 * 集成测试容器基类:容器单例 + 动态接线 + schema 初始化。所有集成测试 {@code extends} 它。
 *
 * <p>关键设计(改动需谨慎,详见 docs/测试规范.md §4.2 / §7):</p>
 * <ul>
 *   <li><b>单例启动、不 stop()</b>:{@code static{}} 块只启动一次,交 Ryuk 回收;配合
 *       {@code withReuse(true)} + 本机 {@code ~/.testcontainers.properties} 的
 *       {@code testcontainers.reuse.enable=true} 跨轮复用,大幅提速。</li>
 *   <li><b>用 @DynamicPropertySource 而非 @ServiceConnection</b>:后者只能绑标准前缀
 *       {@code spring.datasource.*},命中不了 dynamic-datasource 的自定义前缀
 *       {@code spring.datasource.dynamic.datasource.master.*}。</li>
 *   <li><b>schema 直接读仓库内 {@code script/sql/ry_vue.sql}</b>(不拷贝、不 fork,避免与框架升级漂移),
 *       对 {@code ../script/sql} 与 {@code script/sql} 双重兜底(§7 #5)。</li>
 *   <li><b>MySQL 8.4</b>(DDL 按 MySQL 8 编写);<b>Redis 必须设密码</b>(§7 #2)。</li>
 *   <li>TC 2.x 的 {@code MySQLContainer} <b>非泛型</b>(无 {@code <>},§7 #1)。</li>
 * </ul>
 *
 * @author jarvey
 */
public abstract class TestcontainersBase {

    /**
     * Redis 密码:容器 {@code --requirepass} 与 {@code spring.data.redis.password} 必须一致,
     * 空密码也会触发 AUTH 报错(§7 #2)。
     */
    private static final String REDIS_PASSWORD = "ruoyi123";

    /**
     * MySQL 8.4 单例容器。TC 2.x {@code MySQLContainer} 非泛型,构造不带 {@code <>}。
     */
    private static final MySQLContainer MYSQL;

    /**
     * Redis 单例容器。用 TC core 的 {@code GenericContainer} 跑官方镜像(对齐生产 docker-compose),
     * 不额外引入 com.redis:testcontainers-redis(省依赖、离线可用)。
     */
    private static final GenericContainer<?> REDIS;

    static {
        MYSQL = new MySQLContainer(DockerImageName.parse("mysql:8.4"));
        MYSQL.withDatabaseName("ry-vue");
        MYSQL.withReuse(true);

        REDIS = new GenericContainer<>(DockerImageName.parse("redis:8.6.3-alpine"));
        REDIS.withExposedPorts(6379);
        REDIS.withCommand("redis-server", "--requirepass", REDIS_PASSWORD);
        REDIS.withReuse(true);

        // 单例启动、不 stop():交给 Ryuk 回收,配合 withReuse 跨轮复用
        MYSQL.start();
        REDIS.start();
        initSchema();
    }

    /**
     * 动态注入连接坐标。连接坐标不写死在 yml,全部由容器实际值动态注入。
     *
     * @param registry 动态属性注册表
     */
    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.dynamic.datasource.master.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.dynamic.datasource.master.username", MYSQL::getUsername);
        registry.add("spring.datasource.dynamic.datasource.master.password", MYSQL::getPassword);
        registry.add("spring.datasource.dynamic.datasource.master.driver-class-name", MYSQL::getDriverClassName);
        // p6spy 会包装容器 URL 导致连接异常,测试关闭(§7 #6)
        registry.add("spring.datasource.dynamic.p6spy", () -> false);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", REDIS::getFirstMappedPort);
        registry.add("spring.data.redis.password", () -> REDIS_PASSWORD);
    }

    /**
     * 在容器内执行 {@code script/sql/ry_vue.sql} 建库建表(MySQL 8 为准)。
     */
    private static void initSchema() {
        FileSystemResource script = resolveSchemaScript();
        try (Connection conn = DriverManager.getConnection(
                MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())) {
            ScriptUtils.executeSqlScript(conn, script);
        } catch (Exception e) {
            throw new IllegalStateException("执行 ry_vue.sql 失败: " + script.getPath(), e);
        }
    }

    /**
     * 解析 ry_vue.sql 路径:surefire/IDEA cwd=模块目录 → {@code ../script/sql};
     * 仓库根 cwd → {@code script/sql}。两者都找不到时打印绝对路径帮助定位(§7 #5)。
     *
     * @return ry_vue.sql 的文件资源
     */
    private static FileSystemResource resolveSchemaScript() {
        for (String path : new String[]{"../script/sql/ry_vue.sql", "script/sql/ry_vue.sql"}) {
            FileSystemResource resource = new FileSystemResource(path);
            if (resource.exists()) {
                return resource;
            }
        }
        throw new IllegalStateException("找不到 ry_vue.sql,已尝试: "
            + new File("../script/sql/ry_vue.sql").getAbsolutePath() + " 与 "
            + new File("script/sql/ry_vue.sql").getAbsolutePath());
    }

}
