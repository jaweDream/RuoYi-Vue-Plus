package org.dromara.test.datapermission;

import org.dromara.common.core.utils.ThreadUtils;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 虚拟线程上下文传播 单元测试 —— 哑铃的「便宜端」。
 *
 * <p>不起 Spring、不连库、不需要 Docker,几毫秒出红/绿。它钉死的是框架工具
 * {@link ThreadUtils#virtualSubmitAll} 的传播「契约」:每个任务跑在一条全新的虚拟线程上,
 * <b>不会继承调用方的普通 ThreadLocal</b>。</p>
 *
 * <p>这正是缺陷的「机制内核」:登录态 / 数据权限上下文挂在普通 ThreadLocal
 * (Sa-Token 的 {@code SaHolder.getStorage()})上,而 6.X 无 TTL —— 所以一旦把
 * {@code @DataPermission} 查询丢进 {@code virtualSubmitAll},上下文就丢了。作者
 * {@code 84afd6e6c} 这么改、{@code 16b54b327} 又回滚,就是栽在这条契约上。</p>
 *
 * <p><b>边界(为什么它绿、而集成测试红):</b>本类只证「通用机制 / 工具契约」,是<b>回归护栏</b>
 * (现状如此 → 绿;哪天有人给 virtualSubmitAll 加了上下文传播,它会翻红提醒契约变了)。
 * 真实 {@code @DataPermission} 链路切线程后到底丢不丢过滤,属于哑铃的「昂贵端」,由
 * {@code DataPermissionVirtualThreadIntegrationTest}(全量 @SpringBootTest + Testcontainers)
 * 作为<b>红灯探针</b>在生产同构环境里证明。两端各证一半,缺一不可。</p>
 *
 * @author jarvey
 */
@Tag("dev")  // 必须:root pom surefire <groups>=dev,不打此标签 Maven/CI 下不会执行(见 docs/测试规范.md §6.2)
@DisplayName("虚拟线程上下文传播")
class VirtualThreadContextPropagationUnitTest {

    @Test
    @DisplayName("ThreadUtils.virtualSubmitAll 不继承调用方的 ThreadLocal")
    void virtualSubmitAllDoesNotInheritCallerThreadLocal() {
        ThreadLocal<String> ctx = new ThreadLocal<>();
        try {
            ctx.set("user-A");
            assertThat(ctx.get()).as("调用方线程上可见").isEqualTo("user-A");

            // 切到虚拟线程:普通 ThreadLocal 不被子线程继承 → 任务里读到 null
            String seenInVirtualThread = ThreadUtils.virtualSubmitAll(ctx::get).getFirst();
            assertThat(seenInVirtualThread)
                .as("普通 ThreadLocal 不跨虚拟线程传播 —— 这就是 @DataPermission 上下文跨线程丢失的根因")
                .isNull();

            assertThat(ctx.get()).as("批量执行后调用方自身上下文不被回写污染").isEqualTo("user-A");
        } finally {
            ctx.remove();
        }
    }

    @Test
    @DisplayName("修复范式:任务内手动捕获+重放即可恢复上下文")
    void manualReplayInsideTaskRestoresContext() {
        ThreadLocal<String> ctx = new ThreadLocal<>();
        try {
            ctx.set("user-A");
            String captured = ctx.get();  // 调用方捕获

            String seenInVirtualThread = ThreadUtils.virtualSubmitAll(() -> {
                ctx.set(captured);        // 子虚拟线程重放
                try {
                    return ctx.get();
                } finally {
                    ctx.remove();         // 用完即清
                }
            }).getFirst();

            assertThat(seenInVirtualThread)
                .as("显式捕获+重放后任务内可见调用方上下文 —— 这就是跨线程边界的正解")
                .isEqualTo("user-A");
        } finally {
            ctx.remove();
        }
    }

}
