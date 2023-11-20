package com.confluent;

import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.connect.mirror.RemoteClusterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class TestConsumerGroup {
    private static final Logger log = LoggerFactory.getLogger(TestConsumerGroup.class);

    //not working properly?
    public static void main(String[] args) throws Exception {
        Map<String, Object> configs = new HashMap<>();
        configs.put("bootstrap.servers", "localhost:9093");
        configs.put("replication.policy.class","org.apache.kafka.connect.mirror.IdentityReplicationPolicy");
        Map<TopicPartition, OffsetAndMetadata> offsets =
                RemoteClusterUtils.translateOffsets(configs,
                        "east-kafka",
                        "my-group",
                        Duration.ofSeconds(5L));
        for (Map.Entry<TopicPartition, OffsetAndMetadata> o : offsets.entrySet()) {
            System.out.println(o.getKey().partition() + " : " + o.getValue().offset());
        }
    }
}
