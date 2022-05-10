package org.apache.flink.streaming.dis.example;

import com.huaweicloud.dis.DISConfig;
import com.huaweicloud.dis.adapter.common.consumer.DisConsumerConfig;

import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.dis.FlinkDisConsumer;
import org.apache.flink.streaming.connectors.dis.config.RebalanceMode;
import org.apache.flink.streaming.connectors.dis.internals.DisCommitCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;

public class DISFlinkStreamingSourceJavaExample {
    private static final Logger LOGGER = LoggerFactory.getLogger(DISFlinkStreamingSourceJavaExample.class);

    public static void main(String[] args) {
        LOGGER.info("Start DIS Flink Streaming Source Java Demo.");

        // DIS终端节点，如 https://dis.cn-north-1.myhuaweicloud.com
        String endpoint;
        // DIS服务所在区域ID，如 cn-north-1
        String region;
        // 用户的AK
        String ak;
        // 用户的SK
        String sk;
        // 用户的项目ID
        String projectId;
        // DIS通道名称
        String streamName;
        // 消费策略，只有当分区没有Checkpoint或者Checkpoint过期时，才会使用此配置的策略；如果存在有效的Checkpoint，则会从此Checkpoint开始继续消费
        // 取值有： LATEST      从最新的数据开始消费，此策略会忽略通道中已有数据
        //         EARLIEST    从最老的数据开始消费，此策略会获取通道中所有的有效数据
        String startingOffsets;
        // 消费组标识，同一个消费组下的不同客户端可以同时消费同一个通道
        String groupId;
        String outputPath;

        if (args.length < 8) {
            LOGGER.error("args is wrong, should be [endpoint region ak sk projectId streamName startingOffsets groupId]");
            return;
        }

        endpoint = args[0];
        region = args[1];
        ak = args[2];
        sk = args[3];
        projectId = args[4];
        streamName = args[5];
        startingOffsets = args[6];
        groupId = args[7];
        // outputPath = args[8];

        // DIS Config
        DISConfig disConfig = DISConfig.buildDefaultConfig();
       disConfig.put(DISConfig.PROPERTY_ENDPOINT, endpoint);
        // disConfig.put(DISConfig.PROPERTY_ENDPOINT, "https://10.78.12.103:8443/dis-gw");
        // disConfig.put(DISConfig.PROPERTY_MANAGER_ENDPOINT, "https://10.78.12.103:8443");
        disConfig.put(DISConfig.PROPERTY_REGION_ID, region);
        disConfig.put(DISConfig.PROPERTY_AK, ak);
        disConfig.put(DISConfig.PROPERTY_SK, sk);
        disConfig.put(DISConfig.PROPERTY_PROJECT_ID, projectId);
        disConfig.put(DisConsumerConfig.AUTO_OFFSET_RESET_CONFIG, startingOffsets);
        disConfig.put(DisConsumerConfig.GROUP_ID_CONFIG, groupId);
        // 是否主动更新分片信息及更新时间间隔（毫秒），若有主动扩缩容需求，可以开启
        disConfig.put(FlinkDisConsumer.KEY_PARTITION_DISCOVERY_INTERVAL_MILLIS, "10000");
        disConfig.put(DisConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        String[] streamNames = streamName.split(",");

        // String outputPath = "obs://B4Y3XYD0TO1YRUZEKT9Y:vdaGE4y4mijxrHRj4u1T7Fg8qZIZZbFEooGMJpOp@obs.cn-north-6.myhuaweicloud.com:443/dis-auto-test-cn-north-6/test2";

        try {
            // get an ExecutionEnvironment
            StreamExecutionEnvironment env =
                    StreamExecutionEnvironment.getExecutionEnvironment();

            // 开启Flink CheckPoint配置，周期为10000毫秒，开启时若触发CheckPoint，会将Offset信息记录到DIS服务
            env.enableCheckpointing(10000);

            // env.setParallelism(2);

            // 设置时间特性，设置TimeCharacteristic.EventTime可以获取到记录写入DIS的时间
            env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);

            // Create a DIS Consumer
            FlinkDisConsumer<String> consumer =
                new FlinkDisConsumer<String>(
                    Arrays.asList(streamNames),
                    // Arrays.asList("stz_test3", "stz_test4", "stz_test5"),
                    // Collections.singletonList(streamName),
                    new SimpleStringSchema(),
                    disConfig);

            // Create DIS Consumer data source
            DataStream<String> stream = env.addSource(consumer).process(new MyFunction());
            // DataStream<String> stream = env.addSource(consumer).disableChaining();

            // Print
            // stream.print();

            // Write as Text
//            stream.writeAsText("/tmp/flink/stz_test_sink");

            // 创建文件输出流
            // final StreamingFileSink<String> sink = StreamingFileSink
                // 指定文件输出路径与行编码格式
                // .forRowFormat(new Path(outputPath), new SimpleStringEncoder<String>("UTF-8"))
                // 指定文件输出路径批量编码格式，以parquet格式输出
                // .forBulkFormat(new Path(outputPath), ParquetAvroWriters.forGenericRecord(schema))
                // 指定自定义桶分配器
                // .withBucketAssigner(new DateTimeBucketAssigner<>())
                // 指定滚动策略
                // .withRollingPolicy(OnCheckpointRollingPolicy.build())
                // .build();

            // Add sink for DIS Consumer data source
            // stream.addSink(sink).disableChaining().name("obs");

            env.execute();
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
        }
    }
}
