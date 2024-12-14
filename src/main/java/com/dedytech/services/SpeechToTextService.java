package com.dedytech.services;

import com.dedytech.model.ParsedVoiceCommand;
import com.dedytech.model.VoiceCommand;

public interface SpeechToTextService {

    ParsedVoiceCommand speechToText(VoiceCommand voiceCommand);
}
