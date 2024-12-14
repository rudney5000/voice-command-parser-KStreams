package com.dedytech;

import com.dedytech.model.ParsedVoiceCommand;
import com.dedytech.model.VoiceCommand;
import com.dedytech.serdes.JsonSerde;
import com.dedytech.services.SpeechToTextService;
import com.dedytech.services.TranslateService;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.TopologyTestDriver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Properties;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class VoiceCommandParserTopologyTest {

    @Mock
    private SpeechToTextService speechToTextService;

    @Mock
    private TranslateService translateService;

    private TopologyTestDriver topologyTestDriver;
    private TestInputTopic<String, VoiceCommand> voiceCommandInputTopic;
    private TestOutputTopic<String, ParsedVoiceCommand> recognizedCommandsOutputTopic;
    private TestOutputTopic<String, ParsedVoiceCommand> unrecognizedCommandsOutputTopic;

    @InjectMocks
    private VoiceCommandParserTopology voiceCommandParserTopology;

    @BeforeEach
    void setUp() {
        voiceCommandParserTopology = new VoiceCommandParserTopology(translateService, speechToTextService, 0.90);
        var voiceCommandParserTopology = this.voiceCommandParserTopology;
        var topology = voiceCommandParserTopology.createTopology();
        var props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "test");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29092");
        topologyTestDriver = new TopologyTestDriver(topology, props);
        var voiceCommandJsonSerde = new JsonSerde<>(VoiceCommand.class);
        var parseVoiceCommandJsonSerde = new JsonSerde<>(ParsedVoiceCommand.class);
        topologyTestDriver.createInputTopic(VoiceCommandParserTopology.VOICE_COMMANDS_TOPIC, Serdes.String().serializer(), voiceCommandJsonSerde.serializer());
        voiceCommandInputTopic = topologyTestDriver.createInputTopic(VoiceCommandParserTopology.VOICE_COMMANDS_TOPIC, Serdes.String().serializer(), voiceCommandJsonSerde.serializer());
        recognizedCommandsOutputTopic = topologyTestDriver.createOutputTopic(VoiceCommandParserTopology.RECOGNIZED_COMMANDS_TOPIC, Serdes.String().deserializer(), parseVoiceCommandJsonSerde.deserializer());
        unrecognizedCommandsOutputTopic = topologyTestDriver.createOutputTopic(VoiceCommandParserTopology.UNRECOGNIZED_COMMANDS_TOPIC, Serdes.String().deserializer(), parseVoiceCommandJsonSerde.deserializer());
    }

    @Test
    @DisplayName("Given an English voice command, when processed correctly Then I receive a ParsedVoiceCommand in the correct format")
    void testScenario1() {
        // Pre-conditions (given)
        byte[] randomBytes = new byte[20];
        new Random().nextBytes(randomBytes);
        var voiceCommand = VoiceCommand.builder()
                .id(UUID.randomUUID().toString())
                .audio(randomBytes)
                .audioCode("FLAC")
                .language("en-US")
                .build();

        var parseVoiceCommand1 = ParsedVoiceCommand.builder()
                .id(voiceCommand.getId())
                .language("en-US")
                .text("call john")
                .probability(0.98)
                .build();

        given(speechToTextService.speechToText(voiceCommand)).willReturn(parseVoiceCommand1);
        // Actions(when)
        voiceCommandInputTopic.pipeInput(voiceCommand);

        // Verifications(then)
        var parseVoiceCommand = recognizedCommandsOutputTopic.readRecord().value();

        assertEquals(voiceCommand.getId(), parseVoiceCommand.getId());
        assertEquals("call john", parseVoiceCommand.getText());
    }

    @DisplayName("Given a non-English voice command, when processed correctly Then I receive a ParsedVoiceCommand in the recognized-commands topic.")
    @Test
    void testScenario2() {
        // Pre-conditions (given)
        byte[] randomBytes = new byte[20];
        new Random().nextBytes(randomBytes);
        var voiceCommand = VoiceCommand.builder()
                .id(UUID.randomUUID().toString())
                .audio(randomBytes)
                .audioCode("FLAC")
                .language("es-AR")
                .build();

        var parseVoiceCommand1 = ParsedVoiceCommand.builder()
                .id(voiceCommand.getId())
                .language("es-AR")
                .text("llamar a Juan")
                .probability(0.98)
                .build();

        given(speechToTextService.speechToText(voiceCommand)).willReturn(parseVoiceCommand1);
        var translateVoiceCommand = ParsedVoiceCommand.builder()
                .id(voiceCommand.getId())
                .text("call juan")
                .language("en-US")
                .build();
        given(translateService.translate(parseVoiceCommand1)).willReturn(translateVoiceCommand);
        // Actions(when)
        voiceCommandInputTopic.pipeInput(voiceCommand);

        // Verifications(then)
        var parseVoiceCommand = recognizedCommandsOutputTopic.readRecord().value();

        assertEquals(voiceCommand.getId(), parseVoiceCommand.getId());
        assertEquals("call juan", parseVoiceCommand.getText());

    }

