package cn.wenyuan.zrpc.example.api.impl;

import cn.wenyuan.zrpc.example.api.GreetingService;
import cn.wenyuan.zrpc.example.dto.User;

public class GreetingServiceImpl implements GreetingService {

    @Override
    public String greet(User user) {
        if (user == null) {
            return "Hello, anonymous!";
        }
        return "Hello, " + user.getName() + " (id=" + user.getId() + ")";
    }

    @Override
    public int sum(int left, int right) {
        return left + right;
    }
}
