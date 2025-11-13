package cn.wenyuan.zrpc.demo.api;

import cn.wenyuan.zrpc.demo.dto.User;

public interface GreetingService {

    String greet(User user);

    int sum(int left, int right);
}