//    @DisplayName("Given a non-English voice command, when processed correctly Then I receive a ParsedVoiceCommand in the recognized-commands topic.")
//    @Test
//    void testScenario3() {
//        // Pre-conditions (given)
//        byte[] randomBytes = new byte[20];
//        new Random().nextBytes(randomBytes);
//        var voiceCommand = VoiceCommand.builder()
//                .id(UUID.randomUUID().toString())
//                .audio(randomBytes)
//                .audioCode("FLAC")
//                .language("fr-FR")
//                .build();
//
//        var parseVoiceCommand1 = ParseVoiceCommand.builder()
//                .id(voiceCommand.getId())
//                .language("fr-FR")
//                .text("appeler Jean")
//                .probability(0.98)
//                .build();
//
//        given(speechToTextService.speechToText(voiceCommand)).willReturn(parseVoiceCommand1);
//        var translateVoiceCommand = ParseVoiceCommand.builder()
//                .id(voiceCommand.getId())
//                .text("call jean")
//                .language("en-US")
//                .build();
//        given(translateService.translate(parseVoiceCommand1)).willReturn(translateVoiceCommand);
//        // Actions(when)
//        voiceCommandInputTopic.pipeInput(voiceCommand);
//
//        // Verifications(then)
//        var parseVoiceCommand = recognizedCommandsOutputTopic.readRecord().value();
//
//        assertEquals(voiceCommand.getId(), parseVoiceCommand.getId());
//        assertEquals("call jean", parseVoiceCommand.getText());
//
//    }
//
//    @DisplayName("Given a non-English voice command, when processed correctly Then I receive a ParsedVoiceCommand in the recognized-commands topic.")
//    @Test
//    void testScenario4() {
//        // Pre-conditions (given)
//        byte[] randomBytes = new byte[20];
//        new Random().nextBytes(randomBytes);
//        var voiceCommand = VoiceCommand.builder()
//                .id(UUID.randomUUID().toString())
//                .audio(randomBytes)
//                .audioCode("FLAC")
//                .language("ru-RU")
//                .build();
//
//        var parseVoiceCommand1 = ParseVoiceCommand.builder()
//                .id(voiceCommand.getId())
//                .language("ru-RU")
//                .text("zvonite Juanu")
//                .probability(0.98)
//                .build();
//
//        given(speechToTextService.speechToText(voiceCommand)).willReturn(parseVoiceCommand1);
//        var translateVoiceCommand = ParseVoiceCommand.builder()
//                .id(voiceCommand.getId())
//                .text("call Juanu")
//                .language("en-US")
//                .build();
//        given(translateService.translate(parseVoiceCommand1)).willReturn(translateVoiceCommand);
//        // Actions(when)
//        voiceCommandInputTopic.pipeInput(voiceCommand);
//
//        // Verifications(then)
//        var parseVoiceCommand = recognizedCommandsOutputTopic.readRecord().value();
//
//        assertEquals(voiceCommand.getId(), parseVoiceCommand.getId());
//        assertEquals("call Juanu", parseVoiceCommand.getText());
//
//    }

    @Test
    @DisplayName("Given a non-recognizable voice command, when processed correctly Then I receive a ParsedVoiceCommand in the unrecognized-commands topic.")
    void testScenario5() {
        // Pre-conditions (given)
        byte[] randomBytes = new byte[20];
        new Random().nextBytes(randomBytes);
        var voiceCommand = VoiceCommand.builder()
                .id(UUID.randomUUID().toString())
                .audio(randomBytes)
                .audioCode("FLAC")
                .language("en-US")
                .build();

        var parseVoiceCommand1 = ParsedVoiceCommand.builder()
                .id(voiceCommand.getId())
                .language("en-US")
                .text("call john")
                .probability(0.75)
                .build();

        given(speechToTextService.speechToText(voiceCommand)).willReturn(parseVoiceCommand1);
        // Actions(when)
        voiceCommandInputTopic.pipeInput(voiceCommand);

        // Verifications(then)
        var parseVoiceCommand = unrecognizedCommandsOutputTopic.readRecord().value();

        assertEquals(voiceCommand.getId(), parseVoiceCommand.getId());
        assertTrue(recognizedCommandsOutputTopic.isEmpty());
        verify(translateService, never()).translate(any(ParsedVoiceCommand.class));
    }

    @Test
    @DisplayName("Given voice command that is too short(less than 10 bytes), when processed correctly Then I don't receive any command in any of the output topics.")
    void testScenario6() {
        // Pre-conditions (given)
        byte[] randomBytes = new byte[9];
        new Random().nextBytes(randomBytes);
        var voiceCommand = VoiceCommand.builder()
                .id(UUID.randomUUID().toString())
                .audio(randomBytes)
                .audioCode("FLAC")
                .language("en-US")
                .build();

        // Actions(when)
        voiceCommandInputTopic.pipeInput(voiceCommand);

        // Verifications(then)
        assertTrue(recognizedCommandsOutputTopic.isEmpty());
        assertTrue(unrecognizedCommandsOutputTopic.isEmpty());

        verify(speechToTextService, never()).speechToText(any(VoiceCommand.class));
        verify(translateService, never()).translate(any(ParsedVoiceCommand.class));

    }
}