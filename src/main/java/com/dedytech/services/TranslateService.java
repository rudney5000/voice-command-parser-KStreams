package com.dedytech.services;

import com.dedytech.model.ParsedVoiceCommand;

public interface TranslateService {

    ParsedVoiceCommand translate(ParsedVoiceCommand original);
}
