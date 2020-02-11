package com.rpc.kafka;

import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;

public class InterfaceStatisticsProducer {
    private static Logger logger = LoggerFactory.getLogger(InterfaceStatisticsProducer.class);
    private String kafkaIP;
    private int port;
    private String[] topics;
    private static InterfaceStatisticsProducer interfaceStatisticsProducer = null;
    private Properties properties;
    private KafkaProducer<Integer, String> producer;
    public static InterfaceStatisticsProducer getInstance() {
        synchronized (InterfaceStatisticsProducer.class) {
            if(interfaceStatisticsProducer ==null) {
                interfaceStatisticsProducer = new InterfaceStatisticsProducer();
            }
            return interfaceStatisticsProducer;
        }
    }

    private InterfaceStatisticsProducer() {
        properties = new Properties();
        decodeProperties();
        createProducer();
    }

    private void decodeProperties() {
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("rpckafka-producer.properties");
        try {
            properties.load(inputStream);
        } catch (IOException e) {
            logger.error(e.toString());
        }
        kafkaIP = properties.getProperty("address");
        topics = properties.getProperty("topics").split(",");
        logger.info("kafka iaddres: " + kafkaIP);


        for(int i = 0; i<topics.length;i++) {
            logger.info("subcribe topics: "+topics[i]);
        }

    }

    private void createProducer() {
        Properties kfkProperties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaIP);
        properties.put(ProducerConfig.CLIENT_ID_CONFIG, "monitor-producer");
        // key的序列化
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                IntegerSerializer.class.getName());
        // value的序列化
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class.getName());
        // 批量发送大小：生产者发送多个消息到broker的同一个分区时，为了减少网络请求，通过批量方式提交消息，默认16kb
        properties.put(ProducerConfig.BATCH_SIZE_CONFIG, 16 * 1024);
        // 批量发送间隔时间：为每次发送到broker的请求增加一些delay，聚合更多的消息，提高吞吐量
        properties.put(ProducerConfig.LINGER_MS_CONFIG, 1000);

        producer=new KafkaProducer<>(properties);
    }

    public void produce(String topic, String msg) {
        if(Arrays.asList(this.topics).contains(topic)){
            producer.send(new ProducerRecord<>(topic, msg), new Callback() {
                @Override
                public void onCompletion(RecordMetadata recordMetadata, Exception e) {
                    logger.info("callback : " + recordMetadata.offset() + "->" + recordMetadata.partition());
                }
            });
        } else {
            logger.error("kafka does not have topic: {}", topic);
        }
    }

}
