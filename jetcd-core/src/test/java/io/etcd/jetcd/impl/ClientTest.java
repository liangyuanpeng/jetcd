package io.etcd.jetcd.impl;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.Lease;
import io.etcd.jetcd.Watch;
import io.etcd.jetcd.lease.LeaseGrantResponse;
import io.etcd.jetcd.lease.LeaseKeepAliveResponse;
import io.etcd.jetcd.options.WatchOption;
import io.etcd.jetcd.watch.WatchResponse;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ClientTest {

    @Test
    public void testLease() throws InterruptedException {
         Client client = Client.builder().endpoints("http://127.0.0.1:2379").build();
        Lease leaseClient = client.getLeaseClient();
        CompletableFuture<LeaseGrantResponse> grant = leaseClient.grant(5);
        long leaseID = grant.join().getID();
        System.out.println(grant.join().getID());
        TimeUnit.SECONDS.sleep(6);
        CompletableFuture<LeaseKeepAliveResponse> leaseKeepAliveResponseCompletableFuture = leaseClient.keepAliveOnce(leaseID);
        System.out.println(leaseKeepAliveResponseCompletableFuture.join().getTTL());
    }
    
    @Test
    public void testWatcherAndGet() throws Exception {

        final Client client = Client.builder().endpoints("http://127.0.0.1:2379").build();
        final ByteSequence keyToGet = ByteSequence.from("/keytoGet", StandardCharsets.UTF_8);
        final ByteSequence keyToWatch = ByteSequence.from("/keytoWatch", StandardCharsets.UTF_8);
        final ByteSequence keyValueToWatch = ByteSequence.from("keyValuetoWatch", StandardCharsets.UTF_8);

        CountDownLatch countDownLatch = new CountDownLatch(1);
        client.getWatchClient().watch(keyToWatch, WatchOption.DEFAULT, new Watch.Listener() {

            @Override
            public void onNext(WatchResponse watchResponse) {
                try {
                    client.getKVClient().get(keyToGet).get(); // stuck there with 0.7.3 but ok with 0.5.7
                    countDownLatch.countDown();
                } catch (Exception e) {
//                   e.printStackTrace();
                }
            }

            @Override
            public void onError(Throwable throwable) { }
            @Override
            public void onCompleted() { }
        });

        client.getKVClient().put(keyToWatch, keyValueToWatch).get();
        countDownLatch.await();
    }
}
