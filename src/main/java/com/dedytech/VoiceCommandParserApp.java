package com.dedytech;

import com.dedytech.config.StreamsConfiguration;
import com.dedytech.services.MockSttClient;
import com.dedytech.services.MockTranslateClient;
import org.apache.kafka.streams.KafkaStreams;

public class VoiceCommandParserApp {
    public static void main(String[] args) {
        var streamsConfiguration = new StreamsConfiguration();
        var voiceParserTopology = new VoiceCommandParserTopology(new MockTranslateClient(), new MockSttClient());

        var kafkaStreams = new KafkaStreams(voiceParserTopology.createTopology(), streamsConfiguration.streamsConfiguration());

        kafkaStreams.start();

        Runtime.getRuntime().addShutdownHook(new Thread(kafkaStreams::close));
    }
}