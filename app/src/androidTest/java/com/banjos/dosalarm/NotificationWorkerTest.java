package com.banjos.dosalarm;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.work.testing.TestWorkerBuilder;
import androidx.work.testing.WorkManagerTestInitHelper;

import com.banjos.dosalarm.tools.PreferencesService;
import com.banjos.dosalarm.worker.NotificationWorker;

import org.junit.Before;
import org.junit.Test;
public class NotificationWorkerTest {

    private Context context;
    private PreferencesService preferencesService;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        WorkManagerTestInitHelper.initializeTestWorkManager(context);
        preferencesService = new PreferencesService(context);
    }

    @Test
    public void testMyWorkerAll() {
        NotificationWorker worker = TestWorkerBuilder.from(context, NotificationWorker.class).build();
        preferencesService.shacharitReminderSwitched(true);
        preferencesService.minchaReminderSwitched(true);
        preferencesService.maarivReminderSwitched(true);
        preferencesService.candleLightReminderSwitched(true);
        androidx.work.ListenableWorker.Result result = worker.doWork();

        // Assert the result
        assertEquals(androidx.work.ListenableWorker.Result.success(), result);
    }

    @Test
    public void testMyWorkerAllWithTimeChange() {
        NotificationWorker worker = TestWorkerBuilder.from(context, NotificationWorker.class).build();

        preferencesService.setShacharitMinutesBeforeSunriseForReminder(60);
        preferencesService.shacharitReminderSwitched(true);

        preferencesService.setMinchaMinutesBeforeSunsetForReminder(60);
        preferencesService.minchaReminderSwitched(true);

        preferencesService.setMaarivMinutesAfterSunsetForReminder(60);
        preferencesService.maarivReminderSwitched(true);

        preferencesService.setCandleLightingMinutesBeforeShabbatForReminder(60);
        preferencesService.candleLightReminderSwitched(true);
        androidx.work.ListenableWorker.Result result = worker.doWork();

        // Assert the result
        assertEquals(androidx.work.ListenableWorker.Result.success(), result);
    }

    @Test
    public void testMyWorkerShacharit() {
        NotificationWorker worker = TestWorkerBuilder.from(context, NotificationWorker.class).build();

        preferencesService.shacharitReminderSwitched(true);
        preferencesService.minchaReminderSwitched(false);
        preferencesService.maarivReminderSwitched(false);
        preferencesService.candleLightReminderSwitched(false);
        androidx.work.ListenableWorker.Result result = worker.doWork();

        // Assert the result
        assertEquals(androidx.work.ListenableWorker.Result.success(), result);
    }
}
