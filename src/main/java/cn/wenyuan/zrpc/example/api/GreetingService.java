package cn.wenyuan.zrpc.example.api;

import cn.wenyuan.zrpc.example.dto.User;

public interface GreetingService {

    String greet(User user);

    int sum(int left, int right);
}
