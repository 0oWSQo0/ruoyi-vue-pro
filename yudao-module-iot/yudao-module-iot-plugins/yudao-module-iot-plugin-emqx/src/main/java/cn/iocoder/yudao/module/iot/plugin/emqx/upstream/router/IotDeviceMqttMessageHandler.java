package cn.iocoder.yudao.module.iot.plugin.emqx.upstream.router;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.module.iot.api.device.IotDeviceUpstreamApi;
import cn.iocoder.yudao.module.iot.api.device.dto.control.upstream.IotDeviceEventReportReqDTO;
import cn.iocoder.yudao.module.iot.api.device.dto.control.upstream.IotDevicePropertyReportReqDTO;
import cn.iocoder.yudao.module.iot.plugin.common.util.IotPluginCommonUtils;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.buffer.Buffer;
import io.vertx.mqtt.MqttClient;
import io.vertx.mqtt.messages.MqttPublishMessage;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * IoT 设备 MQTT 消息处理器
 * <p>
 * 参考：
 * <p>
 * "<a href=
 * "https://help.aliyun.com/zh/iot/user-guide/device-properties-events-and-services?spm=a2c4g.11186623.0.0.97a72915vRck44#section-g4j-5zg-12b">...</a>">
 */
@Slf4j
public class IotDeviceMqttMessageHandler {

    // 设备上报属性 标准 JSON
    // 请求Topic：/sys/${productKey}/${deviceName}/thing/event/property/post
    // 响应Topic：/sys/${productKey}/${deviceName}/thing/event/property/post_reply
    // 设备上报事件 标准 JSON
    // 请求Topic：/sys/${productKey}/${deviceName}/thing/event/${tsl.event.identifier}/post
    // 响应Topic：/sys/${productKey}/${deviceName}/thing/event/${tsl.event.identifier}/post_reply

    private static final String SYS_TOPIC_PREFIX = "/sys/";
    private static final String PROPERTY_POST_TOPIC = "/thing/event/property/post";
    private static final String EVENT_POST_TOPIC_PREFIX = "/thing/event/";
    private static final String EVENT_POST_TOPIC_SUFFIX = "/post";
    private static final String REPLY_SUFFIX = "_reply";
    private static final int SUCCESS_CODE = 200;
    private static final String SUCCESS_MESSAGE = "success";
    private static final String PROPERTY_METHOD = "thing.event.property.post";
    private static final String EVENT_METHOD_PREFIX = "thing.event.";
    private static final String EVENT_METHOD_SUFFIX = ".post";

    private final IotDeviceUpstreamApi deviceUpstreamApi;
    private final MqttClient mqttClient;

    public IotDeviceMqttMessageHandler(IotDeviceUpstreamApi deviceUpstreamApi, MqttClient mqttClient) {
        this.deviceUpstreamApi = deviceUpstreamApi;
        this.mqttClient = mqttClient;
    }

    /**
     * 处理MQTT消息
     *
     * @param message MQTT发布消息
     */
    public void handle(MqttPublishMessage message) {
        String topic = message.topicName();
        String payload = message.payload().toString();
        log.info("[messageHandler][接收到消息][topic: {}][payload: {}]", topic, payload);

        try {
            if (payload == null || payload.isEmpty()) {
                log.warn("[messageHandler][消息内容为空][topic: {}]", topic);
                return;
            }
            handleMessage(topic, payload);
        } catch (Exception e) {
            log.error("[messageHandler][处理消息失败][topic: {}][payload: {}]", topic, payload, e);
        }
    }

    /**
     * 根据主题类型处理消息
     *
     * @param topic   主题
     * @param payload 消息内容
     */
    private void handleMessage(String topic, String payload) {
        // 校验前缀
        if (!topic.startsWith(SYS_TOPIC_PREFIX)) {
            log.warn("[handleMessage][未知的消息类型][topic: {}]", topic);
            return;
        }

        // 处理设备属性上报消息
        if (topic.endsWith(PROPERTY_POST_TOPIC)) {
            log.info("[handleMessage][接收到设备属性上报][topic: {}]", topic);
            handlePropertyPost(topic, payload);
            return;
        }

        // 处理设备事件上报消息
        if (topic.contains(EVENT_POST_TOPIC_PREFIX) && topic.endsWith(EVENT_POST_TOPIC_SUFFIX)) {
            log.info("[handleMessage][接收到设备事件上报][topic: {}]", topic);
            handleEventPost(topic, payload);
            return;
        }

        // 未知消息类型
        log.warn("[handleMessage][未知的消息类型][topic: {}]", topic);
    }

    /**
     * 处理设备属性上报消息
     *
     * @param topic   主题
     * @param payload 消息内容
     */
    private void handlePropertyPost(String topic, String payload) {
        try {
            // 解析消息内容
            JSONObject jsonObject = JSONUtil.parseObj(payload);
            String[] topicParts = parseTopic(topic);
            if (topicParts == null) {
                return;
            }

            // 构建设备属性上报请求对象
            IotDevicePropertyReportReqDTO reportReqDTO = buildPropertyReportDTO(jsonObject, topicParts);

            // 调用上游 API 处理设备上报数据
            deviceUpstreamApi.reportDeviceProperty(reportReqDTO);
            log.info("[handlePropertyPost][处理设备属性上报成功][topic: {}]", topic);

            // 发送响应消息
            sendResponse(topic, jsonObject, PROPERTY_METHOD, null);
        } catch (Exception e) {
            log.error("[handlePropertyPost][处理设备属性上报失败][topic: {}][payload: {}]", topic, payload, e);
        }
    }

