package org.dromara.chain.evm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dromara.chain.api.exception.ChainAdapterException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 极简 EVM JSON-RPC 客户端(JDK HttpClient,无第三方 SDK 依赖)
 *
 * <p>只覆盖扫块/转账所需的少量方法调用;节点返回 error 时统一抛
 * {@link ChainAdapterException},由业务层决定中止策略。
 *
 * @author jarvey
 */
public class EvmJsonRpcClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpClient http;
    private final URI endpoint;
    private final AtomicLong idSeq = new AtomicLong(1);

    public EvmJsonRpcClient(String rpcUrl) {
        this.endpoint = URI.create(rpcUrl);
        this.http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    }

    /**
     * 发起一次 JSON-RPC 调用
     *
     * @param method RPC 方法名
     * @param params 参数列表(按 JSON-RPC 位置参数序列化)
     * @return result 节点(可能为 null 节点,如查询不存在的交易)
     */
    public JsonNode call(String method, Object... params) {
        try {
            ObjectNode req = MAPPER.createObjectNode();
            req.put("jsonrpc", "2.0");
            req.put("id", idSeq.getAndIncrement());
            req.put("method", method);
            req.set("params", MAPPER.valueToTree(params));
            HttpRequest httpRequest = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(req)))
                .build();
            HttpResponse<String> resp = http.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                throw new ChainAdapterException("JSON-RPC HTTP 状态异常: " + resp.statusCode() + ", method=" + method);
            }
            JsonNode body = MAPPER.readTree(resp.body());
            JsonNode error = body.get("error");
            if (error != null && !error.isNull()) {
                throw new ChainAdapterException("JSON-RPC 节点返回错误: method=" + method + ", error=" + error);
            }
            return body.get("result");
        } catch (IOException e) {
            throw new ChainAdapterException("JSON-RPC 通信失败: method=" + method, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ChainAdapterException("JSON-RPC 调用被中断: method=" + method, e);
        }
    }

}
