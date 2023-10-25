package com.banjos.dosalarm;

import static org.junit.Assert.assertEquals;

import com.banjos.dosalarm.receiver.NotificationReceiver;

import org.junit.Test;

public class NotificationReceiverTest {

    @Test
    public void addition_isCorrect() {
        NotificationReceiver notificationReceiver = new NotificationReceiver();
      //  notificationReceiver.onReceive();
        assertEquals(4, 2 + 2);
    }
}