    /**
     * 处理设备事件上报消息
     *
     * @param topic   主题
     * @param payload 消息内容
     */
    private void handleEventPost(String topic, String payload) {
        try {
            // 解析消息内容
            JSONObject jsonObject = JSONUtil.parseObj(payload);
            String[] topicParts = parseTopic(topic);
            if (topicParts == null) {
                return;
            }

            // 构建设备事件上报请求对象
            IotDeviceEventReportReqDTO reportReqDTO = buildEventReportDTO(jsonObject, topicParts);

            // 调用上游 API 处理设备上报数据
            deviceUpstreamApi.reportDeviceEvent(reportReqDTO);
            log.info("[handleEventPost][处理设备事件上报成功][topic: {}]", topic);

            // 从 topic 中获取事件标识符
            String eventIdentifier = getEventIdentifier(topicParts, topic);
            if (eventIdentifier == null) {
                return;
            }

            // 发送响应消息
            String method = EVENT_METHOD_PREFIX + eventIdentifier + EVENT_METHOD_SUFFIX;
            sendResponse(topic, jsonObject, method, null);
        } catch (Exception e) {
            log.error("[handleEventPost][处理设备事件上报失败][topic: {}][payload: {}]", topic, payload, e);
        }
    }

    /**
     * 解析主题，获取主题各部分
     *
     * @param topic 主题
     * @return 主题各部分数组，如果解析失败返回null
     */
    private String[] parseTopic(String topic) {
        String[] topicParts = topic.split("/");
        if (topicParts.length < 7) {
            log.warn("[parseTopic][主题格式不正确][topic: {}]", topic);
            return null;
        }
        return topicParts;
    }

    /**
     * 从主题部分中获取事件标识符
     *
     * @param topicParts 主题各部分
     * @param topic      原始主题，用于日志
     * @return 事件标识符，如果获取失败返回null
     */
    private String getEventIdentifier(String[] topicParts, String topic) {
        try {
            return topicParts[6];
        } catch (ArrayIndexOutOfBoundsException e) {
            log.warn("[getEventIdentifier][无法从主题中获取事件标识符][topic: {}][topicParts: {}]",
                    topic, Arrays.toString(topicParts));
            return null;
        }
    }

    /**
     * 发送响应消息
     *
     * @param topic      原始主题
     * @param jsonObject 原始消息JSON对象
     * @param method     响应方法
     * @param customData 自定义数据，可为null
     */
    private void sendResponse(String topic, JSONObject jsonObject, String method, JSONObject customData) {
        String replyTopic = topic + REPLY_SUFFIX;
        JSONObject data = customData != null ? customData : new JSONObject();

        JSONObject response = new JSONObject()
                .set("id", jsonObject.getStr("id"))
                .set("code", SUCCESS_CODE)
                .set("data", data)
                .set("message", SUCCESS_MESSAGE)
                .set("method", method);

        try {
            mqttClient.publish(replyTopic,
                    Buffer.buffer(response.toString()),
                    MqttQoS.AT_LEAST_ONCE,
                    false,
                    false);
            log.info("[sendResponse][发送响应消息成功][topic: {}]", replyTopic);
        } catch (Exception e) {
            log.error("[sendResponse][发送响应消息失败][topic: {}][response: {}]",
                    replyTopic, response.toString(), e);
        }
    }

    /**
     * 构建设备属性上报请求对象
     *
     * @param jsonObject 消息内容
     * @param topicParts 主题部分
     * @return 设备属性上报请求对象
     */
    private IotDevicePropertyReportReqDTO buildPropertyReportDTO(JSONObject jsonObject, String[] topicParts) {
        IotDevicePropertyReportReqDTO reportReqDTO = new IotDevicePropertyReportReqDTO();
        reportReqDTO.setRequestId(jsonObject.getStr("id"));
        reportReqDTO.setProcessId(IotPluginCommonUtils.getProcessId());
        reportReqDTO.setReportTime(LocalDateTime.now());
        reportReqDTO.setProductKey(topicParts[2]);
        reportReqDTO.setDeviceName(topicParts[3]);
        reportReqDTO.setProperties(jsonObject.getJSONObject("params"));
        return reportReqDTO;
    }

    /**
     * 构建设备事件上报请求对象
     *
     * @param jsonObject 消息内容
     * @param topicParts 主题部分
     * @return 设备事件上报请求对象
     */
    private IotDeviceEventReportReqDTO buildEventReportDTO(JSONObject jsonObject, String[] topicParts) {
        IotDeviceEventReportReqDTO reportReqDTO = new IotDeviceEventReportReqDTO();
        reportReqDTO.setRequestId(jsonObject.getStr("id"));
        reportReqDTO.setProcessId(IotPluginCommonUtils.getProcessId());
        reportReqDTO.setReportTime(LocalDateTime.now());
        reportReqDTO.setProductKey(topicParts[2]);
        reportReqDTO.setDeviceName(topicParts[3]);
        reportReqDTO.setIdentifier(topicParts[6]);
        reportReqDTO.setParams(jsonObject.getJSONObject("params"));
        return reportReqDTO;
    }
}