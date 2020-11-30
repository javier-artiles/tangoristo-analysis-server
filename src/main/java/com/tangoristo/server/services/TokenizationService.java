package com.tangoristo.server.services;

import java.util.List;
import com.tangoristo.server.model.Token;
import org.springframework.stereotype.Service;

public interface TokenizationService {
    List<Token> tokenize(String text);
}
