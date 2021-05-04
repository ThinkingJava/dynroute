package com.dynroute.controller;

import cn.hutool.json.JSONObject;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/server")
@RestController
public class ServerController {

    @RequestMapping(value = "/test")
    public String test(@RequestParam String key){
        JSONObject jsonObject = new JSONObject();
        jsonObject.set("key",key);
        return jsonObject.toString();
    }

}
