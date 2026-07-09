package org.dromara.test.redis;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.dromara.common.redis.manager.PlusSpringCacheManager;
import org.dromara.common.redis.utils.RedisUtils;
import org.dromara.test.container.TestcontainersBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * caffeine 一级缓存(L1)接线 集成测试。
 *
 * <p>目标:证明 {@code CacheConfig.cacheManager(Cache)} 把 caffeine 传入
 * {@link PlusSpringCacheManager} 后,本地一级缓存真正进入了读链路;而修改前的
 * {@code new PlusSpringCacheManager()}(caffeine=null)不会有 L1。</p>
 *
 * <p><b>可观测信号</b>:{@link org.dromara.common.redis.manager.CaffeineCacheDecorator#get}
 * 会把值缓在本地,30s 内二次读不回源。因此"绕过 Spring Cache 抽象直接改 Redis"后:
 * L1 生效读到旧值(stale),L1 未接读到新值。"绕过抽象"用一个无 caffeine 的
 * manager 指向同一 map 完成——它走 {@code RedissonCache.put} 直落 Redis,
 * 序列化格式与读一致,且不触碰另一 manager 的 caffeine。</p>
 *
 * @author jarvey
 */
@Tag("dev")
@ActiveProfiles({"dev", "test"})
@SpringBootTest
class CacheL1IntegrationTest extends TestcontainersBase {

    /**
     * 应用真实装配的缓存管理器(即 CacheConfig 修改后的 bean,带 caffeine)。
     */
    @Autowired
    private CacheManager applicationCacheManager;

    /**
     * 无 caffeine 的旁路 manager:等价于"修改前"的 {@code new PlusSpringCacheManager()},
     * 兼作"绕过抽象直接改 Redis"的写入器(走 RedissonCache.put 直落 Redis)。
     */
    private PlusSpringCacheManager mutator;

    @BeforeEach
    void setUp() {
        mutator = noCaffeineManager();
    }

    /**
     * 构造与 CacheConfig 同参的 caffeine 实例。
     */
    private Cache<Object, Object> newCaffeine() {
        return Caffeine.newBuilder()
            .expireAfterWrite(30, TimeUnit.SECONDS)
            .initialCapacity(100)
            .maximumSize(1000)
            .build();
    }

    private PlusSpringCacheManager caffeineManager() {
        PlusSpringCacheManager m = new PlusSpringCacheManager(newCaffeine());
        // 去掉事务装饰,消除无关变量,写入即时可见
        m.setTransactionAware(false);
        return m;
    }

    private PlusSpringCacheManager noCaffeineManager() {
        PlusSpringCacheManager m = new PlusSpringCacheManager(); // caffeine=null,等价修改前
        m.setTransactionAware(false);
        return m;
    }

    /**
     * 清掉底层 Redis map,避免容器 reuse 残留干扰。
     */
    private void clearRedis(String name) {
        RedisUtils.getClient().getMap(name).delete();
    }

    /**
     * 直接改 Redis 底层值(绕过被测 manager 的 caffeine),模拟"其他节点/进程改了缓存"。
     */
    private void mutateRedisDirectly(String cacheName, Object key, Object value) {
        mutator.getCache(cacheName).put(key, value);
    }

    @Test
    @DisplayName("修改后:传入 caffeine → 二次读命中 L1,读到旧值(证明 L1 在链路里)")
    void caffeinePresent_secondReadServesStaleFromL1() {
        String cacheName = "test:l1:on";
        clearRedis(cacheName);

        org.springframework.cache.Cache cache = caffeineManager().getCache(cacheName);

        cache.put("k", "v1");                                   // 写入 Redis(+失效本地,无影响)
        assertThat(cache.get("k").get()).isEqualTo("v1");       // 首读回源 → 填充 caffeine

        mutateRedisDirectly(cacheName, "k", "v2");              // 绕过 caffeine 直接改 Redis

        // 关键断言:L1 生效 → 二次读仍是旧值 v1(未回源)
        assertThat(cache.get("k").get())
            .as("caffeine 在读链路里,30s 内应命中本地旧值")
            .isEqualTo("v1");

        // 佐证:Redis 里其实已经是 v2(用无 L1 的旁路读)
        assertThat(mutator.getCache(cacheName).get("k").get()).isEqualTo("v2");
    }

    @Test
    @DisplayName("修改前:caffeine=null → 二次读直连 Redis,读到新值(证明无 L1)")
    void caffeineAbsent_secondReadHitsRedis() {
        String cacheName = "test:l1:off";
        clearRedis(cacheName);

        org.springframework.cache.Cache cache = noCaffeineManager().getCache(cacheName);

        cache.put("k", "v1");
        assertThat(cache.get("k").get()).isEqualTo("v1");

        mutateRedisDirectly(cacheName, "k", "v2");

        // 无 L1 → 二次读必然拿到 Redis 最新值 v2
        assertThat(cache.get("k").get())
            .as("无 caffeine,每次读都直连 Redis")
            .isEqualTo("v2");
    }

    @Test
    @DisplayName("接线校验:应用真实 CacheManager(CacheConfig 修改后)确实带 L1")
    void applicationCacheManager_hasL1() {
        assertThat(applicationCacheManager).isInstanceOf(PlusSpringCacheManager.class);

        String cacheName = "test:l1:app";
        clearRedis(cacheName);

        org.springframework.cache.Cache cache = applicationCacheManager.getCache(cacheName);
        cache.put("k", "v1");
        assertThat(cache.get("k").get()).isEqualTo("v1");       // 填充 L1

        mutateRedisDirectly(cacheName, "k", "v2");

        // 若 CacheConfig 没把 caffeine 传进去,这里会变成 v2 → 断言失败,即回归探针
        assertThat(cache.get("k").get())
            .as("CacheConfig 应已把 caffeine 注入 CacheManager,故命中本地旧值")
            .isEqualTo("v1");
    }

    @Test
    @DisplayName("L1 关闭位:cacheName#0#0#0#0(local=0)即便有 caffeine 也应直连 Redis")
    void localFlagZero_bypassesL1EvenWithCaffeine() {
        String base = "test:l1:flagoff";
        // 第 5 段 local=0 关闭本地缓存;前面 ttl/maxIdle/maxSize 用 0 走 createMap 分支
        String cacheName = base + "#0#0#0#0";
        clearRedis(base);

        org.springframework.cache.Cache cache = caffeineManager().getCache(cacheName);
        cache.put("k", "v1");
        assertThat(cache.get("k").get()).isEqualTo("v1");

        mutateRedisDirectly(base, "k", "v2");                   // 底层 map 名是 base(# 前那段)

        assertThat(cache.get("k").get())
            .as("local=0 显式关闭 L1,应读到 Redis 新值")
            .isEqualTo("v2");
    }

}
