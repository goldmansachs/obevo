package com.gs.obevo.db.apps.reveng;

import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Maps;

public class LineParseOutput {
    private String lineOutput;
    private MutableMap<String, String> tokens = Maps.mutable.empty();

    public LineParseOutput() {
    }

    public LineParseOutput(String lineOutput) {
        this.lineOutput = lineOutput;
    }

    public String getLineOutput() {
        return lineOutput;
    }

    public void setLineOutput(String lineOutput) {
        this.lineOutput = lineOutput;
    }

    public MutableMap<String, String> getTokens() {
        return tokens;
    }

    public void addToken(String key, String value) {
        tokens.put(key, value);
    }

    LineParseOutput withToken(String key, String value) {
        tokens.put(key, value);
        return this;
    }
}
