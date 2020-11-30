package com.tangoristo.server.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RubyHint {
    private String ruby;
    private String rt;
    private String leftContext;
    private String rightContext;

    @Override
    public String toString() {
        return String.format("%s\t%s\t%s\t%s", ruby, rt, leftContext, rightContext);
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RubyHint) {
            return this.toString().equals(obj.toString());
        } else {
            return obj == this;
        }
    }
}
