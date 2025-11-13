package cn.wenyuan.zrpc.demo.api;

import cn.wenyuan.zrpc.demo.dto.User;

import java.util.concurrent.CompletableFuture;

public interface GreetingServiceAsync {

    CompletableFuture<String> greet(User user);

    CompletableFuture<Integer> sum(int left, int right);
}
