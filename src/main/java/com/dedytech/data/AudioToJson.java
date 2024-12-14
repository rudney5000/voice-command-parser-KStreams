package com.dedytech.data;

import com.dedytech.model.VoiceCommand;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class AudioToJson {

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @SneakyThrows
    public static void main(String[] args) {

        byte[] johnBytes = getFileAsBytes("audio/callJohn.flac");
        byte[] juanBytes = getFileAsBytes("audio/llamarJuan.flac");
        byte[] gibberishBytes = getFileAsBytes("audio/gibberish.flac");

        var johnData = VoiceCommand.builder()
                .id(UUID.randomUUID().toString())
                .audio(johnBytes)
                .language("en-US")
                .audioCode("FLAC")
                .build();
        var juanData = VoiceCommand.builder()
                .id(UUID.randomUUID().toString())
                .audio(juanBytes)
                .language("es-AR")
                .audioCode("FLAC")
                .build();
        var gibberishData = VoiceCommand.builder()
                .id(UUID.randomUUID().toString())
                .audio(gibberishBytes)
                .language("en-US")
                .audioCode("FLAC")
                .build();
        OBJECT_MAPPER.writeValue(Paths.get("/Users/physm/IdeaProjects/voice-command-parser/src/main/resources/data/test.json").toFile(), List.of(johnData, juanData, gibberishData));


    }

    private static byte[] getFileAsBytes(String fileName) throws IOException {
        return Objects.requireNonNull(AudioToJson.class.getClassLoader().getResourceAsStream(fileName)).readAllBytes();
    }
}
