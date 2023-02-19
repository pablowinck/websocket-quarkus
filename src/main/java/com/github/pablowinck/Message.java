package com.github.pablowinck;

import com.google.gson.Gson;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Message {
    private String username;
    private String message;

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
