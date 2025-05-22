package com.luoqing.ciyuanda;

import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.TimeUnit;

@SpringBootTest
public class RxjavaTest {

    @SneakyThrows
    @Test
    public void test() {
        //创建数据流
         Flowable<Long> flowable = Flowable.interval(1, TimeUnit.SECONDS)
                .map(i -> i + 1)
                .subscribeOn(Schedulers.io());

         //创建订阅者
         flowable
                 .observeOn(Schedulers.io())
                 .doOnNext(i -> System.out.println(i.toString()))
                 .subscribe();

         //线程休眠
        Thread.sleep(10000L);

    }
}
