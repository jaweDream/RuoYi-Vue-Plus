package org.dromara.test.datapermission;

import cn.dev33.satoken.context.SaTokenContextForThreadLocalStaff;
import cn.dev33.satoken.context.mock.SaRequestForMock;
import cn.dev33.satoken.context.mock.SaResponseForMock;
import cn.dev33.satoken.context.mock.SaStorageForMock;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.dromara.common.core.utils.ThreadUtils;
import org.dromara.common.mybatis.helper.DataPermissionHelper;
import org.dromara.system.api.domain.RoleDTO;
import org.dromara.system.api.model.LoginUser;
import org.dromara.system.domain.vo.SysUserVo;
import org.dromara.system.mapper.SysUserMapper;
import org.dromara.test.container.TestcontainersBase;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * 数据权限上下文 跨虚拟线程传播 集成测试。
 *
 * <p>背景:本测试刻意复现框架作者踩过的同一类 ThreadLocal 缺陷——
 * 提交 {@code 84afd6e6c}(2026-05-16)把工作流办理人解析改成
 * {@link ThreadUtils#virtualSubmitAll} 并发执行,12 天后提交 {@code 16b54b327}
 * (“fix 修复 虚拟线程导致的问题”)又整段回滚回顺序执行。根因是:登录态 / 数据权限
 * 上下文挂在 Sa-Token 的 ThreadLocal 存储({@code SaHolder.getStorage()})上,
 * 而 6.X 没有 TTL,上下文不会跟随任务切到新的虚拟线程——子线程上
 * {@code SaHolder.getStorage()} 尚未初始化,{@code @DataPermission} 查询直接抛异常。</p>
 *
 * <p>三个用例构成对照:</p>
 * <ol>
 *   <li>{@link #filterAppliesOnRequestThread()} —— 基线(应绿):请求线程内上下文齐全,
 *       {@code @DataPermission} 真实改写 SQL(ignore 取到全量 → 限制性 scope 过滤为空)。</li>
 *   <li>{@link #contextShouldPropagateAcrossVirtualThread()} —— 缺陷复现(当前红):
 *       同一查询切到虚拟线程后丢失上下文。本用例即“把 bug 钉成 CI 红灯”的探针,
 *       须在实现上下文传播(TTL / 显式重放 / ScopedValue)后才会转绿。</li>
 *   <li>{@link #manualContextPropagationRestoresFilter()} —— 修复范式(应绿):
 *       在子虚拟线程内手动重建 Sa-Token 上下文并重放数据权限变量后,过滤恢复正常,
 *       证明“显式传播”就是这条边界的正解。</li>
 * </ol>
 *
 * <p>⚠️ 前置依赖:本类 {@code extends TestcontainersBase}(容器单例 + 接线 + schema,
 * 见 {@code docs/测试规范.md §4.2})。当前工作树尚无该基类,需先按规范重建,本测试方能编译运行。</p>
 *
 * @author jarvey
 */
@Tag("dev")
@ActiveProfiles({"dev", "test"})
@SpringBootTest
@DisplayName("数据权限上下文跨虚拟线程传播")
class DataPermissionVirtualThreadIntegrationTest extends TestcontainersBase {

    /**
     * 不可能命中任何种子行 create_by 的用户 ID(种子 create_by 均为 admin …001)。
     * 用于 SELF scope,使“仅本人”过滤确定性地把结果收敛为空,从而证明拦截器真的改写了 SQL。
     */
    private static final long FAKE_USER_ID = 99999999L;

    @Autowired
    private SysUserMapper userMapper;

    @BeforeEach
    void initSaTokenContext() {
        // MOCK 测试不经 Sa-Token filter,须手动初始化 ThreadLocal 上下文,否则抛“SaTokenContext 上下文尚未初始化”
        SaTokenContextForThreadLocalStaff.setModelBox(
            new SaRequestForMock(), new SaResponseForMock(), new SaStorageForMock());
    }

    @AfterEach
    void clearSaTokenContext() {
        SaTokenContextForThreadLocalStaff.clearModelBox();
    }

    @Test
    @DisplayName("基线:请求线程内 @DataPermission 过滤生效")
    void filterAppliesOnRequestThread() {
        // 忽略数据权限取“全量”作为上界,证明种子库确有数据
        List<SysUserVo> all = DataPermissionHelper.ignore(
            () -> userMapper.selectUserList(Wrappers.emptyWrapper()));
        assertThat(all).as("种子库应存在用户数据").isNotEmpty();

        // 注入“仅本人(5)” + 不存在的 userId → create_by = 99999999 → 过滤为空
        DataPermissionHelper.setVariable("user", selfScopeUser());
        List<SysUserVo> filtered = userMapper.selectUserList(Wrappers.emptyWrapper());
        assertThat(filtered)
            .as("上下文齐全时拦截器应把全量(%d 行)按 SELF 收敛为空,证明 SQL 被真实改写", all.size())
            .isEmpty();
    }

    @Test
    @DisplayName("缺陷复现:@DataPermission 上下文不会跨虚拟线程传播(期望被修复 → 当前红灯)")
    void contextShouldPropagateAcrossVirtualThread() {
        DataPermissionHelper.setVariable("user", selfScopeUser());
        List<Long> onThread = ids(userMapper.selectUserList(Wrappers.emptyWrapper()));

        // 期望:同一 @DataPermission 查询切到虚拟线程后,行为应与请求线程一致(过滤后相等,既不抛异常也不返回全量)。
        // 现状:子虚拟线程上 SaHolder.getStorage() 未初始化 → currentUser() 抛“上下文尚未初始化”
        //       → 被 ThreadUtils.virtualSubmitAll 包成 RuntimeException → 本断言失败(红灯)。
        // 上下文传播(TTL / 显式重放 / ScopedValue)落地后,此用例自动转绿。
        assertThatCode(() -> {
            List<SysUserVo> crossThread = ThreadUtils.virtualSubmitAll(
                () -> userMapper.selectUserList(Wrappers.emptyWrapper())).getFirst();
            assertThat(ids(crossThread)).isEqualTo(onThread);
        }).as("@DataPermission 查询不应因切到虚拟线程而改变行为(应过滤后一致,而非抛异常 / 返回全量)")
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("修复范式:虚拟线程内手动重建上下文后,过滤恢复正常")
    void manualContextPropagationRestoresFilter() {
        LoginUser user = selfScopeUser();
        DataPermissionHelper.setVariable("user", user);
        List<Long> onThread = ids(userMapper.selectUserList(Wrappers.emptyWrapper()));

        List<Long> crossThread = ids(ThreadUtils.virtualSubmitAll(() -> {
            // 关键:在子虚拟线程上重新初始化 Sa-Token 上下文,并把父线程的数据权限变量重放进去
            SaTokenContextForThreadLocalStaff.setModelBox(
                new SaRequestForMock(), new SaResponseForMock(), new SaStorageForMock());
            try {
                DataPermissionHelper.setVariable("user", user);
                return userMapper.selectUserList(Wrappers.emptyWrapper());
            } finally {
                SaTokenContextForThreadLocalStaff.clearModelBox();
            }
        }).getFirst());

        assertThat(crossThread)
            .as("手动传播上下文后,跨虚拟线程结果应与请求线程一致")
            .isEqualTo(onThread);
    }

    /**
     * 构造一个“仅本人(5)”数据范围、且 userId 不命中任何种子行的普通登录用户。
     *
     * <p>不调用 {@code StpUtil.login}:{@code isSuperAdmin()} 只认 Sa-Token token 的 userId,
     * 取不到 → false → 不走超管短路,数据权限过滤才会真实触发(见 docs/测试规范.md §5.2)。</p>
     *
     * @return 预置的登录用户
     */
    private LoginUser selfScopeUser() {
        LoginUser user = new LoginUser();
        user.setUserId(FAKE_USER_ID);
        user.setDeptId(100L);        // SELF 模板不使用 deptId,占位即可
        user.setUserType("sys_user");
        RoleDTO role = new RoleDTO();
        role.setRoleId(5L);
        role.setDataScope("5");      // DataScopeType.SELF
        user.setRoles(List.of(role));
        return user;
    }

    /**
     * 提取并排序用户 ID,用于稳定比较(SysUserVo 未定义 equals,不能直接比对象)。
     *
     * @param users 用户视图列表
     * @return 升序的用户 ID 列表
     */
    private static List<Long> ids(List<SysUserVo> users) {
        return users.stream().map(SysUserVo::getUserId).sorted().toList();
    }

}
