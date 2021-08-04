package net.openhft.chronicle.threads;

import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TimeoutPauserTest {

    @Test
    public void pause() {
        TimeoutPauser tp = new TimeoutPauser(100);
        for (int i = 0; i < 10; i++) {
            long start = System.currentTimeMillis();
            while (true) {
                try {
                    tp.pause(100, TimeUnit.MILLISECONDS);
                    if (System.currentTimeMillis() - start > 110)
                        fail();
                } catch (TimeoutException e) {
                    assertEquals(105, System.currentTimeMillis() - start, 5);
                    tp.reset();
                    break;
                }
            }
        }
    }
}