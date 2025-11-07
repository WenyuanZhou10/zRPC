package cn.wenyuan.zrpc.example.api;

import cn.wenyuan.zrpc.example.dto.User;

import java.util.concurrent.CompletableFuture;

public interface GreetingServiceAsync {

    CompletableFuture<String> greet(User user);

    CompletableFuture<Integer> sum(int left, int right);
}
