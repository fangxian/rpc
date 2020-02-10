package com.rpc.kafa;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantLock;

public class InterfaceStatisticsConsumer extends Thread{
    private static Logger logger = LoggerFactory.getLogger(InterfaceStatisticsConsumer.class);
    private Properties properties;
    private String[] topics;
    private String kafkaIP;
    private KafkaConsumer<Integer, String> consumer;
    private Map<String, Integer> interfaceCount = new HashMap<>();
    private ReentrantLock lock = new ReentrantLock();
    private ObjectMapper mapper = new ObjectMapper();
    private static InterfaceStatisticsConsumer interfaceStatisticsConsumer = null;
    public static InterfaceStatisticsConsumer getInstance() {
        synchronized (InterfaceStatisticsConsumer.class) {
            if(interfaceStatisticsConsumer == null) {
                interfaceStatisticsConsumer = new InterfaceStatisticsConsumer();
            }
            return interfaceStatisticsConsumer;
        }
    }

    private InterfaceStatisticsConsumer() {
        properties = new Properties();
        decodeProperties();
    }


    private void decodeProperties() {
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("rpckafka-consumer.properties");
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

    private void createConsumer() {
        Properties tempProperties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,"192.168.115.132:9092");
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer");
        //设置 offset自动提交
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        // 自动提交(批量确认)间隔时间
        properties.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        // 获取消息的超时时间
        properties.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "30000");
        // 消费者key与value的反序列化方式
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.IntegerDeserializer");
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringDeserializer");
        //对于当前groupid来说，消息的offset从最早的消息开始消费 与之相反的是latest
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,"earliest");
        consumer= new KafkaConsumer<>(properties);
        consumer.subscribe(Arrays.asList(topics));
    }

    @Override
    public void run() {
        while(true) {
            consumeMsg();
        }
    }

    public void consumeMsg(){
        ConsumerRecords<Integer, String> records = consumer.poll(Duration.ofSeconds(1));
        records.forEach(record->{
            //msg 从 string转换成json
            try {
                Map<String, Integer> temp = mapper.readValue(record.value(), Map.class);
                lock.lock();
                interfaceCount.putAll(temp);
            } catch (Exception e){
                logger.error(e.toString());
            } finally {
                lock.unlock();
            }
        });
    }


    public Map<String, Integer> getInterfaceCount() {
        lock.lock();
        Map<String, Integer> temp = interfaceCount;
        lock.unlock();
        return temp;
    }


}
